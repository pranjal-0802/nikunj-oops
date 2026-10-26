package com.iitsm.web.dto;

import java.util.List;
import java.util.Map;

public record ServiceAnalysisReportDto(
        List<ServiceMetricItem> serviceMetrics,
        Map<String, Long> categoryDistribution) {

    public record ServiceMetricItem(
            String serviceId,
            String serviceName,
            String owningTeamId,
            String businessCriticality,
            long totalIncidents,
            long activeIncidents,
            long p1Incidents) {}
}

