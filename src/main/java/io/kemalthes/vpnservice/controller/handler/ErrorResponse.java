package io.kemalthes.vpnservice.controller.handler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

    private String exceptionClassName;

    private Integer status;

    private String message;

    private String path;
}
