package com.example.ecommerce.invoice.specification;

import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {

    public static Specification<InvoiceEntity> withFilters(
            Long customerId,
            InvoiceStatus status,
            LocalDateTime issueDateFrom,
            LocalDateTime issueDateTo,
            String keyword
    ) {
        return Specification.allOf(
                byCustomerId(customerId),
                byStatus(status),
                byIssueDateFrom(issueDateFrom),
                byIssueDateTo(issueDateTo),
                byKeyword(keyword)
        );
    }

    private static Specification<InvoiceEntity> byCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    private static Specification<InvoiceEntity> byStatus(InvoiceStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    private static Specification<InvoiceEntity> byIssueDateFrom(LocalDateTime issueDateFrom) {
        return (root, query, cb) -> {
            if (issueDateFrom == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("issueDate"), issueDateFrom);
        };
    }

    private static Specification<InvoiceEntity> byIssueDateTo(LocalDateTime issueDateTo) {
        return (root, query, cb) -> {
            if (issueDateTo == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("issueDate"), issueDateTo);
        };
    }

    private static Specification<InvoiceEntity> byKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("invoiceNumber")), searchPattern));
            predicates.add(cb.like(cb.lower(root.get("billingName")), searchPattern));
            predicates.add(cb.like(cb.lower(root.get("billingEmail")), searchPattern));
            predicates.add(cb.like(cb.lower(root.get("billingPhone")), searchPattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
