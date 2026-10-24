package org.app.backend.common.validation;

import java.util.Set;
import org.app.backend.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

public final class PageableValidation {
  private PageableValidation() {}

  public static void validate(Pageable pageable, Set<String> fields) {
    for (var order : pageable.getSort()) {
      if (!fields.contains(order.getProperty())) {
        throw new AppException(
            HttpStatus.BAD_REQUEST, "Unsupported sort field: " + order.getProperty());
      }
    }
  }
}
