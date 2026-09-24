package com.example.ecommerce.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request từ chối phiếu mua hàng bởi Product Owner")
public class TicketRejectRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    @Schema(description = "Lý do từ chối phiếu", example = "Sản phẩm yêu cầu hiện không nằm trong kế hoạch phân phối quý này.")
    private String reason;
}
