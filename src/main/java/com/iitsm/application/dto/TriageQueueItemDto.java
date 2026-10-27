package com.iitsm.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Unified DTO for the Administrator triage queue containing both
 * low-confidence classified incidents and wrong-domain reassignment requests.
 */
public record TriageQueueItemDto(
        String triageType, // "LOW_CONFIDENCE" or "WRONG_DOMAIN"
        String incidentId,
        String title,
        String description,
        String category,
        String serviceId,
        String serviceName,
        Double confidence,
        List<CategorySuggestionDto> suggestions,
        String requestingTechnicianId,
        String reason,
        Instant createdAt
) {
    public record CategorySuggestionDto(String category, double confidence) {}
}
