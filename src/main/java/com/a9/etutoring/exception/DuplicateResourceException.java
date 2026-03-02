package com.a9.etutoring.exception;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String code, String message) {
        super(code, message);
    }
}
