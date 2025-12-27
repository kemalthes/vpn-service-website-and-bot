package io.kemalthes.vpnservice.controller.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleAny(Exception ex, HttpServletRequest request) {
        log.error("Unhandled {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ExceptionResponse body = ExceptionResponse.builder()
                .exceptionClassName(ex.getClass().getName())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal Server Error. UnexpectedError")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(500).body(body);
    }
}
