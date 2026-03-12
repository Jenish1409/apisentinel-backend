package com.apisentinel.payload.response;

import java.time.LocalDateTime;
import java.util.List;

public class ApiResponse {
    private Long id;
    private String name;
    private String url;
    private String method;
    private Integer intervalSeconds;
    private String currentStatus;
    private Double averageResponseTime;
    private Double uptimePercentage;
    private Boolean enabled;
    private Long totalChecks;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime rateLimitUntil;
    private Long incidentCount;
    private List<String> recentStatuses;

    public ApiResponse(Long id, String name, String url, String method, Integer intervalSeconds,
                       String currentStatus, Double averageResponseTime, Double uptimePercentage, Boolean enabled,
                       Long totalChecks, LocalDateTime lastCheckedAt, LocalDateTime rateLimitUntil,
                       Long incidentCount, List<String> recentStatuses) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.method = method;
        this.intervalSeconds = intervalSeconds;
        this.currentStatus = currentStatus;
        this.averageResponseTime = averageResponseTime;
        this.uptimePercentage = uptimePercentage;
        this.enabled = enabled;
        this.totalChecks = totalChecks;
        this.lastCheckedAt = lastCheckedAt;
        this.rateLimitUntil = rateLimitUntil;
        this.incidentCount = incidentCount;
        this.recentStatuses = recentStatuses;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public Double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }

    public Double getUptimePercentage() { return uptimePercentage; }
    public void setUptimePercentage(Double uptimePercentage) { this.uptimePercentage = uptimePercentage; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(Long totalChecks) { this.totalChecks = totalChecks; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public LocalDateTime getRateLimitUntil() { return rateLimitUntil; }
    public void setRateLimitUntil(LocalDateTime rateLimitUntil) { this.rateLimitUntil = rateLimitUntil; }

    public Long getIncidentCount() { return incidentCount; }
    public void setIncidentCount(Long incidentCount) { this.incidentCount = incidentCount; }

    public List<String> getRecentStatuses() { return recentStatuses; }
    public void setRecentStatuses(List<String> recentStatuses) { this.recentStatuses = recentStatuses; }
}
