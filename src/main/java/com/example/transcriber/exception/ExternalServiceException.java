package com.example.audiototext.exception;

public class ExternalServiceException extends RuntimeException {
    private final boolean serviceUnavailable;

    public ExternalServiceException(String message) {
        super(message);
        this.serviceUnavailable = false;
    }

    public ExternalServiceException(String message, boolean serviceUnavailable) {
        super(message);
        this.serviceUnavailable = serviceUnavailable;
    }

    public boolean isServiceUnavailable() {
        return serviceUnavailable;
    }
}
