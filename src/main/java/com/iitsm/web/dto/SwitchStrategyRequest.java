package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to switch the active assignment strategy.
 */
public record SwitchStrategyRequest(
        @NotBlank(message = "Strategy name must not be blank")
        String strategyName
) {}

