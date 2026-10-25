package com.iitsm.domain.exception;

/**
 * OOP: Thrown when an optimistic lock conflict, concurrency collision, or storage failure occurs.
 */
public class PersistenceFailureException extends IitsmException {

    public PersistenceFailureException(String message, Throwable cause) {
        super("ERR_PERSISTENCE_FAILURE", message, cause);
    }

    public PersistenceFailureException(String message) {
        super("ERR_PERSISTENCE_FAILURE", message);
    }
}

