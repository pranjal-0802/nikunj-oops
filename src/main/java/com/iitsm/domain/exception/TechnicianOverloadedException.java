package com.iitsm.domain.exception;

/**
 * OOP: Thrown when an assignment would breach a technician's maximum workload capacity.
 */
public class TechnicianOverloadedException extends IitsmException {

    public TechnicianOverloadedException(String technicianId, int currentLoad, int maxCapacity) {
        super(
                "ERR_TECHNICIAN_OVERLOADED",
                String.format(
                        "Technician '%s' is overloaded: active load is %d, maximum capacity is %d",
                        technicianId, currentLoad, maxCapacity));
    }
}

