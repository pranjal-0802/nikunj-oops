package com.iitsm.domain.state;

import com.iitsm.domain.model.IncidentStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pattern: State / Flyweight Registry.
 * Holds stateless singleton instances of each IncidentState implementation in an EnumMap for fast lookup.
 */
public final class IncidentStates {

    // EnumMap chosen deliberately for compact memory footprint and O(1) array-indexed lookup
    private static final Map<IncidentStatus, IncidentState> REGISTRY = new EnumMap<>(IncidentStatus.class);

    static {
        REGISTRY.put(IncidentStatus.NEW, new NewState());
        REGISTRY.put(IncidentStatus.CLASSIFIED, new ClassifiedState());
        REGISTRY.put(IncidentStatus.ASSIGNED, new AssignedState());
        REGISTRY.put(IncidentStatus.ACKNOWLEDGED, new AcknowledgedState());
        REGISTRY.put(IncidentStatus.IN_PROGRESS, new InProgressState());
        REGISTRY.put(IncidentStatus.PENDING_INFO, new PendingInfoState());
        REGISTRY.put(IncidentStatus.RESOLVED, new ResolvedState());
        REGISTRY.put(IncidentStatus.VERIFIED, new VerifiedState());
        REGISTRY.put(IncidentStatus.CLOSED, new ClosedState());
    }

    private IncidentStates() {
        // Prevent instantiation
    }

    public static IncidentState of(IncidentStatus status) {
        Objects.requireNonNull(status, "IncidentStatus cannot be null");
        IncidentState state = REGISTRY.get(status);
        if (state == null) {
            throw new IllegalStateException("No IncidentState registered for status: " + status);
        }
        return state;
    }
}
