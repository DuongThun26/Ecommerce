package com.example.ecommerce.order.entity;

import com.example.ecommerce.common.BaseEntity;
import com.example.ecommerce.product.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", uniqueConstraints = {
        @UniqueConstraint(name = "UK_ORDER_ITEM_SUB_ORDER_PRODUCT", columnNames = {"sub_order_id", "product_id"})
})
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id")
    private SubOrderEntity subOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

}
