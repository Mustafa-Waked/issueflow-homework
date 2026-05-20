package com.att.tdp.issueflow.exception;

import com.att.tdp.issueflow.dto.CommonDtos;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonDtos.ApiError handleUnauthorized(UnauthorizedException ex) {
        return new CommonDtos.ApiError("UNAUTHORIZED", ex.getMessage(), null, Instant.now());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonDtos.ApiError handleNotFound(NotFoundException ex) {
        return new CommonDtos.ApiError("NOT_FOUND", ex.getMessage(), null, Instant.now());
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonDtos.ApiError handleBadRequest(Exception ex) {
        return new CommonDtos.ApiError("BAD_REQUEST", ex.getMessage(), null, Instant.now());
    }

    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public CommonDtos.ApiError handleConflict(Exception ex) {
        return new CommonDtos.ApiError("CONFLICT", ex.getMessage(), null, Instant.now());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonDtos.ApiError handleUnreadable(Exception ex) {
        return new CommonDtos.ApiError("BAD_REQUEST", "Invalid request body or parameter", null, Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonDtos.ApiError handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return new CommonDtos.ApiError("VALIDATION_ERROR", "Validation failed", errors, Instant.now());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonDtos.ApiError handleForbidden(AccessDeniedException ex) {
        return new CommonDtos.ApiError("FORBIDDEN", ex.getMessage(), null, Instant.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonDtos.ApiError handleGeneric(Exception ex) {
        return new CommonDtos.ApiError("INTERNAL_ERROR", "An unexpected error occurred", null, Instant.now());
    }
}
