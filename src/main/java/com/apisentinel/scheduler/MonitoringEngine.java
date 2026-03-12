package com.apisentinel.scheduler;

import com.apisentinel.entity.ApiCheckHistory;
import com.apisentinel.entity.ApiIncident;
import com.apisentinel.entity.CheckStatus;
import com.apisentinel.entity.IncidentStatus;
import com.apisentinel.entity.MonitoredApi;
import com.apisentinel.repository.ApiCheckHistoryRepository;
import com.apisentinel.repository.ApiIncidentRepository;
import com.apisentinel.repository.MonitoredApiRepository;
import com.apisentinel.service.EmailService;
import com.apisentinel.security.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringEngine {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringEngine.class);
    private final Map<Long, Long> lastCheckTimes = new ConcurrentHashMap<>();

    @Autowired private MonitoredApiRepository apiRepository;
    @Autowired private ApiCheckHistoryRepository historyRepository;
    @Autowired private ApiIncidentRepository incidentRepository;
    @Autowired private EmailService emailService;
    @Autowired private EncryptionUtils encryptionUtils;

    private final WebClient webClient = WebClient.builder().build();

    @Scheduled(fixedRate = 10000)
    public void runChecks() {
        // Use findAllWithUser() to JOIN FETCH the User in one query so it's fully
        // initialized before the reactive Flux pipeline runs on Netty IO threads.
        List<MonitoredApi> apis = apiRepository.findAllWithUser();

        long now = System.currentTimeMillis();

        Flux.fromIterable(apis)
            .filter(api -> {
                if (!api.getEnabled()) return false;
                
                // Skip if we are currently serving a rate-limit cooldown
                if (api.getRateLimitUntil() != null && LocalDateTime.now().isBefore(api.getRateLimitUntil())) {
                    return false;
                }
                
                long lastCheck = lastCheckTimes.getOrDefault(api.getId(), 0L);
                long intervalMs = api.getIntervalSeconds() * 1000L;
                return (now - lastCheck) >= intervalMs;
            })
            .flatMap(api -> {
                lastCheckTimes.put(api.getId(), now);
                long startTime = System.currentTimeMillis();
                
                final String userEmail = api.getUser().getEmail(); // safe: User loaded by JOIN FETCH

                WebClient.RequestHeadersSpec<?> request = webClient.method(HttpMethod.valueOf(api.getMethod()))
                    .uri(api.getUrl());
                
                if (api.getApiKey() != null && !api.getApiKey().isEmpty()) {
                    try {
                        String decryptedKey = encryptionUtils.decrypt(api.getApiKey());
                        request.header("Authorization", "Bearer " + decryptedKey);
                    } catch (Exception e) {
                        logger.error("Failed to decrypt API key for {}", api.getName(), e);
                    }
                }

                return request.exchangeToMono(response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        HttpStatusCode statusCode = response.statusCode();
                        boolean isUp = !statusCode.is4xxClientError() && !statusCode.is5xxServerError();
                        
                        recordResult(api, userEmail, isUp ? CheckStatus.UP : CheckStatus.DOWN, (int) responseTime, statusCode.value());
                        return Mono.empty();
                    })
                    .onErrorResume(e -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        recordResult(api, userEmail, CheckStatus.DOWN, (int) responseTime, 0);
                        return Mono.empty();
                    });
            })
            .subscribe();
    }

    private void recordResult(MonitoredApi api, String userEmail, CheckStatus status, int responseTimeMs, int statusCode) {
        Optional<ApiCheckHistory> lastCheckOpt = historyRepository.findTopByApiIdOrderByCheckedAtDesc(api.getId());
        CheckStatus lastStatus = lastCheckOpt.map(ApiCheckHistory::getStatus).orElse(CheckStatus.UP);

        ApiCheckHistory history = new ApiCheckHistory();
        history.setApi(api);
        history.setStatus(status);
        history.setResponseTimeMs(responseTimeMs);
        history.setStatusCode(statusCode);
        historyRepository.save(history);

        boolean apiUpdated = false;

        if (status == CheckStatus.DOWN) {
            int failures = api.getConsecutiveFailures() == null ? 0 : api.getConsecutiveFailures();
            api.setConsecutiveFailures(failures + 1);
            apiUpdated = true;

            if (api.getConsecutiveFailures() >= 3 && (api.getAlertSent() == null || !api.getAlertSent())) {
                String reason = getReasonForStatusCode(statusCode);
                emailService.sendApiDownAlert(userEmail, api.getName(), statusCode, reason);
                api.setAlertSent(true);
            }

            if (statusCode == 429) {
                // Rate limit detected, pause checking for 5 minutes
                api.setRateLimitUntil(LocalDateTime.now().plusMinutes(5));
            }
        } else if (status == CheckStatus.UP) {
            if (api.getConsecutiveFailures() != null && api.getConsecutiveFailures() > 0) {
                api.setConsecutiveFailures(0);
                apiUpdated = true;
            }
            if (api.getAlertSent() != null && api.getAlertSent()) {
                emailService.sendApiRecoveryAlert(userEmail, api.getName());
                api.setAlertSent(false);
                apiUpdated = true;
            }
            if (api.getRateLimitUntil() != null) {
                api.setRateLimitUntil(null);
                apiUpdated = true;
            }
        }

        if (apiUpdated) {
            apiRepository.save(api);
        }

        if (lastStatus == CheckStatus.UP && status == CheckStatus.DOWN) {
            ApiIncident incident = new ApiIncident();
            incident.setApi(api);
            incident.setStatusChange(IncidentStatus.DOWN);
            incident.setStatusCode(statusCode);
            incidentRepository.save(incident);
        } else if (lastStatus == CheckStatus.DOWN && status == CheckStatus.UP) {
            ApiIncident incident = new ApiIncident();
            incident.setApi(api);
            incident.setStatusChange(IncidentStatus.RECOVERED);
            incident.setStatusCode(statusCode);
            incidentRepository.save(incident);
        }
    }

    private String getReasonForStatusCode(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 403 -> "Forbidden access";
            case 404 -> "Endpoint not found";
            case 429 -> "API rate limit exceeded";
            case 500 -> "Server error";
            case 0 -> "API not responding / Timeout";
            default -> "HTTP " + statusCode;
        };
    }
}
