package com.bt.cloudLibrary.exception;

public class InvalidDatabaseAccessException extends Exception {

    public InvalidDatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDatabaseAccessException(String message) {
        super(message);
    }

    public InvalidDatabaseAccessException(Throwable cause) {
        super(cause);
    }

}
