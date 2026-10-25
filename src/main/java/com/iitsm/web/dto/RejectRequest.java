package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank(message = "Rejection reason cannot be blank")
        String reason) {}

