package com.iitsm.domain.exception;

/**
 * OOP: Thrown when authentication fails due to bad credentials or account lockout.
 */
public class InvalidCredentialsException extends IitsmException {

    public InvalidCredentialsException(String message) {
        super("ERR_INVALID_CREDENTIALS", message);
    }
}
