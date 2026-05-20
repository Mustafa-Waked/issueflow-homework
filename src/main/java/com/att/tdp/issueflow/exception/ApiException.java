package com.att.tdp.issueflow.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
