package com.apisentinel.payload.response;

import java.time.LocalDateTime;

public class IncidentResponse {
    private Long id;
    private String statusChange;
    private Integer statusCode;
    private LocalDateTime timestamp;
    private String reason;

    public IncidentResponse(Long id, String statusChange, Integer statusCode, LocalDateTime timestamp, String reason) {
        this.id = id;
        this.statusChange = statusChange;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getStatusChange() { return statusChange; }
    public Integer getStatusCode() { return statusCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReason() { return reason; }
}
