package com.example.ecommerce.testdata;

import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.enums.UserRole;
import com.example.ecommerce.enums.UserStatus;
import java.time.LocalDateTime;

/**
 * Builder for creating test UserEntity instances.
 * Provides sensible defaults for common test scenarios.
 *
 * Usage:
 * UserEntity user = UserTestDataBuilder.aUser()
 *     .withUsername("testuser")
 *     .withEmail("test@example.com")
 *     .build();
 */
public class UserTestDataBuilder {

    private Long id = null;
    private String username = "testuser";
    private String email = "test@example.com";
    private String password = "$2a$10$dXJ3SW6G7P50eS6DjMmnCOYvFH7R60s8KV4Yy.E0jC6tQ9Xt8KmVa"; // bcrypt of "password"
    private UserRole role = UserRole.ADMIN;
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    public UserTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestDataBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestDataBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestDataBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public UserEntity build() {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus(status);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }
}
