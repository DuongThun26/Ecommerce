package com.example.ecommerce.customer.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.service.OrderService;
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
@RequestMapping("/customers")
@Tag(
        name = "Customer Orders",
        description = "APIs cho danh sách đơn hàng theo khách hàng."
)
public class CustomerOrderController {

    private final OrderService orderService;
    private final com.example.ecommerce.customer.repository.CustomerRepository customerRepository;

    @Operation(
            summary = "Lấy danh sách đơn hàng của tài khoản đang đăng nhập",
            description = "API cho phép xem lịch sử đơn hàng của chính mình."
    )
    @GetMapping({"/me/orders", "/orders"})
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        com.example.ecommerce.customer.entity.CustomerEntity customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("Customer profile not found for user: " + username));
        PageResponse<OrderResponse> orders = orderService.getOrdersByCustomerId(customer.getId(), params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách đơn hàng của bạn thành công", orders));
    }

    // Lấy toàn bộ đơn hàng của một khách hàng, hỗ trợ lọc trạng thái và phân trang.
    @Operation(
            summary = "Lấy danh sách đơn hàng theo khách hàng",
            description = "API cho phép lấy danh sách đơn hàng của một khách hàng, hỗ trợ lọc theo ngày tạo, status, khoảng giá trị đơn hàng, keyword và phân trang qua query params động."
    )
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams) {
        PageResponse<OrderResponse> orders = orderService.getOrdersByCustomerId(customerId, params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách đơn hàng theo khách hàng thành công", orders));
    }
}
