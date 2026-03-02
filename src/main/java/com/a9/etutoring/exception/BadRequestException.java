package com.a9.etutoring.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }
}
