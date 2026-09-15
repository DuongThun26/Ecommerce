package com.example.ecommerce.testdata;

import com.example.ecommerce.entity.CustomerEntity;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.enums.CustomerStatus;

import java.time.LocalDateTime;

public class CustomerTestDataBuilder {

    private Long id = null;
    private String name = "Test Customer";
    private String email = "customer@example.com";
    private String phone = "0900000000";
    private CustomerStatus status = CustomerStatus.ACTIVE;
    private boolean deleted = false;
    private UserEntity user = null;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }

    public CustomerTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public CustomerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CustomerTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public CustomerTestDataBuilder withStatus(CustomerStatus status) {
        this.status = status;
        return this;
    }

    public CustomerTestDataBuilder withDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public CustomerTestDataBuilder withUser(UserEntity user) {
        this.user = user;
        return this;
    }

    public CustomerTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public CustomerTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public CustomerEntity build() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setStatus(status);
        customer.setDeleted(deleted);
        customer.setUser(user);
        customer.setCreatedAt(createdAt);
        customer.setUpdatedAt(updatedAt);
        return customer;
    }
}
