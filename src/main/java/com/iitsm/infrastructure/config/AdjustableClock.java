package com.iitsm.infrastructure.config;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pattern: Decorator / Virtual Clock.
 * Wraps a base java.time.Clock, allowing dynamic time acceleration (x1, x60, x600)
 * and discrete fast-forward jumps without altering the system clock.
 * Thread-safe for simulation execution and test deterministic evaluation.
 */
public class AdjustableClock extends Clock implements Serializable {

    private final ZoneId zone;
    private final AtomicReference<Instant> baseInstant;
    private final AtomicLong baseSystemNanos;
    private final AtomicLong speedMultiplier; // 1, 60, 600, etc.

    public AdjustableClock(Instant initialInstant, ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone cannot be null");
        this.baseInstant = new AtomicReference<>(Objects.requireNonNull(initialInstant, "initialInstant cannot be null"));
        this.baseSystemNanos = new AtomicLong(System.nanoTime());
        this.speedMultiplier = new AtomicLong(1L);
    }

    public static AdjustableClock of(Instant initialInstant, ZoneId zone) {
        return new AdjustableClock(initialInstant, zone);
    }

    public static AdjustableClock ofUtc(Instant initialInstant) {
        return new AdjustableClock(initialInstant, ZoneId.of("UTC"));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        AdjustableClock copy = new AdjustableClock(instant(), zone);
        copy.setSpeedMultiplier(this.getSpeedMultiplier());
        return copy;
    }

    @Override
    public Instant instant() {
        long currentNanos = System.nanoTime();
        long elapsedNanos = currentNanos - baseSystemNanos.get();
        long virtualElapsedNanos = elapsedNanos * speedMultiplier.get();
        return baseInstant.get().plusNanos(virtualElapsedNanos);
    }

    /**
     * Fast-forwards the clock by a specified duration immediately.
     */
    public synchronized void fastForward(Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        Instant current = instant();
        baseInstant.set(current.plus(duration));
        baseSystemNanos.set(System.nanoTime());
    }

    /**
     * Sets the clock to an exact target instant.
     */
    public synchronized void setTimeTo(Instant target) {
        Objects.requireNonNull(target, "target cannot be null");
        baseInstant.set(target);
        baseSystemNanos.set(System.nanoTime());
    }

    /**
     * Sets the speed multiplier (e.g. 1 for real time, 60 for 1m/sec, 600 for 10m/sec).
     */
    public synchronized void setSpeedMultiplier(long multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be positive: " + multiplier);
        }
        Instant current = instant();
        baseInstant.set(current);
        baseSystemNanos.set(System.nanoTime());
        this.speedMultiplier.set(multiplier);
    }

    public long getSpeedMultiplier() {
        return speedMultiplier.get();
    }

    /**
     * Resets clock to current real-world UTC time at x1 speed.
     */
    public synchronized void resetToRealTime() {
        baseInstant.set(Instant.now());
        baseSystemNanos.set(System.nanoTime());
        speedMultiplier.set(1L);
    }
}

