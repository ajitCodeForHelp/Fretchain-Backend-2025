package com.bt.fretchain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String messageKey) {
        super(messageKey);
    }
}