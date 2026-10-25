package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Duration;
import java.util.Objects;

/**
 * OOP: Entity encapsulating Service Level Agreement contractual targets.
 * Supports global priority-based policies and specific service overrides.
 */
public class SlaPolicy {

    private final String id;
    private final Priority priority;
    private final String serviceIdOverride; // null for default priority-wide policy
    private final Duration responseTarget;
    private final Duration resolutionTarget;
    private final double warningThresholdRatio; // e.g., 0.75 for 75%

    public SlaPolicy(
            String id,
            Priority priority,
            String serviceIdOverride,
            Duration responseTarget,
            Duration resolutionTarget,
            double warningThresholdRatio) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "SLA Policy ID cannot be blank");
        }
        this.id = id.trim();
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
        this.serviceIdOverride = (serviceIdOverride != null && !serviceIdOverride.trim().isEmpty())
                ? serviceIdOverride.trim()
                : null;
        if (responseTarget == null || responseTarget.isNegative() || responseTarget.isZero()) {
            throw new ValidationException("responseTarget", "Response target must be positive");
        }
        if (resolutionTarget == null || resolutionTarget.isNegative() || resolutionTarget.isZero()) {
            throw new ValidationException("resolutionTarget", "Resolution target must be positive");
        }
        if (resolutionTarget.compareTo(responseTarget) < 0) {
            throw new ValidationException("resolutionTarget", "Resolution target cannot be shorter than response target");
        }
        if (warningThresholdRatio <= 0.0 || warningThresholdRatio >= 1.0) {
            throw new ValidationException("warningThresholdRatio", "Warning threshold must be between 0.0 and 1.0");
        }
        this.responseTarget = responseTarget;
        this.resolutionTarget = resolutionTarget;
        this.warningThresholdRatio = warningThresholdRatio;
    }

    public String getId() {
        return id;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getServiceIdOverride() {
        return serviceIdOverride;
    }

    public boolean isServiceOverride() {
        return serviceIdOverride != null;
    }

    public Duration getResponseTarget() {
        return responseTarget;
    }

    public Duration getResolutionTarget() {
        return resolutionTarget;
    }

    public double getWarningThresholdRatio() {
        return warningThresholdRatio;
    }

    /**
     * Factory defaults as specified in Section 8.5:
     * P1: 5m / 30m, P2: 15m / 2h, P3: 1h / 8h, P4: 4h / 24h.
     */
    public static SlaPolicy defaultFor(Priority priority) {
        return switch (priority) {
            case P1 -> new SlaPolicy("SLA-DEF-P1", Priority.P1, null, Duration.ofMinutes(5), Duration.ofMinutes(30), 0.75);
            case P2 -> new SlaPolicy("SLA-DEF-P2", Priority.P2, null, Duration.ofMinutes(15), Duration.ofHours(2), 0.75);
            case P3 -> new SlaPolicy("SLA-DEF-P3", Priority.P3, null, Duration.ofHours(1), Duration.ofHours(8), 0.75);
            case P4 -> new SlaPolicy("SLA-DEF-P4", Priority.P4, null, Duration.ofHours(4), Duration.ofHours(24), 0.75);
        };
    }
}
