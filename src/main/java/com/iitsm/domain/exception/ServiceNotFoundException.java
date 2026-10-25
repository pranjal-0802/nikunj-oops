package com.iitsm.domain.exception;

/**
 * OOP: Thrown when an IT service cannot be resolved in the service catalog.
 */
public class ServiceNotFoundException extends IitsmException {

    public ServiceNotFoundException(String serviceIdentifier) {
        super(
                "ERR_SERVICE_NOT_FOUND",
                String.format("IT Service '%s' was not found in the service catalogue", serviceIdentifier));
    }
}

