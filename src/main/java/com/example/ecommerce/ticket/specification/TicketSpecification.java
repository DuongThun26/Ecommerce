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
            String keyword,
            Long categoryId,
            Long poUserId
    ) {
        return notDeleted()
                .and(byCustomerId(customerId))
                .and(byStatus(status))
                .and(byCreatedAtFrom(createdFrom))
                .and(byCreatedAtTo(createdTo))
                .and(byKeyword(keyword))
                .and(byCategoryId(categoryId))
                .and(byCategoryManagedByUser(poUserId));
    }

    private static Specification<PurchaseTicketEntity> byCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            query.distinct(true);

            // SubOrder order items category
            var subOrderJoin = root.join("subOrder", jakarta.persistence.criteria.JoinType.LEFT);
            var subOrderItemsJoin = subOrderJoin.join("orderItems", jakarta.persistence.criteria.JoinType.LEFT);
            var subProductJoin = subOrderItemsJoin.join("product", jakarta.persistence.criteria.JoinType.LEFT);
            var subCategoryPredicate = cb.equal(subProductJoin.get("category").get("id"), categoryId);

            // Fallback for legacy tickets without subOrder
            var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);
            var orderItemsJoin = orderJoin.join("orderItems", jakarta.persistence.criteria.JoinType.LEFT);
            var productJoin = orderItemsJoin.join("product", jakarta.persistence.criteria.JoinType.LEFT);
            var legacyCategoryPredicate = cb.and(cb.isNull(root.get("subOrder")), cb.equal(productJoin.get("category").get("id"), categoryId));

            return cb.or(subCategoryPredicate, legacyCategoryPredicate);
        };
    }

    private static Specification<PurchaseTicketEntity> byCategoryManagedByUser(Long poUserId) {
        return (root, query, cb) -> {
            if (poUserId == null) {
                return null;
            }
            query.distinct(true);

            // 1. SubOrder managedBy
            var subOrderJoin = root.join("subOrder", jakarta.persistence.criteria.JoinType.LEFT);
            var subOrderManagedBy = cb.equal(subOrderJoin.get("managedBy").get("id"), poUserId);

            // 2. SubOrder's order items category managedBy
            var subOrderItemsJoin = subOrderJoin.join("orderItems", jakarta.persistence.criteria.JoinType.LEFT);
            var subProductJoin = subOrderItemsJoin.join("product", jakarta.persistence.criteria.JoinType.LEFT);
            var subCategoryJoin = subProductJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);
            var subCategoryManagedBy = cb.equal(subCategoryJoin.get("managedBy").get("id"), poUserId);

            // 3. Fallback for legacy tickets without subOrder
            var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);
            var orderItemsJoin = orderJoin.join("orderItems", jakarta.persistence.criteria.JoinType.LEFT);
            var productJoin = orderItemsJoin.join("product", jakarta.persistence.criteria.JoinType.LEFT);
            var categoryJoin = productJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);
            var legacyCategoryManagedBy = cb.and(cb.isNull(root.get("subOrder")), cb.equal(categoryJoin.get("managedBy").get("id"), poUserId));

            return cb.or(subOrderManagedBy, subCategoryManagedBy, legacyCategoryManagedBy);
        };
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
            if (status == TicketStatus.PENDING_APPROVAL) {
                var subOrderJoin = root.join("subOrder", jakarta.persistence.criteria.JoinType.LEFT);
                var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);

                var ticketPending = cb.equal(root.get("status"), TicketStatus.PENDING_APPROVAL);
                var subOrderNotCancelled = cb.or(cb.isNull(root.get("subOrder")), cb.notEqual(subOrderJoin.get("status"), com.example.ecommerce.order.enums.SubOrderStatus.CANCELLED));
                var orderNotCancelled = cb.or(cb.isNull(root.get("order")), cb.notEqual(orderJoin.get("status"), com.example.ecommerce.order.enums.OrderStatus.CANCELLED));

                return cb.and(ticketPending, subOrderNotCancelled, orderNotCancelled);
            }
            if (status == TicketStatus.CANCELLED) {
                var subOrderJoin = root.join("subOrder", jakarta.persistence.criteria.JoinType.LEFT);
                var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);

                var ticketCancelled = cb.equal(root.get("status"), TicketStatus.CANCELLED);
                var subOrderCancelled = cb.and(cb.isNotNull(root.get("subOrder")), cb.equal(subOrderJoin.get("status"), com.example.ecommerce.order.enums.SubOrderStatus.CANCELLED));
                var orderCancelled = cb.and(cb.isNotNull(root.get("order")), cb.equal(orderJoin.get("status"), com.example.ecommerce.order.enums.OrderStatus.CANCELLED));

                return cb.or(ticketCancelled, subOrderCancelled, orderCancelled);
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
