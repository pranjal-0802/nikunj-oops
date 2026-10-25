package com.iitsm.domain.exception;

/**
 * OOP: Thrown when an actor attempts an action without adequate ownership or role privileges.
 */
public class UnauthorizedOperationException extends IitsmException {

    public UnauthorizedOperationException(String message) {
        super("ERR_UNAUTHORIZED_OPERATION", message);
    }
}

