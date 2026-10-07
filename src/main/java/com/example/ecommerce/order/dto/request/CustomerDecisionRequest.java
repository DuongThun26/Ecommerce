package com.example.ecommerce.order.dto.request;

import com.example.ecommerce.order.enums.CustomerDecisionAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDecisionRequest {

    @NotNull(message = "Hành động quyết định không được để trống (ACCEPT_PARTIAL hoặc CANCEL_ALL)")
    private CustomerDecisionAction action;

    private String notes;
}
