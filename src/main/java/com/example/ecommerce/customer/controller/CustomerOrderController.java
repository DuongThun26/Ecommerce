package com.example.ecommerce.customer.controller;

import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
@Tag(
        name = "Customer Orders",
        description = "APIs cho danh sách đơn hàng theo khách hàng."
)
public class CustomerOrderController {

    private final OrderService orderService;

    // Lấy toàn bộ đơn hàng của một khách hàng, hỗ trợ lọc trạng thái và phân trang.
    @Operation(
            summary = "Lấy danh sách đơn hàng theo khách hàng",
            description = "API cho phép lấy danh sách đơn hàng của một khách hàng, hỗ trợ lọc theo status và phân trang."
    )
    @GetMapping("/{customerId}/orders")
    public PageResponse<OrderResponse> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.getOrdersByCustomerId(customerId, status, pageable);
    }
}
