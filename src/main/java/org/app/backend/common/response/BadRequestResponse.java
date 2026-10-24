package org.app.backend.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for bad request errors")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BadRequestResponse {
  @Schema(
      description =
          "Map of field errors where the key is the field name and the value is the error message",
      example = "{\"username\": \"Username is required\", \"email\": \"Email format is invalid\"}")
  Map<String, String> errors;

  /**
   * Builds a {@link BadRequestResponse} from a {@link MethodArgumentNotValidException}.
   *
   * <p>This method extracts field errors from the exception and converts them into a map. If
   * multiple validation errors exist for the same field, their messages are concatenated with a
   * semicolon.
   *
   * @param ex The {@link MethodArgumentNotValidException} containing validation error details.
   * @return A populated {@link BadRequestResponse} with extracted field errors.
   */
  public static BadRequestResponse from(MethodArgumentNotValidException ex) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                    (existing, replacement) -> existing + "; " + replacement));

    return BadRequestResponse.builder().errors(errors).build();
  }

  /**
   * Builds a {@link BadRequestResponse} from a {@link MethodArgumentTypeMismatchException}.
   *
   * <p>This method extracts the field name, invalid value, and expected type from the exception to
   * create a descriptive error message.
   *
   * @param ex The {@link MethodArgumentTypeMismatchException} containing type mismatch details.
   * @return A populated {@link BadRequestResponse} with the type mismatch error.
   */
  public static BadRequestResponse from(MethodArgumentTypeMismatchException ex) {
    String field = ex.getName();
    Object value = ex.getValue();
    Class<?> requiredType = ex.getRequiredType();

    String message =
        String.format(
            "Invalid value '%s'%s.",
            value, requiredType != null ? ". Expected type: " + requiredType.getSimpleName() : "");

    return BadRequestResponse.builder().errors(Map.of(field, message)).build();
  }
}
