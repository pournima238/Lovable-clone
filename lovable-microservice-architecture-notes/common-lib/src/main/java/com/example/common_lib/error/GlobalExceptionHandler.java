package com.example.common_lib.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.SignatureException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());
        log.error(error.toString(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND, ex.getResourceName() + "with id: " + ex.getResourceId() + "Not found");
        log.error(error.toString(), ex.getResourceName() + "with id: " + ex.getResourceId() + "Not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBadInvalidInputRequest(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream().map(error -> new ApiFieldErrors(error.getField(), error.getDefaultMessage())).toList();

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
        log.error(error.toString(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ==========================================
    // JWT Authentication Exception Handlers
    // ==========================================

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiError> handleExpiredJwtException(ExpiredJwtException ex) {
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED, "Token has expired. Please log in again.");
        log.error("JWT Expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiError> handleSignatureException(SignatureException ex) {
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED, "Invalid JWT signature.");
        log.error("JWT Signature Invalid: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    @ExceptionHandler({MalformedJwtException.class, UnsupportedJwtException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleInvalidJwtTokens(Exception ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "Invalid JWT token format.");
        log.error("JWT Token Invalid: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
