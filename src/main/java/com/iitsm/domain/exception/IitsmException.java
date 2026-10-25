package com.iitsm.domain.exception;

/**
 * OOP: Base unchecked exception for all domain and application errors in I-ITSM.
 * Every exception carries a machine-readable error code for consistent client mapping.
 */
public abstract class IitsmException extends RuntimeException {

    private final String errorCode;

    protected IitsmException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected IitsmException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

