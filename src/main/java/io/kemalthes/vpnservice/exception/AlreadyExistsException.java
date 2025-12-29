package io.kemalthes.vpnservice.exception;

import org.springframework.http.HttpStatus;

public class AlreadyExistsException extends ServiceException {

    public AlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT.value());
    }
}
