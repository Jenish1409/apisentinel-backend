package com.apisentinel.service;

import com.apisentinel.entity.*;
import com.apisentinel.payload.request.ApiRequest;
import com.apisentinel.payload.response.ApiResponse;
import com.apisentinel.payload.response.HistoryResponse;
import com.apisentinel.payload.response.IncidentResponse;
import com.apisentinel.repository.*;
import com.apisentinel.security.EncryptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApiService {
    @Autowired MonitoredApiRepository apiRepository;
    @Autowired ApiCheckHistoryRepository historyRepository;
    @Autowired ApiIncidentRepository incidentRepository;
    @Autowired UserRepository userRepository;
    @Autowired EncryptionUtils encryptionUtils;

    public MonitoredApi createApi(ApiRequest request, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        MonitoredApi api = new MonitoredApi();
        api.setName(request.getName());
        api.setUrl(request.getUrl());
        api.setMethod(request.getMethod());
        api.setIntervalSeconds(request.getIntervalSeconds());
        
        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            api.setApiKey(encryptionUtils.encrypt(request.getApiKey().trim()));
        }

        api.setEnabled(true);
        api.setLastCheckedAt(java.time.LocalDateTime.now().minusDays(1)); // Make it trigger immediately
        api.setUser(user);
        return apiRepository.save(api);
    }

    public List<ApiResponse> getUserApis(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<MonitoredApi> apis = apiRepository.findByUserId(user.getId());
        
        List<ApiResponse> responses = new ArrayList<>();
        for (MonitoredApi api : apis) {
            String status = "UNKNOWN";
            Optional<ApiCheckHistory> latestCheck = historyRepository.findTopByApiIdOrderByCheckedAtDesc(api.getId());
            if (latestCheck.isPresent()) {
                status = latestCheck.get().getStatus().name();
            }

            long totalChecks = historyRepository.countByApiId(api.getId());
            long successChecks = historyRepository.countByApiIdAndStatus(api.getId(), CheckStatus.UP);
            double uptime = 100.0;
            if (totalChecks > 0) {
                uptime = (double) successChecks / totalChecks * 100;
            }

            Double avgResponse = historyRepository.getAverageResponseTime(api.getId());
            if (avgResponse == null) avgResponse = 0.0;

            long incidentCount = incidentRepository.countByApiId(api.getId());
            List<String> recentStatuses = historyRepository.findTop10ByApiIdOrderByCheckedAtDesc(api.getId())
                    .stream().map(h -> h.getStatus().name()).collect(Collectors.toList());

            responses.add(new ApiResponse(api.getId(), api.getName(), api.getUrl(), api.getMethod(), 
                    api.getIntervalSeconds(), status, avgResponse, uptime, api.getEnabled(),
                    totalChecks, api.getLastCheckedAt(), api.getRateLimitUntil(), incidentCount, recentStatuses));
        }
        return responses;
    }

    public com.apisentinel.payload.response.DashboardSummaryResponse getDashboardSummary(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<MonitoredApi> apis = apiRepository.findByUserId(user.getId());
        
        long totalApis = apis.size();
        long upApis = 0;
        long downApis = 0;
        double totalAvgResponse = 0;
        int apisWithResponseData = 0;
        
        for (MonitoredApi api : apis) {
            Optional<ApiCheckHistory> latestCheck = historyRepository.findTopByApiIdOrderByCheckedAtDesc(api.getId());
            if (latestCheck.isPresent()) {
                if (latestCheck.get().getStatus() == CheckStatus.UP) {
                    upApis++;
                } else {
                    downApis++;
                }
            }
            
            Double avgResponse = historyRepository.getAverageResponseTime(api.getId());
            if (avgResponse != null && avgResponse > 0) {
                totalAvgResponse += avgResponse;
                apisWithResponseData++;
            }
        }
        
        double overallAvgResponse = apisWithResponseData > 0 ? totalAvgResponse / apisWithResponseData : 0.0;
        
        // Count incidents today for user's APIs
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().toLocalDate().atStartOfDay();
        long incidentsToday = 0;
        for (MonitoredApi api : apis) {
            long apiIncidentsToday = incidentRepository.findByApiIdOrderByTimestampDesc(api.getId()).stream()
                .filter(i -> i.getTimestamp().isAfter(startOfDay))
                .count();
            incidentsToday += apiIncidentsToday;
        }

        return new com.apisentinel.payload.response.DashboardSummaryResponse(totalApis, upApis, downApis, incidentsToday, overallAvgResponse);
    }

    public ApiResponse getApiDetails(Long apiId, String email) {
        validateOwner(apiId, email);
        MonitoredApi api = apiRepository.findById(apiId).get();
        
        String status = "UNKNOWN";
        Optional<ApiCheckHistory> latestCheck = historyRepository.findTopByApiIdOrderByCheckedAtDesc(api.getId());
        if (latestCheck.isPresent()) {
            status = latestCheck.get().getStatus().name();
        }

        long totalChecks = historyRepository.countByApiId(api.getId());
        long successChecks = historyRepository.countByApiIdAndStatus(api.getId(), CheckStatus.UP);
        double uptime = 100.0;
        if (totalChecks > 0) {
            uptime = (double) successChecks / totalChecks * 100;
        }

        Double avgResponse = historyRepository.getAverageResponseTime(api.getId());
        if (avgResponse == null) avgResponse = 0.0;

        long incidentCount = incidentRepository.countByApiId(api.getId());
        List<String> recentStatuses = historyRepository.findTop10ByApiIdOrderByCheckedAtDesc(api.getId())
                .stream().map(h -> h.getStatus().name()).collect(Collectors.toList());

        return new ApiResponse(api.getId(), api.getName(), api.getUrl(), api.getMethod(), 
                api.getIntervalSeconds(), status, avgResponse, uptime, api.getEnabled(),
                totalChecks, api.getLastCheckedAt(), api.getRateLimitUntil(), incidentCount, recentStatuses);
    }

    public List<HistoryResponse> getApiHistory(Long apiId, String email) {
        validateOwner(apiId, email);
        return historyRepository.findByApiIdOrderByCheckedAtDesc(apiId).stream()
                .map(history -> new HistoryResponse(
                        history.getId(),
                        history.getStatus().name(),
                        history.getResponseTimeMs(),
                        history.getStatusCode(),
                        history.getCheckedAt(),
                        getReasonForStatusCode(history.getStatusCode())
                ))
                .collect(Collectors.toList());
    }

    public List<IncidentResponse> getApiIncidents(Long apiId, String email) {
        validateOwner(apiId, email);
        return incidentRepository.findByApiIdOrderByTimestampDesc(apiId).stream()
                .map(incident -> new IncidentResponse(
                        incident.getId(),
                        incident.getStatusChange().name(),
                        incident.getStatusCode(),
                        incident.getTimestamp(),
                        getReasonForStatusCode(incident.getStatusCode() != null ? incident.getStatusCode() : 0)
                ))
                .collect(Collectors.toList());
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

    private void validateOwner(Long apiId, String email) {
        MonitoredApi api = apiRepository.findById(apiId).orElseThrow(() -> new RuntimeException("API not found"));
        if (!api.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
    }

    public void updateApi(Long apiId, ApiRequest request, String email) {
        validateOwner(apiId, email);
        MonitoredApi api = apiRepository.findById(apiId).get();
        api.setName(request.getName());
        api.setUrl(request.getUrl());
        api.setMethod(request.getMethod());
        api.setIntervalSeconds(request.getIntervalSeconds());
        
        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            api.setApiKey(encryptionUtils.encrypt(request.getApiKey().trim()));
        } else if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
            // Optional: clear API key if empty string sent
            api.setApiKey(null);
        }

        apiRepository.save(api);
    }

    public void deleteApi(Long apiId, String email) {
        validateOwner(apiId, email);
        // Clean up history and incidents first
        List<ApiCheckHistory> history = historyRepository.findByApiIdOrderByCheckedAtDesc(apiId);
        historyRepository.deleteAll(history);
        List<ApiIncident> incidents = incidentRepository.findByApiIdOrderByTimestampDesc(apiId);
        incidentRepository.deleteAll(incidents);
        
        apiRepository.deleteById(apiId);
    }

    public void toggleApi(Long apiId, String email) {
        validateOwner(apiId, email);
        MonitoredApi api = apiRepository.findById(apiId).get();
        api.setEnabled(!api.getEnabled());
        apiRepository.save(api);
    }
}
