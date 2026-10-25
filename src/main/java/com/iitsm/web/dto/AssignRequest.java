package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRequest(
        @NotBlank(message = "Technician ID is required")
        String technicianId) {}

