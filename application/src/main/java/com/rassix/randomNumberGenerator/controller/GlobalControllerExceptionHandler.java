package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.controller.dto.ErrorResponse;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> exceptionResponse(MethodArgumentNotValidException ex) {
        List<String> errorMessages = new ArrayList<>();

        for(ObjectError error : ex.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errorMessages.add(fieldError.getField() + " " + fieldError.getDefaultMessage());
            } else {
                errorMessages.add(error.getObjectName() + " " + error.getDefaultMessage());
            }
        }

        return new ResponseEntity<ErrorResponse>(
            new ErrorResponse("INVALID_REQUEST", String.join(", ", errorMessages)),
            HttpStatus.BAD_REQUEST
        );
    }


    @ExceptionHandler(PlayerExistsException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> exceptionResponse(PlayerExistsException ex) {
        return new ResponseEntity<>(
            new ErrorResponse("PLAYER_EXISTS", ex.getMessage()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> exceptionResponse(Exception ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse("UNKNOWN_ERROR", "Internal server error"),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}
