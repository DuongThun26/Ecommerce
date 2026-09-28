package com.example.ecommerce.product.entity;

import com.example.ecommerce.common.BaseEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Table(name = "products")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "product")
    private List<OrderItemEntity> orderItems;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImageEntity> images = new java.util.ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void addImage(ProductImageEntity image) {
        if (images == null) {
            images = new java.util.ArrayList<>();
        }
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImageEntity image) {
        if (images != null) {
            images.remove(image);
            image.setProduct(null);
        }
    }

    public void clearImages() {
        if (images != null) {
            for (ProductImageEntity image : images) {
                image.setProduct(null);
            }
            images.clear();
        }
    }

    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        for (ProductImageEntity img : images) {
            if (img.isPrimary()) {
                return img.getImageUrl();
            }
        }
        return images.get(0).getImageUrl();
    }

    public List<String> extractImageUrls() {
        if (images == null || images.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return images.stream().map(ProductImageEntity::getImageUrl).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }
}
