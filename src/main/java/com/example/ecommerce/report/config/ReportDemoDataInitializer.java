package com.example.ecommerce.report.config;

import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ReportDemoDataInitializer implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and initializing report demo seed data...");

        // Ensure customer 1 exists (or reuse existing customer)
        CustomerEntity c1 = customerRepository.findById(1L).orElseGet(() -> {
            if (!customerRepository.existsByEmail("an.nguyen@example.com")) {
                return customerRepository.save(CustomerEntity.builder()
                        .name("Nguyễn Văn An")
                        .email("an.nguyen@example.com")
                        .phone("0901234567")
                        .status(CustomerStatus.ACTIVE)
                        .deleted(false)
                        .build());
            }
            return customerRepository.findAll().stream().findFirst().orElse(null);
        });

        // Ensure customer 2 exists
        CustomerEntity c2 = customerRepository.findById(2L).orElseGet(() -> {
            if (!customerRepository.existsByEmail("binh.tran@example.com")) {
                return customerRepository.save(CustomerEntity.builder()
                        .name("Trần Thị Bình")
                        .email("binh.tran@example.com")
                        .phone("0907654321")
                        .status(CustomerStatus.ACTIVE)
                        .deleted(false)
                        .build());
            }
            return customerRepository.findAll().stream()
                    .filter(c -> !c.getId().equals(c1 != null ? c1.getId() : null))
                    .findFirst()
                    .orElse(c1);
        });

        if (c1 == null) {
            return;
        }

        // Seed demo orders: ORD-201 to ORD-205 if not already present
        seedOrderIfNotExists("ORD-201", c1, new BigDecimal("45980000.00"), OrderStatus.COMPLETED,
                LocalDateTime.of(2026, 10, 1, 10, 0));

        seedOrderIfNotExists("ORD-202", c2 != null ? c2 : c1, new BigDecimal("190000.00"), OrderStatus.COMPLETED,
                LocalDateTime.of(2026, 10, 2, 6, 30));

        seedOrderIfNotExists("ORD-203", c1, new BigDecimal("22990000.00"), OrderStatus.CONFIRMED,
                LocalDateTime.of(2026, 10, 3, 8, 15));

        seedOrderIfNotExists("ORD-204", c2 != null ? c2 : c1, new BigDecimal("5990000.00"), OrderStatus.CANCELLED,
                LocalDateTime.of(2026, 10, 3, 9, 0));

        seedOrderIfNotExists("ORD-205", c2 != null ? c2 : c1, new BigDecimal("380000.00"), OrderStatus.PAYMENT_PENDING,
                null);
    }

    private void seedOrderIfNotExists(String orderCode, CustomerEntity customer, BigDecimal totalAmount,
                                      OrderStatus status, LocalDateTime paidAt) {
        boolean exists = orderRepository.findAll().stream()
                .anyMatch(o -> orderCode.equals(o.getOrderCode()));
        if (!exists) {
            orderRepository.save(OrderEntity.builder()
                    .orderCode(orderCode)
                    .customer(customer)
                    .totalAmount(totalAmount)
                    .status(status)
                    .paidAt(paidAt)
                    .statusChangedAt(paidAt != null ? paidAt : LocalDateTime.of(2026, 10, 4, 10, 0))
                    .build());
            log.info("Seeded report demo order: {}", orderCode);
        }
    }
}
