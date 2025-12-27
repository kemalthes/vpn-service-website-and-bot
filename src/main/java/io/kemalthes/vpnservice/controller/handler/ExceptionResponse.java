package io.kemalthes.vpnservice.controller.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "exception response")
public class ExceptionResponse {

    private String exceptionClassName;

    private Integer status;

    private String message;

    private String path;
}
