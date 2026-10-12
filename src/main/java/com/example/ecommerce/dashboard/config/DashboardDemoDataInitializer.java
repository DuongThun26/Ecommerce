package com.example.ecommerce.dashboard.config;

import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("!test")
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DashboardDemoDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and initializing dashboard demo seed products and orders...");

        // 1. AirPods Pro 2 (ACTIVE, stock = 0) -> Should appear in lowStockProducts
        ProductEntity airPods = productRepository.findAll().stream()
                .filter(p -> "AirPods Pro 2".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElseGet(() -> productRepository.save(ProductEntity.builder()
                        .name("AirPods Pro 2")
                        .price(new BigDecimal("5490000.00"))
                        .stock(0)
                        .status(ProductStatus.ACTIVE)
                        .deleted(false)
                        .version(0L)
                        .build()));

        // 2. iPhone 13 (INACTIVE, stock = 5) -> Must NOT appear in lowStockProducts because status is INACTIVE
        ProductEntity iphone13 = productRepository.findAll().stream()
                .filter(p -> "iPhone 13".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElseGet(() -> productRepository.save(ProductEntity.builder()
                        .name("iPhone 13")
                        .price(new BigDecimal("13990000.00"))
                        .stock(5)
                        .status(ProductStatus.INACTIVE)
                        .deleted(false)
                        .version(0L)
                        .build()));

        // 3. iPhone 16 128GB (ACTIVE, stock = 50) -> Top selling product
        ProductEntity iphone16 = productRepository.findAll().stream()
                .filter(p -> "iPhone 16 128GB".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElseGet(() -> productRepository.save(ProductEntity.builder()
                        .name("iPhone 16 128GB")
                        .price(new BigDecimal("22990000.00"))
                        .stock(50)
                        .status(ProductStatus.ACTIVE)
                        .deleted(false)
                        .version(0L)
                        .build()));

        // 4. Attach order item to an existing or new order for top5Products demo
        OrderEntity order = orderRepository.findAll().stream()
                .filter(o -> o.getPaidAt() != null && o.getStatus() == OrderStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        if (order != null && orderItemRepository.findByOrderId(order.getId()).isEmpty()) {
            orderItemRepository.save(OrderItemEntity.builder()
                    .order(order)
                    .product(iphone16)
                    .quantity(2)
                    .unitPrice(new BigDecimal("22990000.00"))
                    .subTotal(new BigDecimal("45980000.00"))
                    .build());
            log.info("Linked demo order item for iPhone 16 128GB to order {}", order.getId());
        }
    }
}
