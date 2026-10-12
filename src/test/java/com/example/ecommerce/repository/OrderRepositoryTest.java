package com.example.ecommerce.repository;

import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.specification.OrderSpecification;
import com.example.ecommerce.testdata.CustomerTestDataBuilder;
import com.example.ecommerce.testdata.OrderTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("existsByCustomerId should return true when customer has orders")
    void existsByCustomerIdShouldWork() {
        CustomerEntity customer = customerRepository.save(
                CustomerTestDataBuilder.aCustomer().withEmail("o1@example.com").build()
        );
        orderRepository.save(OrderTestDataBuilder.anOrder().withCustomer(customer).withStatus(OrderStatus.PAYMENT_PENDING).build());

        assertThat(orderRepository.existsByCustomerId(customer.getId())).isTrue();
        assertThat(orderRepository.existsByCustomerId(999999L)).isFalse();
    }

    @Test
    @DisplayName("findByStatusAndCreatedAtBefore should return expired pending orders")
    void findByStatusAndCreatedAtBeforeShouldWork() {
        CustomerEntity customer = customerRepository.save(
                CustomerTestDataBuilder.aCustomer().withEmail("o2@example.com").build()
        );
        LocalDateTime now = LocalDateTime.now();

        OrderEntity oldPending = orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(customer)
                        .withStatus(OrderStatus.PAYMENT_PENDING)
                        .withCreatedAt(now.minusHours(25))
                        .withUpdatedAt(now.minusHours(25))
                        .withStatusChangedAt(now.minusHours(25))
                        .withPaymentExpiresAt(now.minusHours(1))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(customer)
                        .withStatus(OrderStatus.PAYMENT_PENDING)
                        .withCreatedAt(now.minusHours(2))
                        .withUpdatedAt(now.minusHours(2))
                        .withStatusChangedAt(now.minusHours(2))
                        .withPaymentExpiresAt(now.plusHours(22))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(customer)
                        .withStatus(OrderStatus.PAID)
                        .withCreatedAt(now.minusHours(26))
                        .withUpdatedAt(now.minusHours(26))
                        .withStatusChangedAt(now.minusHours(26))
                        .withPaymentExpiresAt(now.minusHours(2))
                        .build()
        );

        List<OrderEntity> result = orderRepository.findByStatusAndPaymentExpiresAtBefore(OrderStatus.PAYMENT_PENDING, now);

        assertThat(result).extracting(OrderEntity::getId).contains(oldPending.getId());
        assertThat(result).allMatch(o -> o.getStatus() == OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("withFilters should filter by keyword, amount and created date range")
    void withFiltersShouldApplyCombinedConditions() {
        CustomerEntity matchedCustomer = customerRepository.save(
                CustomerTestDataBuilder.aCustomer()
                        .withName("Nguyen Van A")
                        .withEmail("nguyenvana@example.com")
                        .build()
        );
        CustomerEntity otherCustomer = customerRepository.save(
                CustomerTestDataBuilder.aCustomer()
                        .withEmail("other@example.com")
                        .withName("Tran Thi B")
                        .build()
        );

        LocalDateTime now = LocalDateTime.now();
        OrderEntity expectedOrder = orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(matchedCustomer)
                        .withStatus(OrderStatus.PAYMENT_PENDING)
                        .withTotalAmount(BigDecimal.valueOf(250_000))
                        .withCreatedAt(now.minusDays(2))
                        .withUpdatedAt(now.minusDays(2))
                        .withStatusChangedAt(now.minusDays(2))
                        .withPaymentExpiresAt(now.minusDays(1))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(otherCustomer)
                        .withStatus(OrderStatus.PAYMENT_PENDING)
                        .withTotalAmount(BigDecimal.valueOf(250_000))
                        .withCreatedAt(now.minusDays(2))
                        .withUpdatedAt(now.minusDays(2))
                        .withStatusChangedAt(now.minusDays(2))
                        .withPaymentExpiresAt(now.minusDays(1))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(matchedCustomer)
                        .withStatus(OrderStatus.COMPLETED)
                        .withTotalAmount(BigDecimal.valueOf(900_000))
                        .withCreatedAt(now.minusDays(10))
                        .withUpdatedAt(now.minusDays(10))
                        .build()
        );

        List<OrderEntity> result = orderRepository.findAll(
                OrderSpecification.withFilters(
                        null,
                        OrderStatus.PAYMENT_PENDING,
                        now.minusDays(5),
                        LocalDateTime.now().plusMinutes(1),
                        BigDecimal.valueOf(100_000),
                        BigDecimal.valueOf(500_000),
                        "nguyen"
                )
        );

        assertThat(result).extracting(OrderEntity::getId).containsExactly(expectedOrder.getId());
    }
}
