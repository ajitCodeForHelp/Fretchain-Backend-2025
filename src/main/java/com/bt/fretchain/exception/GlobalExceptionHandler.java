package com.bt.fretchain.exception;

import com.bt.fretchain.bean.ResponsePacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired private ResourceBundleMessageSource messageSource;

    // 500 > Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandling(Exception e, WebRequest request) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ResponseEntity<>(ResponsePacket.builder()
                .errorCode(1)
                .message(e.getMessage())
                .build(), HttpStatus.OK);
    }

    // 400 > Bad Request
    @ExceptionHandler(com.bt.cloudLibrary.exception.BadRequestException.class)
    public ResponseEntity<?> invalidDataExceptionHandling(Locale locale, com.bt.cloudLibrary.exception.BadRequestException e, WebRequest request) {
        log.debug(e.getMessage());
        String message = e.getMessage();
        if (message.startsWith("cloudLibrary")) {
            message = messageSource.getMessage(e.getMessage(), null, locale);
        }
        return new ResponseEntity<>(ResponsePacket.builder()
                .errorCode(1)
                .message(message)
                .build(), HttpStatus.OK);
    }

}
