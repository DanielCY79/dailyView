package com.daniel.dailyView.exception;

public class ExternalDataAccessException extends RuntimeException {

    public ExternalDataAccessException(String message) {
        super(message);
    }

    public ExternalDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
