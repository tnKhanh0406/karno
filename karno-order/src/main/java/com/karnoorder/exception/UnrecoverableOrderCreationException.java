package com.karnoorder.exception;

public class UnrecoverableOrderCreationException extends RuntimeException {

    public UnrecoverableOrderCreationException(String message) {
        super(message);
    }

    public UnrecoverableOrderCreationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
