package com.example.coursewaitlist.application;

public class RequestException extends RuntimeException {
    public enum Kind { INVALID, NOT_FOUND, INELIGIBLE, CONFLICT }

    private final Kind kind;

    public RequestException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
