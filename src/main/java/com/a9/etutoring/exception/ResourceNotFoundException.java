package com.a9.etutoring.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
