package com.iitsm.web.dto;

import java.util.Map;

public record SlaPerformanceReportDto(
        long totalEvaluated,
        long metCount,
        long breachedCount,
        long warningCount,
        long atRiskCount,
        double overallCompliancePercentage,
        Map<String, PrioritySlaMetric> priorityCompliance) {

    public record PrioritySlaMetric(
            long metCount,
            long breachedCount,
            double compliancePercentage) {}
}

