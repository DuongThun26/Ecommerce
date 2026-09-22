package com.example.ecommerce.repository;

import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.testdata.CustomerTestDataBuilder;
import com.example.ecommerce.testdata.OrderTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

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
        orderRepository.save(OrderTestDataBuilder.anOrder().withCustomer(customer).withStatus(OrderStatus.PENDING_PAYMENT).build());

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
                        .withStatus(OrderStatus.PENDING_PAYMENT)
                        .withCreatedAt(now.minusHours(25))
                        .withUpdatedAt(now.minusHours(25))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(customer)
                        .withStatus(OrderStatus.PENDING_PAYMENT)
                        .withCreatedAt(now.minusHours(2))
                        .withUpdatedAt(now.minusHours(2))
                        .build()
        );
        orderRepository.save(
                OrderTestDataBuilder.anOrder()
                        .withCustomer(customer)
                        .withStatus(OrderStatus.CONFIRMED)
                        .withCreatedAt(now.minusHours(26))
                        .withUpdatedAt(now.minusHours(26))
                        .build()
        );

        List<OrderEntity> result = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, now.minusHours(24));

        assertThat(result).extracting(OrderEntity::getId).contains(oldPending.getId());
        assertThat(result).allMatch(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT);
    }
}
