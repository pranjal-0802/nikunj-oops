package com.iitsm.web.dto;

import java.util.Map;

public record MttrReportDto(
        double overallMttrMinutes,
        double overallMttrespMinutes,
        Map<String, Double> mttrByCategoryMinutes,
        Map<String, Double> mttrByPriorityMinutes) {}

