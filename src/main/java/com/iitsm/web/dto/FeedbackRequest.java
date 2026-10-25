package com.iitsm.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FeedbackRequest(
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        int rating,

        String comment) {}

