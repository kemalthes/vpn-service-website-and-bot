package io.nesvpn.backendsiteservice.exception;

import io.nesvpn.backendsiteservice.controller.handler.ExceptionResponse;
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
        @ApiResponse(responseCode = "403",
                content = @Content(
                        schema = @Schema(implementation = ExceptionResponse.class),
                        mediaType = "application/json",
                        examples = @ExampleObject(value = """
                            {
                               "exceptionClassName": "ForbiddenException",
                               "status": 403,
                               "message": "Запрещено",
                               "path": "path"
                            }
                        """)))
})
public @interface Swagger403 {
}
