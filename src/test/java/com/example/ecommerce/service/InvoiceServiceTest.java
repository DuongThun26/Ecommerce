package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.invoice.event.InvoiceIssuedEvent;
import com.example.ecommerce.invoice.mapper.InvoiceMapper;
import com.example.ecommerce.invoice.repository.InvoiceRepository;
import com.example.ecommerce.invoice.service.InvoiceNumberGenerator;
import com.example.ecommerce.invoice.service.InvoicePdfGenerator;
import com.example.ecommerce.invoice.service.PdfStorageService;
import com.example.ecommerce.invoice.service.impl.InvoiceServiceImpl;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;
    @Mock
    private InvoicePdfGenerator invoicePdfGenerator;
    @Mock
    private PdfStorageService pdfStorageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private CustomerEntity customer;
    private OrderEntity paidOrder;
    private InvoiceEntity invoice;

    @BeforeEach
    void setUp() {
        customer = CustomerEntity.builder()
                .id(1L)
                .name("Nguyen Van A")
                .email("vana@example.com")
                .phone("0901234567")
                .build();

        ProductEntity product = ProductEntity.builder()
                .id(10L)
                .name("iPhone 15 Pro")
                .price(BigDecimal.valueOf(25000000))
                .build();

        OrderItemEntity orderItem = OrderItemEntity.builder()
                .id(100L)
                .product(product)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25000000))
                .subTotal(BigDecimal.valueOf(50000000))
                .build();

        paidOrder = OrderEntity.builder()
                .id(50L)
                .customer(customer)
                .status(OrderStatus.PAID)
                .totalAmount(BigDecimal.valueOf(50000000))
                .orderItems(new ArrayList<>(List.of(orderItem)))
                .build();
        orderItem.setOrder(paidOrder);

        invoice = InvoiceEntity.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .order(paidOrder)
                .customer(customer)
                .status(InvoiceStatus.ISSUED)
                .issueDate(LocalDateTime.now())
                .subtotal(BigDecimal.valueOf(50000000))
                .totalAmount(BigDecimal.valueOf(50000000))
                .billingName(customer.getName())
                .billingEmail(customer.getEmail())
                .billingPhone(customer.getPhone())
                .build();
    }

    @Test
    @DisplayName("createInvoiceForOrder should create and snapshot invoice successfully")
    void createInvoiceForOrder_Success() {
        when(invoiceRepository.findByOrderId(50L)).thenReturn(Optional.empty());
        when(invoiceNumberGenerator.generateInvoiceNumber()).thenReturn("INV-20260924-000001");
        when(paymentRepository.findByOrderId(50L)).thenReturn(Optional.of(
                PaymentEntity.builder()
                        .paymentMethod(PaymentMethod.VNPAY)
                        .transactionRef("ORD50_TX123")
                        .status(PaymentStatus.SUCCESS)
                        .build()
        ));
        when(invoiceRepository.saveAndFlush(any(InvoiceEntity.class))).thenAnswer(invocation -> {
            InvoiceEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(50L)
                .customerId(1L)
                .status(InvoiceStatus.ISSUED)
                .totalAmount(BigDecimal.valueOf(50000000))
                .build();
        when(invoiceMapper.toResponse(any(InvoiceEntity.class))).thenReturn(response);

        InvoiceResponse result = invoiceService.createInvoiceForOrder(paidOrder);

        assertThat(result).isNotNull();
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-20260924-000001");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000000));

        ArgumentCaptor<InvoiceIssuedEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceIssuedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().invoiceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createInvoiceForOrder should be idempotent when invoice already exists")
    void createInvoiceForOrder_Idempotency() {
        when(invoiceRepository.findByOrderId(50L)).thenReturn(Optional.of(invoice));
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(50L)
                .build();
        when(invoiceMapper.toResponse(invoice)).thenReturn(response);

        InvoiceResponse result = invoiceService.createInvoiceForOrder(paidOrder);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(invoiceRepository, never()).saveAndFlush(any(InvoiceEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("createInvoiceForOrder should throw when order is not paid")
    void createInvoiceForOrder_UnpaidOrder_ThrowsBadRequest() {
        paidOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        when(invoiceRepository.findByOrderId(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoiceForOrder(paidOrder))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Order must be PAID");
    }

    @Test
    @DisplayName("getInvoiceById should allow customer owner to access")
    void getInvoiceById_OwnerCustomer_Success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findByUserUsername("vana")).thenReturn(Optional.of(customer));
        InvoiceResponse response = InvoiceResponse.builder().id(1L).invoiceNumber("INV-20260924-000001").build();
        when(invoiceMapper.toResponse(invoice)).thenReturn(response);

        InvoiceResponse result = invoiceService.getInvoiceById(1L, "vana", List.of("ROLE_CUSTOMER"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getInvoiceById should deny other customer accessing invoice")
    void getInvoiceById_OtherCustomer_ThrowsAccessDenied() {
        CustomerEntity otherCustomer = CustomerEntity.builder().id(2L).build();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findByUserUsername("otheruser")).thenReturn(Optional.of(otherCustomer));

        assertThatThrownBy(() -> invoiceService.getInvoiceById(1L, "otheruser", List.of("ROLE_CUSTOMER")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getInvoiceById should allow ADMIN to access any invoice")
    void getInvoiceById_Admin_Success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        InvoiceResponse response = InvoiceResponse.builder().id(1L).invoiceNumber("INV-20260924-000001").build();
        when(invoiceMapper.toResponse(invoice)).thenReturn(response);

        InvoiceResponse result = invoiceService.getInvoiceById(1L, "admin", List.of("ROLE_ADMIN"));

        assertThat(result).isNotNull();
        verify(customerRepository, never()).findByUserUsername(any());
    }

    @Test
    @DisplayName("generateAndStorePdf should generate and store PDF asynchronously")
    void generateAndStorePdf_Success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(invoicePdfGenerator.generateInvoicePdf(invoice)).thenReturn(pdfBytes);
        when(pdfStorageService.store(anyString(), eq(pdfBytes))).thenReturn("2026/09/INV-20260924-000001.pdf");

        invoiceService.generateAndStorePdf(1L);

        verify(invoicePdfGenerator).generateInvoicePdf(invoice);
        verify(pdfStorageService).store(anyString(), eq(pdfBytes));
        verify(invoiceRepository).save(invoice);
        assertThat(invoice.getPdfStorageKey()).isNotNull();
        assertThat(invoice.getPdfGeneratedAt()).isNotNull();
    }
}
