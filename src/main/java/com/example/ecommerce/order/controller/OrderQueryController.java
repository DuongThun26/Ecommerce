package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Tag(
        name = "Order Queries",
        description = "APIs truy vấn đơn hàng (CQRS Read-side)."
)
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    @Operation(
            summary = "Lấy danh sách đơn hàng",
            description = "API cho phép lấy danh sách đơn hàng, hỗ trợ lọc theo ngày tạo, status, khoảng giá trị đơn hàng, keyword khách hàng và phân trang."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams) {
        PageResponse<OrderResponse> orders = orderQueryService.getOrders(params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
    }

    @Operation(
            summary = "Lấy thông tin đơn hàng theo ID",
            description = "API cho phép lấy chi tiết đơn hàng theo ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderQueryService.getOrderById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin đơn hàng thành công", order));
    }

    @Operation(
            summary = "Lấy chi tiết đơn hàng con theo ID",
            description = "API cho phép xem thông tin chi tiết một đơn hàng con."
    )
    @GetMapping("/sub-orders/{subOrderId}")
    public ResponseEntity<ApiResponse<SubOrderResponse>> getSubOrderById(@PathVariable Long subOrderId) {
        SubOrderResponse subOrder = orderQueryService.getSubOrderById(subOrderId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin đơn hàng con thành công.", subOrder));
    }

    @Operation(
            summary = "Lấy lịch sử trạng thái đơn hàng",
            description = "API cho phép lấy toàn bộ lịch sử chuyển trạng thái của đơn hàng."
    )
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderHistory(@PathVariable Long id) {
        List<OrderStatusHistoryResponse> history = orderQueryService.getOrderStatusHistory(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy lịch sử trạng thái đơn hàng thành công.", history));
    }
}
