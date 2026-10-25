package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ProvideInfoRequest(
        @NotBlank(message = "Response text cannot be blank")
        String responseText) {}

