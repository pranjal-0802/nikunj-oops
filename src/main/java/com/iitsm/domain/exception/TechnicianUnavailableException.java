package com.iitsm.domain.exception;

/**
 * OOP: Thrown when no eligible technician is online or within capacity.
 */
public class TechnicianUnavailableException extends IitsmException {

    public TechnicianUnavailableException(String message) {
        super("ERR_TECHNICIAN_UNAVAILABLE", message);
    }
}

