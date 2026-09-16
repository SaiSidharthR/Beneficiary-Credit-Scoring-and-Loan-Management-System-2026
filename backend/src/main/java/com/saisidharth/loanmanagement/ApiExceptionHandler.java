package com.saisidharth.loanmanagement;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.saisidharth.loanmanagement.beneficiary.BeneficiaryNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BeneficiaryNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BeneficiaryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), Instant.now()));
    }

    public record ApiError(int status, String message, Instant timestamp) {
    }
}
