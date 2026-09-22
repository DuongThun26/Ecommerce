package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderStatusRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Tag(
        name = "Order",
        description = "APIs cho việc quản lý đơn hàng."
)
public class OrderController {

    private final OrderService orderService;

    // Tạo đơn hàng mới từ danh sách sản phẩm và số lượng người dùng gửi lên.
    @Operation(
            summary = "Tạo đơn hàng mới",
            description = "API cho phép tạo đơn hàng, kiểm tra customer, product, stock và tạo order item trong transaction."
    )
    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    // Lấy danh sách đơn hàng, có thể lọc theo trạng thái và phân trang.
    @Operation(
            summary = "Lấy danh sách đơn hàng",
            description = "API cho phép lấy danh sách đơn hàng, hỗ trợ lọc theo status và phân trang."
    )
    @GetMapping
    public PageResponse<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.getOrders(status, pageable);
    }

    // Lấy chi tiết một đơn hàng theo ID.
    @Operation(
            summary = "Lấy thông tin đơn hàng theo ID",
            description = "API cho phép lấy chi tiết đơn hàng theo ID."
    )
    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    // Cập nhật trạng thái đơn hàng theo luồng chuyển trạng thái hợp lệ.
    @Operation(
            summary = "Cập nhật trạng thái đơn hàng",
            description = "API cho phép cập nhật trạng thái đơn hàng với transition hợp lệ."
    )
    @PatchMapping("/{id}/status")
    public OrderResponse updateOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateOrderStatus(id, request.getStatus());
    }
}
