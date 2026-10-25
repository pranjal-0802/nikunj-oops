package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 120, message = "Title must be between 5 and 120 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 4000, message = "Description must be between 20 and 4000 characters")
        String description) {}

