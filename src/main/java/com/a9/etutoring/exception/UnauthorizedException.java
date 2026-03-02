package com.a9.etutoring.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
