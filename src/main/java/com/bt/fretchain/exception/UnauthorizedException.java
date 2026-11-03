package com.bt.fretchain.exception;

/** No (valid) login for an endpoint that needs one. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String messageKey) {
        super(messageKey);
    }
}
