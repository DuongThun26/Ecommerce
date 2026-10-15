package com.example.ecommerce.testdata;

import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderTestDataBuilder {

    private Long id = null;
    private BigDecimal totalAmount = BigDecimal.valueOf(100_000);
    private OrderStatus status = OrderStatus.AWAITING_PAYMENT;
    private CustomerEntity customer = null;
    private List<OrderItemEntity> orderItems = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime statusChangedAt = LocalDateTime.now();
    private LocalDateTime paymentExpiresAt = LocalDateTime.now().plusHours(24);
    private String cancelReason = null;

    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }

    public OrderTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public OrderTestDataBuilder withTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public OrderTestDataBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderTestDataBuilder withCustomer(CustomerEntity customer) {
        this.customer = customer;
        return this;
    }

    public OrderTestDataBuilder withOrderItems(List<OrderItemEntity> orderItems) {
        this.orderItems = orderItems;
        return this;
    }

    public OrderTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OrderTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public OrderTestDataBuilder withStatusChangedAt(LocalDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
        return this;
    }

    public OrderTestDataBuilder withPaymentExpiresAt(LocalDateTime paymentExpiresAt) {
        this.paymentExpiresAt = paymentExpiresAt;
        return this;
    }

    public OrderTestDataBuilder withCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
        return this;
    }

    public OrderEntity build() {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setCustomer(customer);
        order.setOrderItems(orderItems);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        order.setStatusChangedAt(statusChangedAt);
        order.setPaymentExpiresAt(paymentExpiresAt);
        order.setCancelReason(cancelReason);
        return order;
    }
}
