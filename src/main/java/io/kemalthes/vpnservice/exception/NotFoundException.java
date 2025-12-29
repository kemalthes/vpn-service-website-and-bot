package io.kemalthes.vpnservice.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ServiceException{

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND.value());
    }
}
