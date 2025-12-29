package io.kemalthes.vpnservice.controller.handler;

import io.kemalthes.vpnservice.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ExceptionResponse> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error("Service exception {} {}: {} - {}", request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage(), e);
        ExceptionResponse body = ExceptionResponse.builder()
                .exceptionClassName(e.getClass().getSimpleName())
                .message(e.getMessage())
                .status(e.getStatus())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        log.error("Validation exception {} {}: {}, errors: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), errors, ex);
        ExceptionResponse response = ExceptionResponse.builder()
                .exceptionClassName(ex.getClass().getSimpleName())
                .message(errors.toString())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(response);
    }
}
