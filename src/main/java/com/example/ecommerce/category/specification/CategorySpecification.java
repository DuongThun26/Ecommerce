package com.example.ecommerce.category.specification;

import com.example.ecommerce.category.entity.CategoryEntity;
import org.springframework.data.jpa.domain.Specification;

public class CategorySpecification {

    public static Specification<CategoryEntity> withFilters(String keyword, Long managedByUserId, String managedByUsername) {
        return deletedFalse()
                .and(byKeyword(keyword))
                .and(byManagedByUser(managedByUserId))
                .and(byManagedByUsername(managedByUsername));
    }

    private static Specification<CategoryEntity> deletedFalse() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    private static Specification<CategoryEntity> byKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
            );
        };
    }

    private static Specification<CategoryEntity> byManagedByUser(Long managedByUserId) {
        return (root, query, cb) -> {
            if (managedByUserId == null) {
                return null;
            }
            return cb.equal(root.get("managedBy").get("id"), managedByUserId);
        };
    }

    private static Specification<CategoryEntity> byManagedByUsername(String managedByUsername) {
        return (root, query, cb) -> {
            if (managedByUsername == null || managedByUsername.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("managedBy").get("username")), managedByUsername.trim().toLowerCase());
        };
    }
}
