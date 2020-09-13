package com.phyzicsz.parsec.reflections.exception;

public class ReflectionsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReflectionsException(String message) {
        super(message);
    }

    public ReflectionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectionsException(Throwable cause) {
        super(cause);
    }
}
