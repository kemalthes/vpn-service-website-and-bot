package io.nesvpn.backendsiteservice.controller.handler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionResponse {

    private String exceptionClassName;

    private Integer status;

    private String message;

    private String path;
}
