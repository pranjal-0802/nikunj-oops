package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record FlagWrongDomainRequest(
        @NotBlank(message = "Reason for wrong domain flag must be provided")
        String reason) {}

