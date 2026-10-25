package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * OOP: Value Object / Lifecycle entity tracking SLA compliance for an incident.
 * Encapsulates response/resolution deadlines, pause intervals during PENDING_INFO, and status evaluation.
 */
public class SlaTracker {

    private final Instant createdAt;
    private Instant responseDueAt;
    private Instant resolutionDueAt;
    private Instant respondedAt;
    private Instant resolvedAt;
    private Duration pausedDuration;
    private Instant pausedAt;
    private SlaStatus status;

    public SlaTracker(Instant createdAt, DeadlinePair deadlines) {
        if (createdAt == null) {
            throw new ValidationException("createdAt", "Created timestamp cannot be null");
        }
        Objects.requireNonNull(deadlines, "deadlines cannot be null");
        this.createdAt = createdAt;
        this.responseDueAt = deadlines.responseDueAt();
        this.resolutionDueAt = deadlines.resolutionDueAt();
        this.pausedDuration = Duration.ZERO;
        this.pausedAt = null;
        this.status = SlaStatus.ON_TRACK;
    }

    public static SlaTracker create(Instant createdAt, SlaPolicy policy) {
        Objects.requireNonNull(policy, "policy cannot be null");
        Instant responseDue = createdAt.plus(policy.getResponseTarget());
        Instant resolutionDue = createdAt.plus(policy.getResolutionTarget());
        return new SlaTracker(createdAt, new DeadlinePair(responseDue, resolutionDue));
    }

    public static SlaTracker reconstitute(
            Instant createdAt,
            Instant responseDueAt,
            Instant resolutionDueAt,
            Instant respondedAt,
            Instant resolvedAt,
            Duration pausedDuration,
            Instant pausedAt,
            SlaStatus status) {
        SlaTracker tracker = new SlaTracker(createdAt, new DeadlinePair(responseDueAt, resolutionDueAt));
        tracker.respondedAt = respondedAt;
        tracker.resolvedAt = resolvedAt;
        tracker.pausedDuration = (pausedDuration != null) ? pausedDuration : Duration.ZERO;
        tracker.pausedAt = pausedAt;
        tracker.status = (status != null) ? status : SlaStatus.ON_TRACK;
        return tracker;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResponseDueAt() {
        return responseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Duration getPausedDuration() {
        return pausedDuration;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public boolean isPaused() {
        return pausedAt != null;
    }

    public SlaStatus getStatus() {
        return status;
    }

    public void recordResponse(Instant now) {
        Objects.requireNonNull(now, "response timestamp cannot be null");
        if (this.respondedAt == null) {
            this.respondedAt = now;
            evaluateStatus(now);
        }
    }

    public void recordResolution(Instant now) {
        Objects.requireNonNull(now, "resolution timestamp cannot be null");
        if (isPaused()) {
            resumeClock(now);
        }
        this.resolvedAt = now;
        if (now.isAfter(resolutionDueAt)) {
            this.status = SlaStatus.BREACHED;
        } else {
            this.status = SlaStatus.MET;
        }
    }

    public void pauseClock(Instant now) {
        Objects.requireNonNull(now, "pause timestamp cannot be null");
        if (!isPaused() && resolvedAt == null) {
            this.pausedAt = now;
        }
    }

    public void resumeClock(Instant now) {
        Objects.requireNonNull(now, "resume timestamp cannot be null");
        if (isPaused()) {
            Duration pausedWindow = Duration.between(pausedAt, now);
            if (!pausedWindow.isNegative()) {
                this.pausedDuration = this.pausedDuration.plus(pausedWindow);
                if (respondedAt == null) {
                    this.responseDueAt = this.responseDueAt.plus(pausedWindow);
                }
                if (resolvedAt == null) {
                    this.resolutionDueAt = this.resolutionDueAt.plus(pausedWindow);
                }
            }
            this.pausedAt = null;
            evaluateStatus(now);
        }
    }

    public void evaluateStatus(Instant now) {
        Objects.requireNonNull(now, "current timestamp cannot be null");
        if (status == SlaStatus.MET) {
            return;
        }

        // Check if response target breached
        if (respondedAt == null && now.isAfter(responseDueAt)) {
            this.status = SlaStatus.BREACHED;
            return;
        }

        // Check if resolution target breached
        if (resolvedAt != null) {
            this.status = resolvedAt.isAfter(resolutionDueAt) ? SlaStatus.BREACHED : SlaStatus.MET;
            return;
        } else if (now.isAfter(resolutionDueAt)) {
            this.status = SlaStatus.BREACHED;
            return;
        }

        // If clock is paused during PENDING_INFO, freeze status
        if (isPaused()) {
            return;
        }

        // Calculate progress percentage of resolution target
        Duration totalWindow = Duration.between(createdAt, resolutionDueAt).minus(pausedDuration);
        Duration elapsed = Duration.between(createdAt, now).minus(pausedDuration);

        if (totalWindow.toMillis() <= 0) {
            this.status = SlaStatus.BREACHED;
            return;
        }

        double ratio = (double) elapsed.toMillis() / (double) totalWindow.toMillis();
        if (ratio >= 0.90) {
            this.status = SlaStatus.WARNING;
        } else if (ratio >= 0.75) {
            this.status = SlaStatus.AT_RISK;
        } else {
            this.status = SlaStatus.ON_TRACK;
        }
    }
}

