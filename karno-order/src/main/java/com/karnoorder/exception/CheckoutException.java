package com.karnoorder.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CheckoutException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CheckoutException(
            HttpStatus status,
            String code,
            String message
    ) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
