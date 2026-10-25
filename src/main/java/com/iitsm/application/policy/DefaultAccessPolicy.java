package com.iitsm.application.policy;

import com.iitsm.domain.policy.AccessPolicy;
import org.springframework.stereotype.Component;

/**
 * Pattern: Policy.
 * Default application-layer implementation of AccessPolicy.
 * Enforces ownership, role authorization, and action guardrails across all application services.
 */
@Component
public class DefaultAccessPolicy implements AccessPolicy {
    // Inherits default behavior defined in AccessPolicy interface
}

