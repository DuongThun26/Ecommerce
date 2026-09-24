package com.example.ecommerce.ticket.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.ticket.dto.request.TicketApproveRequest;
import com.example.ecommerce.ticket.dto.request.TicketRejectRequest;
import com.example.ecommerce.ticket.dto.request.TicketReturnRequest;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import com.example.ecommerce.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/tickets")
@Tag(
        name = "Purchase Ticket Management",
        description = "APIs quản lý phiếu mua hàng và quy trình duyệt phiếu của Product Owner."
)
public class TicketController {

    private final TicketService ticketService;

    @Operation(
            summary = "Lấy danh sách phiếu mua hàng",
            description = "API cho phép lấy danh sách phiếu mua hàng có hỗ trợ lọc status, customerId, khoảng ngày tạo, keyword và phân trang."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getTickets(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams) {
        PageResponse<TicketResponse> tickets = ticketService.getTickets(params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách phiếu mua hàng thành công", tickets));
    }

    @Operation(
            summary = "Lấy chi tiết phiếu mua hàng theo Ticket ID",
            description = "API lấy chi tiết phiếu mua hàng cùng toàn bộ sản phẩm của đơn hàng tương ứng."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketById(@PathVariable Long id) {
        TicketDetailResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy chi tiết phiếu mua hàng thành công", ticket));
    }

    @Operation(
            summary = "Lấy chi tiết phiếu mua hàng theo Order ID",
            description = "API lấy phiếu mua hàng gắn liền với một đơn hàng cụ thể."
    )
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketByOrderId(@PathVariable Long orderId) {
        TicketDetailResponse ticket = ticketService.getTicketByOrderId(orderId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin phiếu mua hàng theo đơn hàng thành công", ticket));
    }

    @Operation(
            summary = "Product Owner duyệt phiếu mua hàng",
            description = "API cho phép Product Owner duyệt phiếu mua hàng và kích hoạt đơn sang trạng thái PAYMENT_PENDING."
    )
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<TicketResponse>> approveTicket(
            @PathVariable Long id,
            @RequestBody(required = false) TicketApproveRequest request,
            Authentication authentication) {
        String poUsername = authentication == null ? null : authentication.getName();
        TicketResponse ticket = ticketService.approveTicket(id, poUsername, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Duyệt phiếu mua hàng thành công", ticket));
    }

    @Operation(
            summary = "Product Owner từ chối phiếu mua hàng",
            description = "API cho phép Product Owner từ chối phiếu kèm lý do và hoàn trả lại kho sản phẩm."
    )
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TicketResponse>> rejectTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketRejectRequest request,
            Authentication authentication) {
        String poUsername = authentication == null ? null : authentication.getName();
        TicketResponse ticket = ticketService.rejectTicket(id, poUsername, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Từ chối phiếu mua hàng thành công", ticket));
    }

    @Operation(
            summary = "Product Owner trả lại phiếu mua hàng yêu cầu chỉnh sửa",
            description = "API cho phép Product Owner trả lại phiếu kèm lý do để khách hàng chỉnh sửa sản phẩm."
    )
    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<TicketResponse>> returnTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketReturnRequest request,
            Authentication authentication) {
        String poUsername = authentication == null ? null : authentication.getName();
        TicketResponse ticket = ticketService.returnTicket(id, poUsername, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Trả lại phiếu mua hàng yêu cầu chỉnh sửa thành công", ticket));
    }
}
