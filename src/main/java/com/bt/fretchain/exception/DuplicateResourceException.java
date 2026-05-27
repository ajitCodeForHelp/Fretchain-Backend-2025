package com.bt.fretchain.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String messageKey) {
        super(messageKey);
    }
}