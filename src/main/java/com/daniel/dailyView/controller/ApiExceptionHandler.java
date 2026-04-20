package com.daniel.dailyView.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.daniel.dailyView.exception.ExternalDataAccessException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ExternalDataAccessException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleExternalDataAccess(ExternalDataAccessException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        detail.setTitle("External data source unavailable");
        return detail;
    }
}
