package com.example.ecommerce.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request duyệt phiếu mua hàng bởi Product Owner")
public class TicketApproveRequest {

    @Schema(description = "Ghi chú phê duyệt của Product Owner", example = "Đã kiểm tra và phê duyệt số lượng sản phẩm hợp lệ.")
    private String notes;
}
