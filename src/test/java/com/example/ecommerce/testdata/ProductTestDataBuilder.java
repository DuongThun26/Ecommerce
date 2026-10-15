package com.example.ecommerce.testdata;

import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductTestDataBuilder {

    private Long id = null;
    private String name = "Test Product";
    private BigDecimal price = BigDecimal.valueOf(99_000);
    private Integer stock = 10;
    private ProductStatus status = ProductStatus.ACTIVE;
    private boolean deleted = false;
    private Long version = 0L;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static ProductTestDataBuilder aProduct() {
        return new ProductTestDataBuilder();
    }

    public ProductTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ProductTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductTestDataBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductTestDataBuilder withStock(Integer stock) {
        this.stock = stock;
        return this;
    }

    public ProductTestDataBuilder withStatus(ProductStatus status) {
        this.status = status;
        return this;
    }

    public ProductTestDataBuilder withDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public ProductTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ProductTestDataBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ProductEntity build() {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        product.setDeleted(deleted);
        product.setVersion(version);
        product.setCreatedAt(createdAt);
        product.setUpdatedAt(updatedAt);
        return product;
    }
}
