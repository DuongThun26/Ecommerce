package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderCancelRequest;
import com.example.ecommerce.order.dto.request.OrderStatusRequest;
import com.example.ecommerce.order.dto.request.OrderUpdateRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(
            summary = "Tạo phiếu mua hàng mới (Nộp duyệt)",
            description = "API cho phép tạo phiếu mua hàng, trừ giữ kho và chuyển vào hàng đợi duyệt của Product Owner (PENDING_APPROVAL)."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        OrderResponse order = orderService.createOrder(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phiếu mua hàng gửi PO duyệt thành công", order));
    }

    @Operation(
            summary = "Tạo đơn hàng nháp",
            description = "API cho phép lưu nháp đơn hàng mà chưa gửi lên PO duyệt (DRAFT)."
    )
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<OrderResponse>> createDraftOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        OrderResponse order = orderService.createDraftOrder(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu đơn hàng nháp thành công", order));
    }

    @Operation(
            summary = "Nộp đơn nháp hoặc đơn trả lại lên PO duyệt",
            description = "API nộp đơn từ trạng thái DRAFT hoặc RETURNED sang PENDING_APPROVAL để PO duyệt."
    )
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(
            @PathVariable Long id,
            Authentication authentication) {
        String actorType = resolveActorType(authentication);
        String actorIdentifier = authentication == null ? null : authentication.getName();
        OrderResponse order = orderService.submitOrder(id, actorType, actorIdentifier);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Nộp đơn hàng lên PO duyệt thành công", order));
    }

    @Operation(
            summary = "Cập nhật sản phẩm đơn hàng bị trả lại",
            description = "API cho phép khách hàng chỉnh sửa sản phẩm của đơn hàng ở trạng thái RETURNED và gửi lại PO duyệt."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateReturnedOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request,
            Authentication authentication) {
        String actorType = resolveActorType(authentication);
        String actorIdentifier = authentication == null ? null : authentication.getName();
        OrderResponse order = orderService.updateReturnedOrder(id, request, actorType, actorIdentifier);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật và nộp lại phiếu mua hàng thành công", order));
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
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request,
            Authentication authentication) {
        String actorType = resolveActorType(authentication);
        String actorIdentifier = authentication == null ? null : authentication.getName();
        OrderResponse order = orderService.updateOrderStatus(id, request.getStatus(), actorType, actorIdentifier, request.getReason());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công.", order));
    }

    @Operation(
            summary = "Hủy đơn hàng",
            description = "API cho phép hủy đơn hàng trước khi thanh toán hoàn tất."
    )
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) OrderCancelRequest request,
            Authentication authentication) {
        String actorType = resolveActorType(authentication);
        String actorIdentifier = authentication == null ? null : authentication.getName();
        String reason = request == null ? null : request.getReason();
        OrderResponse order = orderService.cancelOrder(id, actorType, actorIdentifier, reason);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Hủy đơn hàng thành công.", order));
    }

    @Operation(
            summary = "Lấy lịch sử trạng thái đơn hàng",
            description = "API cho phép lấy toàn bộ lịch sử chuyển trạng thái của đơn hàng."
    )
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderHistory(@PathVariable Long id) {
        List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy lịch sử trạng thái đơn hàng thành công.", history));
    }

    private String resolveActorType(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "SYSTEM";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return "ADMIN";
            }
            if ("ROLE_PRODUCT_OWNER".equals(authority.getAuthority())) {
                return "PRODUCT_OWNER";
            }
            if ("ROLE_CUSTOMER".equals(authority.getAuthority())) {
                return "CUSTOMER";
            }
        }
        return "SYSTEM";
    }
}
