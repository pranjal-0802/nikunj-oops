package com.iitsm.web.dto;

import com.iitsm.domain.model.IncidentCategory;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for administrative incident reclassification.
 */
public record ReclassifyRequest(
        @NotNull(message = "Category cannot be null")
        IncidentCategory category,
        String serviceId,
        String reason
) {}

