package com.example.ecommerce.mail.specification;

import com.example.ecommerce.mail.entity.EmailLogEntity;
import com.example.ecommerce.mail.enums.EmailStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EmailLogSpecification {

    public static Specification<EmailLogEntity> withFilters(
            EmailStatus status,
            String recipientEmail,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (recipientEmail != null && !recipientEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("recipientEmail")), "%" + recipientEmail.trim().toLowerCase() + "%"));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate subjectMatch = cb.like(cb.lower(root.get("subject")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("recipientEmail")), pattern);
                Predicate errorMatch = cb.like(cb.lower(root.get("lastErrorMessage")), pattern);
                predicates.add(cb.or(subjectMatch, emailMatch, errorMatch));
            }

            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }

            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
