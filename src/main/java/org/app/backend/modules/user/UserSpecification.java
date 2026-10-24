package org.app.backend.modules.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.app.backend.modules.role.Role;
import org.app.backend.modules.user.dto.UserFilterDTO;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {
  public static Specification<User> filter(UserFilterDTO filter) {

    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (filter.getQ() != null && !filter.getQ().isBlank()) {
        String like = "%" + filter.getQ().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("fullName")), like)));
      }
      if (filter.getActive() != null) {
        predicates.add(cb.equal(root.get("active"), filter.getActive()));
      }
      if (filter.getLocked() != null) {
        predicates.add(cb.equal(root.get("locked"), filter.getLocked()));
      }
      if (filter.getSuperuser() != null) {
        predicates.add(cb.equal(root.get("superuser"), filter.getSuperuser()));
      }
      if (filter.getRoleName() != null && !filter.getRoleName().isBlank()) {
        Join<User, Role> roleJoin = root.join("roles", JoinType.LEFT);
        predicates.add(
            cb.equal(cb.lower(roleJoin.get("code")), filter.getRoleName().toLowerCase()));
        Objects.requireNonNull(query).distinct(true);
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
