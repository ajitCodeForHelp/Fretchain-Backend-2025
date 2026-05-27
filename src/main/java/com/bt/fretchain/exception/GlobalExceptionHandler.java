package com.bt.fretchain.exception;

import com.bt.fretchain.bean.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // 400 BAD REQUEST
    // =========================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseData> handleBadRequest(BadRequestException e) {

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(400)
                        .message(e.getMessage()) // KEY
                        .build()
        );
    }

    // =========================
    // 404 NOT FOUND
    // =========================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseData> handleNotFound(ResourceNotFoundException e) {

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(404)
                        .message(e.getMessage())
                        .build()
        );
    }

    // =========================
    // 409 DUPLICATE
    // =========================
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ResponseData> handleDuplicate(DuplicateResourceException e) {

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(409)
                        .message(e.getMessage())
                        .build()
        );
    }

    // =========================
    // 500 GLOBAL ERROR
    // =========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData> handleGeneric(Exception e) {

        log.error("Unexpected error", e);

        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(500)
                        .message("something.went.wrong")
                        .build()
        );
    }
}