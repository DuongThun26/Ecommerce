package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderStatusRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn hàng thành công", order));
    }

    // Lấy danh sách đơn hàng, có thể lọc theo trạng thái và phân trang.
    @Operation(
            summary = "Lấy danh sách đơn hàng",
            description = "API cho phép lấy danh sách đơn hàng, hỗ trợ lọc theo ngày tạo, status, khoảng giá trị đơn hàng, keyword khách hàng và phân trang."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders (
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams){
        PageResponse<OrderResponse> orders = orderService.getOrders(params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
    }

    // Lấy chi tiết một đơn hàng theo ID.
    @Operation(
            summary = "Lấy thông tin đơn hàng theo ID",
            description = "API cho phép lấy chi tiết đơn hàng theo ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin đơn hàng thành công", order));
    }

    // Cập nhật trạng thái đơn hàng theo luồng chuyển trạng thái hợp lệ.
    @Operation(
            summary = "Cập nhật trạng thái đơn hàng",
            description = "API cho phép cập nhật trạng thái đơn hàng với transition hợp lệ."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công.", order));
    }
}
