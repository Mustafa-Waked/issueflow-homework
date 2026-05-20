package com.att.tdp.issueflow.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(message);
    }
}
