package io.kemalthes.vpnservice.exception;

import io.kemalthes.vpnservice.controller.handler.ExceptionResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404",
                content = @Content(
                        schema = @Schema(implementation = ExceptionResponse.class),
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                            {
                               "exceptionClassName": "NotFoundException",
                               "status": 404,
                               "message": "Сущность не найдена",
                               "path": "path"
                            }
                        """)))
})
public @interface Swagger404 {
}
