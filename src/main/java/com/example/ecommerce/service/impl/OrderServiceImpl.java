package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.OrderRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.enums.OrderStatus;
import com.example.ecommerce.enums.PaymentMethod;
import com.example.ecommerce.enums.PaymentStatus;
import com.example.ecommerce.exception.OrderProcessingException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.CustomerRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.service.OrderItemService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.specification.OrderSpecification;
import com.example.ecommerce.vnpay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        // Lấy đơn hàng theo ID và ném lỗi nếu không tồn tại.
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(OrderStatus status, Pageable pageable) {
        // Trả về danh sách đơn hàng có thể lọc theo trạng thái và phân trang.
        Page<OrderEntity> page = orderRepository.findAll(OrderSpecification.withFilters(null, status), pageable);
        return PageResponse.of(page.map(orderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, OrderStatus status, Pageable pageable) {
        // Kiểm tra customer trước để trả lỗi rõ ràng khi khách hàng không tồn tại.
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));
        Page<OrderEntity> page = orderRepository.findAll(OrderSpecification.withFilters(customer.getId(), status), pageable);
        return PageResponse.of(page.map(orderMapper::toResponse));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        // Tạo order trong một transaction để bảo đảm mọi bước cùng thành công hoặc cùng rollback.
        Long resolvedCustomerId = request.getCustomerId();
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(resolvedCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + resolvedCustomerId + " not found."));

        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProducts(requestedQuantities);
        List<OrderItemEntity> orderItems = orderItemService.createOrderItems(order, productById, requestedQuantities);
        orderItemService.decrementStock(productById, requestedQuantities);
        order.setOrderItems(orderItems);
        order.setTotalAmount(orderItemService.calculateTotalAmount(orderItems));

        OrderEntity savedOrder = orderRepository.save(order);
        PaymentEntity payment = PaymentEntity.builder()
                .totalAmount(savedOrder.getTotalAmount())
                .order(savedOrder)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .transactionRef(generateTransactionRef(savedOrder.getId()))
                .build();
        paymentRepository.save(payment);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        // Chỉ cho phép chuyển trạng thái theo luồng nghiệp vụ hợp lệ.
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        validateStatusTransition(order.getStatus(), status);
        order.setStatus(status);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public void cancelExpiredOrders() {
        // Lấy thời điểm quá hạn thanh toán của đơn hàng PENDING.
        LocalDateTime cutOffTime = LocalDateTime.now().minusHours(24);

        // Lấy danh sách các đơn PENDING quá hạn và tự động hủy.
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutOffTime);
        for (OrderEntity order : expiredOrders) {
            // Hoàn lại tồn kho cho các sản phẩm trong đơn bị hủy.
            for (OrderItemEntity orderItem : order.getOrderItems()) {
                ProductEntity product = orderItem.getProduct();
                product.setStock(product.getStock() + orderItem.getQuantity());
            }
            order.setStatus(OrderStatus.CANCELLED);
        }
        log.info("Cancelled {} expired orders.", expiredOrders.size());
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        // Không cho phép cập nhật cùng trạng thái để tránh request vô nghĩa.
        if (currentStatus == nextStatus) {
            throw new OrderProcessingException("Order status is already " + currentStatus + ".");
        }
        // Quy tắc chuyển trạng thái hợp lệ của đơn hàng.
        boolean valid =
                (currentStatus == OrderStatus.PENDING_PAYMENT && (nextStatus == OrderStatus.CONFIRMED || nextStatus == OrderStatus.CANCELLED))
                        || (currentStatus == OrderStatus.CONFIRMED && nextStatus == OrderStatus.COMPLETED);
        if (!valid) {
            throw new OrderProcessingException(
                    "Invalid order status transition from " + currentStatus + " to " + nextStatus + "."
            );
        }
    }

    private String generateTransactionRef(Long orderId) {
        return "ORD" + orderId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
