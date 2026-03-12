package com.apisentinel.payload.response;

import java.time.LocalDateTime;
import java.util.List;

public class AdminUserResponse {
    private Long id;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private long apiCount;
    private long incidentCount;
    private LocalDateTime lastActivity;
    private List<ApiSummary> apis;

    public record ApiSummary(Long id, String name, String method, String url, String status, double uptimePercent) {}

    public AdminUserResponse(Long id, String email, String role, LocalDateTime createdAt,
                              long apiCount, long incidentCount,
                              LocalDateTime lastActivity, List<ApiSummary> apis) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.apiCount = apiCount;
        this.incidentCount = incidentCount;
        this.lastActivity = lastActivity;
        this.apis = apis;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getApiCount() { return apiCount; }
    public long getIncidentCount() { return incidentCount; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public List<ApiSummary> getApis() { return apis; }
}
