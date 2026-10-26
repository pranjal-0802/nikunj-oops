package com.iitsm.web.dto;

import com.iitsm.domain.model.EscalationRecord;
import java.util.List;
import java.util.Map;

public record EscalationReportDto(
        long totalEscalations,
        Map<String, Long> escalationsByTier,
        List<EscalationRecord> recentEscalations) {}

