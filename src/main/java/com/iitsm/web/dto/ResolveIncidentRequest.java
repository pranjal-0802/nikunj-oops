package com.iitsm.web.dto;

import com.iitsm.domain.model.IncidentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveIncidentRequest(
        @NotBlank(message = "Root cause cannot be blank")
        String rootCause,

        @NotBlank(message = "Steps taken cannot be blank")
        String stepsTaken,

        String preventiveAction,

        @NotNull(message = "Confirmed category cannot be null")
        IncidentCategory confirmedCategory) {}

