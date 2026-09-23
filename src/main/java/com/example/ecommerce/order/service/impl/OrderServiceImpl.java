package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.order.specification.OrderSpecification;
import com.example.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

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
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params){
        return findOrders(params, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams) {
        return findOrders(params, sortByParams, null);
    }
    // Hàm tạo Pageable từ Params
    private Pageable createPageable(Map<String, Object> params, List<String> sortByParams){
        // Đặt điều kiện mặc định cho Pageable.
        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;
        // Kiểm tra nếu params được truyền vào mới xử lý tiep
        if(params != null){
            // Lấy thông tin phân trang
            if(params.containsKey("page") && params.get("page") != null){
                try{
                    int p = Integer.parseInt(params.get("page").toString().trim());
                    if(p < 0){
                        throw new BadRequestException("Số trang phải lớn hơn hoặc bằng 0.");
                    }
                    page = p;
                } catch (NumberFormatException ex){
                    throw new BadRequestException("Tham số page phải là số nguyên hợp lệ.");
                }
            }
            // Lấy thông tin số lượng bản ghi trên mỗi trang
            if(params.containsKey("size") && params.get("size") != null){
                try{
                    int s = Integer.parseInt(params.get("size").toString().trim());
                    if(s <= 0){
                        throw new BadRequestException("Số lượng bản ghi phải lớn hơn 0.");
                    }
                    size = s;
                } catch(NumberFormatException ex){
                    throw new BadRequestException("Tham số size phải là số nguyên hợp lệ.");
                }
            }
        }
        Sort.Direction defaultDirection = resolveDirection(params);
        List<Sort.Order> orders = buildSortOrders(params, sortByParams, defaultDirection);
        if(orders.isEmpty()){
            orders.add(new Sort.Order(defaultDirection, "id")); // Thêm sort theo id để đảm bảo thứ tự nhất quán
        }
        // Tạo Pageable với các thông số đã xác định.
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private List<Sort.Order> buildSortOrders(Map<String, Object> params, List<String> sortByParams, Sort.Direction defaultDirection) {
        List<Sort.Order> orders = new ArrayList<>();
        List<String> sortByList = new ArrayList<>();
        if(sortByParams != null && !sortByParams.isEmpty()){
            for(String sortBy : sortByParams){
                sortByList.add(sortBy.trim());
            }
        }
        if(sortByList.isEmpty() && params != null){
            Object sortField = params.get("sortBy");
            if(sortField != null && !sortField.toString().isBlank()){
                sortByList.add(sortField.toString().trim());
            }
        }
        for(String sortBy : sortByList){
            String[] parts = sortBy.split(",", 2);
            String field = parts[0].trim();
            if(field.isBlank()){
                continue;
            }
            String mappedField = resolveSortBy(field);
            Sort.Direction direction = defaultDirection;
            if(parts.length == 2 && !parts[1].trim().isBlank()){
                direction = parseDirection(parts[1].trim(), "sortBy");
            }
            orders.add(new Sort.Order(direction, mappedField));
        }
        return orders;
    }

    // Hàm resolve direction từ params sang Sort.Direction
    private Sort.Direction resolveDirection(Map<String, Object> params) {
        // Mặc định kiểu sắp xếp
        Sort.Direction direction = Sort.Direction.DESC;
        // Lấy kiểu sắp xếp
        if(params != null) {
            String dirStr = null;
            if (params.containsKey("direction") && params.get("direction") != null) {
                dirStr = params.get("direction").toString().trim();
            }
            if (dirStr != null && !dirStr.isBlank()) {
                direction = parseDirection(dirStr, "direction");
            }
        }
        return direction;
    }

    // Hàm parse direction từ params sang Sort.Direction
    private Sort.Direction parseDirection(String direction, String fieldName){
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }
        if("sortBy".equals(fieldName)){
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'totalAmount' hoặc 'status'.");
        }
        throw new BadRequestException("Direction phải là 'asc' hoặc 'desc'.");
    }
    // Hàm map sortby từ params sang các trường hợp lệ
    private String resolveSortBy(String sortBy){
        return switch (sortBy){
            case "id", "createdAt", "totalAmount", "status" -> sortBy;
            default -> throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'totalAmount' hoặc 'status'.");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams) {
        // Kiểm tra customer trước để trả lỗi rõ ràng khi khách hàng không tồn tại.
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));
        return findOrders(params, sortByParams, customer.getId());
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

    private PageResponse<OrderResponse> findOrders(Map<String, Object> params, List<String> sortByParams, Long customerId) {
        OrderFilterCriteria criteria = resolveFilterCriteria(params, customerId);
        Pageable pageable = createPageable(params, sortByParams);
        Specification<OrderEntity> spec = OrderSpecification.withFilters(
                criteria.customerId(),
                criteria.status(),
                toStartOfDay(criteria.createdFrom()),
                toEndOfDay(criteria.createdTo()),
                criteria.minTotalAmount(),
                criteria.maxTotalAmount(),
                criteria.keyword()
        );
        Page<OrderEntity> page = orderRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(orderMapper::toResponse));
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999_999_999);
    }

    private OrderFilterCriteria resolveFilterCriteria(Map<String, Object> params, Long customerId) {
        OrderStatus status = parseOrderStatus(params, "status");
        LocalDate createdFrom = parseLocalDate(params, "createdFrom");
        LocalDate createdTo = parseLocalDate(params, "createdTo");
        BigDecimal minTotalAmount = parseBigDecimal(params, "minTotalAmount");
        BigDecimal maxTotalAmount = parseBigDecimal(params, "maxTotalAmount");
        String keyword = trimToNull(readParam(params, "keyword"));
        validateFilterRange(createdFrom, createdTo, minTotalAmount, maxTotalAmount);
        return new OrderFilterCriteria(customerId, status, createdFrom, createdTo, minTotalAmount, maxTotalAmount, keyword);
    }

    private void validateFilterRange(LocalDate createdFrom, LocalDate createdTo, BigDecimal minTotalAmount, BigDecimal maxTotalAmount) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException("createdFrom phải nhỏ hơn hoặc bằng createdTo.");
        }

        if (minTotalAmount != null && maxTotalAmount != null && minTotalAmount.compareTo(maxTotalAmount) > 0) {
            throw new BadRequestException("minTotalAmount phải nhỏ hơn hoặc bằng maxTotalAmount.");
        }
    }

    private OrderStatus parseOrderStatus(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }

        try {
            return OrderStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    private LocalDate parseLocalDate(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BadRequestException(key + " phải theo định dạng yyyy-MM-dd.");
        }
    }

    private BigDecimal parseBigDecimal(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số hợp lệ.");
        }
    }

    private String readParam(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record OrderFilterCriteria(
            Long customerId,
            OrderStatus status,
            LocalDate createdFrom,
            LocalDate createdTo,
            BigDecimal minTotalAmount,
            BigDecimal maxTotalAmount,
            String keyword
    ) {
    }
}
