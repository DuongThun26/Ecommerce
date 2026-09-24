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
@Schema(description = "Request trả lại phiếu mua hàng yêu cầu khách hàng chỉnh sửa")
public class TicketReturnRequest {

    @NotBlank(message = "Lý do trả lại không được để trống")
    @Schema(description = "Lý do trả lại phiếu", example = "Vui lòng điều chỉnh lại số lượng đặt mua theo bội số của thùng (10 cái/thùng).")
    private String reason;
}
