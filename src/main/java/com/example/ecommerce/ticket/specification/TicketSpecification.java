package com.example.ecommerce.ticket.specification;

import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketSpecification {

    public static Specification<PurchaseTicketEntity> withFilters(
            Long customerId,
            TicketStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            String keyword
    ) {
        return notDeleted()
                .and(byCustomerId(customerId))
                .and(byStatus(status))
                .and(byCreatedAtFrom(createdFrom))
                .and(byCreatedAtTo(createdTo))
                .and(byKeyword(keyword));
    }

    private static Specification<PurchaseTicketEntity> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private static Specification<PurchaseTicketEntity> byCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return null;
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    private static Specification<PurchaseTicketEntity> byStatus(TicketStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    private static Specification<PurchaseTicketEntity> byCreatedAtFrom(LocalDateTime createdFrom) {
        return (root, query, cb) -> {
            if (createdFrom == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    private static Specification<PurchaseTicketEntity> byCreatedAtTo(LocalDateTime createdTo) {
        return (root, query, cb) -> {
            if (createdTo == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }

    private static Specification<PurchaseTicketEntity> byKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("ticketCode")), pattern));
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            predicates.add(cb.like(cb.lower(root.get("customer").get("name")), pattern));
            predicates.add(cb.like(cb.lower(root.get("customer").get("email")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
