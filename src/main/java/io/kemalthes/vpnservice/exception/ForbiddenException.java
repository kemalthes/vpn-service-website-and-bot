package io.kemalthes.vpnservice.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ServiceException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN.value());
    }
}
