package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.mapper.SubOrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.OrderQueryService;
import com.example.ecommerce.order.specification.OrderSpecification;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueryServiceImpl implements OrderQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final CustomerRepository customerRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderMapper orderMapper;
    private final SubOrderMapper subOrderMapper;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public SubOrderResponse getSubOrderById(Long subOrderId) {
        SubOrderEntity subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));
        return subOrderMapper.toResponse(subOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams) {
        return findOrders(params, sortByParams, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));
        return findOrders(params, sortByParams, customer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .fromStatus(history.getFromStatus())
                        .toStatus(history.getToStatus())
                        .actorType(history.getActorType())
                        .actorIdentifier(history.getActorIdentifier())
                        .reason(history.getReason())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
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

        Sort.Direction defaultDirection = resolveDirection(params);
        List<Sort.Order> orders = buildSortOrders(params, sortByParams, defaultDirection);
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(defaultDirection, "id"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private List<Sort.Order> buildSortOrders(Map<String, Object> params, List<String> sortByParams, Sort.Direction defaultDirection) {
        List<Sort.Order> orders = new ArrayList<>();
        List<String> rawTokens = new ArrayList<>();

        if (sortByParams != null && !sortByParams.isEmpty()) {
            for (String sortBy : sortByParams) {
                if (sortBy != null && !sortBy.isBlank()) {
                    rawTokens.add(sortBy.trim());
                }
            }
        }

        if (rawTokens.isEmpty() && params != null) {
            Object sortField = params.get("sortBy");
            if (sortField != null && !sortField.toString().isBlank()) {
                rawTokens.add(sortField.toString().trim());
            }
        }

        int i = 0;
        while (i < rawTokens.size()) {
            String token = rawTokens.get(i);
            if (token.isBlank()) {
                i++;
                continue;
            }

            if (token.contains(",") || token.contains(":")) {
                String delimiter = token.contains(":") ? ":" : ",";
                String[] parts = token.split(delimiter, 2);
                String field = parts[0].trim();
                if (!field.isBlank()) {
                    String mappedField = resolveSortBy(field);
                    Sort.Direction direction = defaultDirection;
                    if (parts.length == 2 && !parts[1].trim().isBlank()) {
                        direction = parseDirection(parts[1].trim(), "sortBy");
                    }
                    orders.add(new Sort.Order(direction, mappedField));
                }
                i++;
            } else {
                String mappedField = resolveSortBy(token);
                Sort.Direction direction = defaultDirection;
                if (i + 1 < rawTokens.size()) {
                    String nextToken = rawTokens.get(i + 1).trim();
                    if ("asc".equalsIgnoreCase(nextToken) || "desc".equalsIgnoreCase(nextToken)) {
                        direction = parseDirection(nextToken, "sortBy");
                        i++;
                    }
                }
                orders.add(new Sort.Order(direction, mappedField));
                i++;
            }
        }
        return orders;
    }

    private Sort.Direction resolveDirection(Map<String, Object> params) {
        Sort.Direction direction = Sort.Direction.DESC;
        if (params != null) {
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

    private Sort.Direction parseDirection(String direction, String fieldName) {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }
        if ("sortBy".equals(fieldName)) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        }
        throw new BadRequestException("Direction phải là 'asc' hoặc 'desc'.");
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "id" -> "id";
            case "createdat", "created_at" -> "createdAt";
            case "status" -> "status";
            case "totalamount", "total_amount" -> "totalAmount";
            default -> throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        };
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999_999_999);
    }

    private OrderFilterCriteria resolveFilterCriteria(Map<String, Object> params, Long forcedCustomerId) {
        Long customerId = forcedCustomerId != null ? forcedCustomerId : parseLong(params, "customerId");
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

    private Long parseLong(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số nguyên hợp lệ.");
        }
    }

    private BigDecimal parseBigDecimal(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException(key + " phải lớn hơn hoặc bằng 0.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số hợp lệ.");
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
