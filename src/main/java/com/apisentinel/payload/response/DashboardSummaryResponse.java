package com.apisentinel.payload.response;

public class DashboardSummaryResponse {
    private long totalApis;
    private long upApis;
    private long downApis;
    private long incidentsToday;
    private double avgResponseTime;

    public DashboardSummaryResponse(long totalApis, long upApis, long downApis, long incidentsToday, double avgResponseTime) {
        this.totalApis = totalApis;
        this.upApis = upApis;
        this.downApis = downApis;
        this.incidentsToday = incidentsToday;
        this.avgResponseTime = avgResponseTime;
    }

    public long getTotalApis() { return totalApis; }
    public void setTotalApis(long totalApis) { this.totalApis = totalApis; }

    public long getUpApis() { return upApis; }
    public void setUpApis(long upApis) { this.upApis = upApis; }

    public long getDownApis() { return downApis; }
    public void setDownApis(long downApis) { this.downApis = downApis; }

    public long getIncidentsToday() { return incidentsToday; }
    public void setIncidentsToday(long incidentsToday) { this.incidentsToday = incidentsToday; }

    public double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
}
