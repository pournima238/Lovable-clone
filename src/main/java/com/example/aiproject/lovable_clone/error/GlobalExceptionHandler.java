package com.example.aiproject.lovable_clone.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex){
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST,ex.getMessage());
        log.error(error.toString(),ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex){
        ApiError error = new ApiError(HttpStatus.NOT_FOUND,ex.getResourceName()+"with id: "+ex.getResourceId() +"Not found");
        log.error(error.toString(),ex.getResourceName()+"with id: "+ex.getResourceId() +"Not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBadInvalidInputRequest(MethodArgumentNotValidException ex){
        var errors = ex.getBindingResult().getFieldErrors().stream().map(error->new ApiFieldErrors(error.getField(), error.getDefaultMessage())).toList();

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST,ex.getLocalizedMessage(),errors);
        log.error(error.toString(),ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
