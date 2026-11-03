package com.bt.fretchain.exception;

import com.bt.fretchain.bean.ResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseData> handleBadRequest(BadRequestException e) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(400)
                        .message(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseData> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(404)
                        .message(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ResponseData> handleDuplicate(DuplicateResourceException e) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(409)
                        .message(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseData> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(401)
                        .message(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseData> handleForbidden(ForbiddenException e) {
        return ResponseEntity.ok(
                ResponseData.builder()
                        .statusCode(403)
                        .message(e.getMessage())
                        .build()
        );
    }

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