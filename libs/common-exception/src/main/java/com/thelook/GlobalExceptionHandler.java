package com.thelook;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.exceptions.StandardError;
import com.thelook.exceptions.UnprocessableRequestException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        String error  = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> handleInvalidRequest(ConstraintViolationException e, HttpServletRequest request) {
        String error  = "Invalid request";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String error  = "Invalid parameters";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(IncompleteProfileException.class)
    public ResponseEntity<StandardError> handleIncompleteProfileException(IncompleteProfileException e, HttpServletRequest request) {
        String error  = "User profile is not complete";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UnprocessableRequestException.class)
    public ResponseEntity<StandardError> handleUnprocessableRequestException(UnprocessableRequestException e, HttpServletRequest request) {
        String error  = "Not possible to execute this request";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<StandardError> handleBusinessRuleException(BusinessRuleException e, HttpServletRequest request) {
        String error  = "Request not allowed";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleJsonParseError(HttpMessageNotReadableException e, HttpServletRequest request) {
        String error = "Error processing request (invalid JSON)";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMostSpecificCause().getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralExceptions(Exception e, HttpServletRequest request) {
        String error  = "Unknown error";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

}
