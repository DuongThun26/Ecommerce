package com.example.ecommerce.vnpay.service.impl;

import com.example.ecommerce.entity.OrderEntity;
import com.example.ecommerce.entity.PaymentEntity;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.enums.PaymentMethod;
import com.example.ecommerce.enums.PaymentStatus;
import com.example.ecommerce.exception.OrderProcessingException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.vnpay.config.VnPayConfig;
import com.example.ecommerce.vnpay.repository.PaymentRepository;
import com.example.ecommerce.vnpay.service.VnPayService;
import com.example.ecommerce.vnpay.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {
    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String DEFAULT_RETURN_PATH = "/payments/vnpay/return";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createPaymentUrl(Long orderId, HttpServletRequest request) {
        OrderEntity order;
        try {
            order = orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        } catch (PessimisticLockingFailureException ex) {
            throw new OrderProcessingException("Order is being processed for payment. Please retry.");
        }

        validateOrder(order);
        PaymentEntity payment = getOrCreatePendingPayment(order);
        String transactionRef = payment.getTransactionRef();
        if (transactionRef == null || transactionRef.isBlank()) {
            throw new OrderProcessingException("Payment transaction reference is missing for order " + orderId + ".");
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amount = toVnPayAmount(order.getTotalAmount(), orderId);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", resolveReturnUrl(request));
        params.put("vnp_IpAddr", getClientIp(request));

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_DATE_FORMAT));

        String hashData = VnPayUtil.buildHashData(params);

        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        String queryString = VnPayUtil.buildQueryString(params);

        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handleReturn(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        String secureHash = params.get("vnp_SecureHash");
        boolean valid = VnPayUtil.verifySecureHash(params, secureHash, vnPayConfig.getHashSecret());

        if (!valid) {
            result.put("status", "INVALID_SIGNATURE");
            result.put("message", "Chu ky khong hop le");
            return result;
        }

        String transactionRef = params.get("vnp_TxnRef");
        PaymentEntity payment = paymentRepository.findByTransactionRef(transactionRef).orElse(null);

        if (payment == null) {
            result.put("status", "ORDER_NOT_FOUND");
            result.put("message", "Khong tim thay giao dich");
            return result;
        }

        String vnpAmount = params.get("vnp_Amount");

        if (!isValidAmount(payment, vnpAmount)) {
            result.put("status", "INVALID_AMOUNT");
            result.put("message", "So tien khong hop le");
            return result;
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            applyPaymentResult(payment, params);
            paymentRepository.save(payment);
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            result.put("status", "SUCCESS");
            result.put("message", "Giao dich thanh cong");
        } else {
            result.put("status", "FAILED");
            result.put("message", "Giao dich khong thanh cong");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleIpn(Map<String, String> params) {

        String secureHash = params.get("vnp_SecureHash");

        // 1. Verify checksum
        boolean valid = VnPayUtil.verifySecureHash(params, secureHash, vnPayConfig.getHashSecret());

        if (!valid) {
            return """
                    {"RspCode":"97","Message":"Invalid Checksum"}
                    """;
        }

        // 2. Lấy transaction reference
        String transactionRef = params.get("vnp_TxnRef");
        PaymentEntity payment = paymentRepository.findByTransactionRef(transactionRef).orElse(null);

        // 3. Không tìm thấy Payment
        if (payment == null) {
            return """
                    {"RspCode":"01","Message":"Order not Found"}
                    """;
        }

        // 4. Kiểm tra amount
        String vnpAmount = params.get("vnp_Amount");
        if (!isValidAmount(payment, vnpAmount)) {
            return """
                    {"RspCode":"04","Message":"Invalid Amount"}
                    """;
        }

        // 5. Kiểm tra trạng thái
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return """
                    {"RspCode":"02","Message":"Order already confirmed"}
                    """;
        }
        applyPaymentResult(payment, params);
        paymentRepository.save(payment);
        return """
                {"RspCode":"00","Message":"Confirm Success"}
                """;
    }

    private boolean isValidAmount(PaymentEntity payment, String vnpAmount) {
        if (vnpAmount == null) {
            return false;
        }
        try {
            long expectedAmount = payment.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            long actualAmount = Long.parseLong(vnpAmount);
            return expectedAmount == actualAmount;
        } catch (ArithmeticException | NumberFormatException e) {
            return false;
        }
    }

    private void applyPaymentResult(PaymentEntity payment, Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        payment.setTransactionNo(params.get("vnp_TransactionNo"));
        payment.setResponseCode(responseCode);
        payment.setTransactionStatus(transactionStatus);
        payment.setBankCode(params.get("vnp_BankCode"));
        boolean success = "00".equals(responseCode) && (transactionStatus == null || transactionStatus.isBlank() || "00".equals(transactionStatus));
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            updateOrderStatusForSuccess(payment.getOrder());
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
    }

    private void updateOrderStatusForSuccess(OrderEntity order) {
        if (order == null) {
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
    }

    private void validateOrder(OrderEntity order) {
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderProcessingException("Order amount must be greater than zero.");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderProcessingException("Order is not in a valid state for payment: " + order.getStatus() + ".");
        }
    }

    private PaymentEntity getOrCreatePendingPayment(OrderEntity order) {
        PaymentEntity payment = paymentRepository.findByOrderId(order.getId()).orElseGet(() ->
                PaymentEntity.builder()
                        .order(order)
                        .totalAmount(order.getTotalAmount())
                        .build()
        );

        PaymentStatus currentStatus = payment.getStatus();
        if (currentStatus == PaymentStatus.SUCCESS) {
            throw new OrderProcessingException("Order " + order.getId() + " was already paid successfully.");
        }

        boolean needsNewTransactionRef = currentStatus != PaymentStatus.PENDING
                || payment.getTransactionRef() == null
                || payment.getTransactionRef().isBlank();

        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTotalAmount(order.getTotalAmount());

        if (needsNewTransactionRef) {
            payment.setTransactionRef(generateTransactionRef(order.getId()));
        }

        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            PaymentEntity existingPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow(
                    () -> new OrderProcessingException("Unable to initialize payment for order " + order.getId() + ".")
            );
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new OrderProcessingException("Order " + order.getId() + " was already paid successfully.");
            }
            return existingPayment;
        }
    }

    private long toVnPayAmount(BigDecimal totalAmount, Long orderId) {
        try {
            return totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
        } catch (ArithmeticException ex) {
            throw new OrderProcessingException("Invalid amount for VNPay request on order " + orderId + ".");
        }
    }

    private String generateTransactionRef(Long orderId) {
        return "ORD" + orderId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String resolveReturnUrl(HttpServletRequest request) {
        String configuredReturnUrl = vnPayConfig.getReturnUrl();
        if (configuredReturnUrl == null || configuredReturnUrl.isBlank()) {
            return buildServerBaseUrl(request) + request.getContextPath() + DEFAULT_RETURN_PATH;
        }
        String trimmedReturnUrl = configuredReturnUrl.trim();
        if (trimmedReturnUrl.startsWith("http://") || trimmedReturnUrl.startsWith("https://")) {
            return trimmedReturnUrl;
        }
        String normalizedPath = trimmedReturnUrl.startsWith("/") ? trimmedReturnUrl : "/" + trimmedReturnUrl;
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && !normalizedPath.startsWith(contextPath + "/") && !normalizedPath.equals(contextPath)) {
            normalizedPath = contextPath + normalizedPath;
        }
        return buildServerBaseUrl(request) + normalizedPath;
    }

    private String buildServerBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80) || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
        if (isDefaultPort) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}
