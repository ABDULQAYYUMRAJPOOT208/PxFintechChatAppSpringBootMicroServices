package com.pxfintech.authentication_service.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String message) {
        super(message);
    }
}