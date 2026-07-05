package com.bookverse.exception;

import lombok.Getter;

@Getter
public class BookVerseException extends RuntimeException {
    private final int statusCode;

    public BookVerseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
