package com.ll.Yuruppang.global.exceptions.handler;

import com.ll.Yuruppang.global.exceptions.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> handle(ServiceException ex) {

        String message = ex.getMsg();

        return ResponseEntity
                .status(ex.getResultCode())
                .body(message);
    }
}
