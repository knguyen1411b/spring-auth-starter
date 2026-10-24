package org.app.backend.common.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "404",
    description = "Resource not found",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = org.app.backend.common.response.ApiResponse.class),
            examples = {
              @ExampleObject(
                  value =
                      """
                                                    {
                                                      "success": false,
                                                      "statusCode": 404,
                                                      "message": "Resource not found"
                                                    }
                                                    """)
            }))
public @interface NotFoundApiResponse {}
