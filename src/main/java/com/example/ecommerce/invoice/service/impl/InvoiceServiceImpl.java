package com.example.ecommerce.invoice.service.impl;

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
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.invoice.service.PdfStorageService;
import com.example.ecommerce.invoice.specification.InvoiceSpecification;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final InvoicePdfGenerator invoicePdfGenerator;
    private final PdfStorageService pdfStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId, String username, List<String> roles) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice with id " + invoiceId + " not found."));
        validateOwnership(invoice, username, roles);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId, String username, List<String> roles) {
        InvoiceEntity invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for order id " + orderId + " not found."));
        validateOwnership(invoice, username, roles);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByInvoiceNumber(String invoiceNumber, String username, List<String> roles) {
        InvoiceEntity invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice with number " + invoiceNumber + " not found."));
        validateOwnership(invoice, username, roles);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getInvoices(Map<String, Object> params, List<String> sortByParams) {
        return findInvoices(params, sortByParams, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getInvoicesByCustomerId(
            Long customerId,
            Map<String, Object> params,
            List<String> sortByParams,
            String username,
            List<String> roles
    ) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));

        if (!isAdminOrPo(roles)) {
            CustomerEntity authCustomer = getCustomerByUsername(username);
            if (!authCustomer.getId().equals(customerId)) {
                throw new AccessDeniedException("Access denied: You can only access your own invoices.");
            }
        }

        return findInvoices(params, sortByParams, customer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceResponse createInvoiceForOrderId(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return createInvoiceForOrder(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceResponse createInvoiceForOrder(OrderEntity order) {
        if (order == null) {
            throw new BadRequestException("Order cannot be null when creating an invoice.");
        }

        // Idempotency check: Return existing invoice if already created
        Optional<InvoiceEntity> existingInvoice = invoiceRepository.findByOrderId(order.getId());
        if (existingInvoice.isPresent()) {
            log.info("Invoice already exists for order #{}", order.getId());
            return invoiceMapper.toResponse(existingInvoice.get());
        }

        // Validate that order has been paid
        if (!isOrderPaidOrCompleted(order.getStatus())) {
            throw new BadRequestException("Cannot create invoice for order in status " + order.getStatus() + ". Order must be PAID or COMPLETED.");
        }

        CustomerEntity customer = order.getCustomer();
        if (customer == null) {
            throw new BadRequestException("Order #" + order.getId() + " is missing customer details.");
        }

        // Snapshot payment method and transaction info
        PaymentMethod paymentMethod = PaymentMethod.VNPAY;
        String paymentTxnRef = null;
        Optional<PaymentEntity> paymentOpt = paymentRepository.findByOrderId(order.getId());
        if (paymentOpt.isPresent()) {
            PaymentEntity payment = paymentOpt.get();
            paymentMethod = payment.getPaymentMethod();
            paymentTxnRef = payment.getTransactionRef();
        }

        LocalDateTime now = LocalDateTime.now();
        String invoiceNumber = invoiceNumberGenerator.generateInvoiceNumber();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceItemEntity> items = new ArrayList<>();

        if (order.getOrderItems() != null) {
            for (OrderItemEntity orderItem : order.getOrderItems()) {
                BigDecimal itemSubtotal = orderItem.getSubTotal();
                if (itemSubtotal == null) {
                    itemSubtotal = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
                }
                subtotal = subtotal.add(itemSubtotal);

                InvoiceItemEntity invoiceItem = InvoiceItemEntity.builder()
                        .productId(orderItem.getProduct().getId())
                        .productName(orderItem.getProduct().getName()) // SNAPSHOT product name
                        .quantity(orderItem.getQuantity())
                        .unitPrice(orderItem.getUnitPrice())           // SNAPSHOT unit price
                        .subtotal(itemSubtotal)
                        .build();
                items.add(invoiceItem);
            }
        }

        InvoiceEntity invoice = InvoiceEntity.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .customer(customer)
                .status(InvoiceStatus.ISSUED)
                .issueDate(now)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : subtotal)
                .billingName(customer.getName())
                .billingEmail(customer.getEmail())
                .billingPhone(customer.getPhone())
                .billingAddress(null)
                .paymentMethod(paymentMethod)
                .paymentTransactionRef(paymentTxnRef)
                .build();

        for (InvoiceItemEntity item : items) {
            invoice.addItem(item);
        }

        try {
            InvoiceEntity savedInvoice = invoiceRepository.saveAndFlush(invoice);
            log.info("Created and issued invoice #{} for order #{}", savedInvoice.getInvoiceNumber(), order.getId());

            // Fire event for asynchronous PDF generation
            eventPublisher.publishEvent(new InvoiceIssuedEvent(savedInvoice.getId()));

            return invoiceMapper.toResponse(savedInvoice);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent invoice creation detected for order #{}, returning existing invoice.", order.getId());
            return invoiceRepository.findByOrderId(order.getId())
                    .map(invoiceMapper::toResponse)
                    .orElseThrow(() -> new BadRequestException("Could not create or retrieve invoice for order #" + order.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getInvoicePdfContent(Long invoiceId, String username, List<String> roles) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice with id " + invoiceId + " not found."));
        validateOwnership(invoice, username, roles);

        String storageKey = invoice.getPdfStorageKey();
        if (storageKey != null && pdfStorageService.exists(storageKey)) {
            return pdfStorageService.load(storageKey);
        }

        // Generate on demand if not present
        byte[] pdfBytes = invoicePdfGenerator.generateInvoicePdf(invoice);
        String generatedKey = buildStorageKey(invoice);
        pdfStorageService.store(generatedKey, pdfBytes);
        return pdfBytes;
    }

    @Override
    @Transactional
    public Resource getInvoicePdfResource(Long invoiceId, String username, List<String> roles) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice with id " + invoiceId + " not found."));
        validateOwnership(invoice, username, roles);

        String storageKey = invoice.getPdfStorageKey();
        if (storageKey != null && pdfStorageService.exists(storageKey)) {
            return pdfStorageService.loadAsResource(storageKey);
        }

        // Generate and store synchronously
        byte[] pdfBytes = invoicePdfGenerator.generateInvoicePdf(invoice);
        String generatedKey = buildStorageKey(invoice);
        pdfStorageService.store(generatedKey, pdfBytes);

        invoice.setPdfStorageKey(generatedKey);
        invoice.setPdfGeneratedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        return new ByteArrayResource(pdfBytes);
    }

    @Override
    @Transactional
    public void generateAndStorePdf(Long invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Invoice with id {} not found for asynchronous PDF generation.", invoiceId);
            return;
        }

        if (invoice.getPdfStorageKey() != null && pdfStorageService.exists(invoice.getPdfStorageKey())) {
            log.info("PDF for invoice #{} already exists at {}.", invoice.getInvoiceNumber(), invoice.getPdfStorageKey());
            return;
        }

        try {
            byte[] pdfBytes = invoicePdfGenerator.generateInvoicePdf(invoice);
            String storageKey = buildStorageKey(invoice);
            pdfStorageService.store(storageKey, pdfBytes);

            invoice.setPdfStorageKey(storageKey);
            invoice.setPdfGeneratedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Asynchronously generated and stored PDF for invoice #{} with key {}", invoice.getInvoiceNumber(), storageKey);
        } catch (Exception ex) {
            log.error("Failed to generate and store PDF for invoice #{}", invoice.getInvoiceNumber(), ex);
        }
    }

    private String buildStorageKey(InvoiceEntity invoice) {
        LocalDateTime date = invoice.getIssueDate() != null ? invoice.getIssueDate() : LocalDateTime.now();
        return String.format("%d/%02d/%s.pdf", date.getYear(), date.getMonthValue(), invoice.getInvoiceNumber());
    }

    private boolean isOrderPaidOrCompleted(OrderStatus status) {
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED;
    }

    private void validateOwnership(InvoiceEntity invoice, String username, List<String> roles) {
        if (isAdminOrPo(roles)) {
            return;
        }
        CustomerEntity authCustomer = getCustomerByUsername(username);
        if (!invoice.getCustomer().getId().equals(authCustomer.getId())) {
            throw new AccessDeniedException("Access denied: You do not have permission to view this invoice.");
        }
    }

    private boolean isAdminOrPo(List<String> roles) {
        if (roles == null) {
            return false;
        }
        return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_PRODUCT_OWNER") || roles.contains("ADMIN") || roles.contains("PRODUCT_OWNER");
    }

    private CustomerEntity getCustomerByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Unauthenticated user.");
        }
        return customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Customer profile not found for user: " + username));
    }

    private PageResponse<InvoiceResponse> findInvoices(Map<String, Object> params, List<String> sortByParams, Long customerIdFilter) {
        Pageable pageable = createPageable(params, sortByParams);
        InvoiceStatus status = resolveStatus(params);
        LocalDateTime issueDateFrom = resolveDateTime(params, "issueDateFrom");
        LocalDateTime issueDateTo = resolveDateTime(params, "issueDateTo");
        String keyword = resolveString(params, "keyword");
        Long customerId = customerIdFilter != null ? customerIdFilter : resolveLong(params, "customerId");

        Specification<InvoiceEntity> spec = InvoiceSpecification.withFilters(customerId, status, issueDateFrom, issueDateTo, keyword);
        Page<InvoiceEntity> page = invoiceRepository.findAll(spec, pageable);

        List<InvoiceResponse> content = page.getContent().stream()
                .map(invoiceMapper::toResponse)
                .toList();

        return PageResponse.<InvoiceResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    private Pageable createPageable(Map<String, Object> params, List<String> sortByParams) {
        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;

        if (params != null) {
            if (params.containsKey("page") && params.get("page") != null) {
                try {
                    int parsedPage = Integer.parseInt(params.get("page").toString().trim());
                    if (parsedPage < 0) {
                        throw new BadRequestException("Số trang phải lớn hơn hoặc bằng 0.");
                    }
                    page = parsedPage;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số page phải là số nguyên hợp lệ.");
                }
            }
            if (params.containsKey("size") && params.get("size") != null) {
                try {
                    int parsedSize = Integer.parseInt(params.get("size").toString().trim());
                    if (parsedSize <= 0) {
                        throw new BadRequestException("Số lượng bản ghi phải lớn hơn 0.");
                    }
                    size = parsedSize;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số size phải là số nguyên hợp lệ.");
                }
            }
        }

        Sort.Direction defaultDirection = Sort.Direction.DESC;
        List<Sort.Order> orders = new ArrayList<>();
        if (sortByParams != null && !sortByParams.isEmpty()) {
            for (String sort : sortByParams) {
                String[] parts = sort.split(":");
                String property = parts[0].trim();
                Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(dir, property));
            }
        }
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(defaultDirection, "createdAt"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private InvoiceStatus resolveStatus(Map<String, Object> params) {
        if (params == null || !params.containsKey("status") || params.get("status") == null) {
            return null;
        }
        String statusStr = params.get("status").toString().trim();
        if (statusStr.isEmpty()) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trạng thái hóa đơn không hợp lệ: " + statusStr);
        }
    }

    private LocalDateTime resolveDateTime(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return null;
        }
        String rawValue = params.get(key).toString().trim();
        if (rawValue.isEmpty()) {
            return null;
        }
        try {
            if (rawValue.contains("T")) {
                return LocalDateTime.parse(rawValue);
            }
            LocalDate localDate = LocalDate.parse(rawValue, ISO_LOCAL_DATE);
            return key.endsWith("To") ? localDate.atTime(23, 59, 59) : localDate.atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Định dạng ngày không hợp lệ cho tham số " + key + ": " + rawValue);
        }
    }

    private String resolveString(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return null;
        }
        String val = params.get(key).toString().trim();
        return val.isEmpty() ? null : val;
    }

    private Long resolveLong(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return null;
        }
        String val = params.get(key).toString().trim();
        if (val.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Tham số " + key + " phải là số nguyên hợp lệ.");
        }
    }
}
