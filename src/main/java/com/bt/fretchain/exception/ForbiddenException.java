package com.bt.fretchain.exception;

/** The logged-in user is not allowed to perform this action. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String messageKey) {
        super(messageKey);
    }
}
