package com.iitsm.web.dto;

import com.iitsm.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OverridePriorityRequest(
        @NotNull(message = "New priority is required")
        Priority priority,

        @NotBlank(message = "Mandatory override reason is required")
        String reason) {}

