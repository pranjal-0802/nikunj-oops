package com.iitsm.web.dto;

import java.util.List;

public record WorkloadReportDto(
        List<TechnicianLoadItem> technicianLoads,
        List<TeamLoadItem> teamLoads,
        double averageLoadRatio) {

    public record TechnicianLoadItem(
            String technicianId,
            String name,
            String teamId,
            int activeIncidents,
            int maxCapacity,
            double loadRatio,
            int resolvedIncidents) {}

    public record TeamLoadItem(
            String teamId,
            String teamName,
            int memberCount,
            int totalActiveIncidents,
            int totalCapacity,
            double teamLoadRatio) {}
}

