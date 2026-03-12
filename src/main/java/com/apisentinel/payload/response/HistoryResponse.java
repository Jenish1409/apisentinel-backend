package com.apisentinel.payload.response;

import java.time.LocalDateTime;

public class HistoryResponse {
    private Long id;
    private String status;
    private Integer responseTimeMs;
    private Integer statusCode;
    private LocalDateTime checkedAt;
    private String errorReason;

    public HistoryResponse(Long id, String status, Integer responseTimeMs, Integer statusCode, LocalDateTime checkedAt, String errorReason) {
        this.id = id;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.statusCode = statusCode;
        this.checkedAt = checkedAt;
        this.errorReason = errorReason;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public Integer getStatusCode() { return statusCode; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public String getErrorReason() { return errorReason; }
}
