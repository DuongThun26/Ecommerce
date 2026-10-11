package com.example.ecommerce.controller;

import com.example.ecommerce.common.config.ResponseWrapperAdvice;
import com.example.ecommerce.common.exception.GlobalExceptionHandler;
import com.example.ecommerce.common.exception.IdempotencyConflictException;
import com.example.ecommerce.common.exception.IdempotencyPayloadMismatchException;
import com.example.ecommerce.common.idempotency.dto.IdempotentResult;
import com.example.ecommerce.common.idempotency.service.IdempotencyService;
import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.service.PaymentService;
import com.example.ecommerce.payment.vnpay.controller.VnPayController;
import com.example.ecommerce.payment.vnpay.dto.request.VnPayPaymentRequest;
import com.example.ecommerce.payment.vnpay.dto.response.VnPayPaymentResponse;
import com.example.ecommerce.payment.vnpay.service.VnPayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VnPayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ResponseWrapperAdvice.class})
class VnPayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VnPayService vnPayService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("POST /payments/vnpay không có header Idempotency-Key -> tạo payment bình thường")
    void createPaymentWithoutHeaderShouldSucceed() throws Exception {
        when(vnPayService.createPaymentUrl(eq(100L), any())).thenReturn("http://vnpay.vn/pay?token=xyz");
        when(paymentService.getPaymentByOrderId(100L)).thenReturn(PaymentResponse.builder()
                .id(1L)
                .transactionRef("TXN_100")
                .build());

        VnPayPaymentRequest req = new VnPayPaymentRequest(100L);

        mockMvc.perform(post("/payments/vnpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.data.transactionRef").value("TXN_100"))
                .andExpect(jsonPath("$.data.paymentUrl").value("http://vnpay.vn/pay?token=xyz"));
    }

    @Test
    @DisplayName("POST /payments/vnpay có header Idempotency-Key lần đầu -> xử lý và trả về 200 OK")
    void createPaymentWithIdempotencyKeyFresh() throws Exception {
        VnPayPaymentResponse resp = VnPayPaymentResponse.builder()
                .paymentId(1L)
                .transactionRef("TXN_FRESH")
                .paymentUrl("http://vnpay.vn/fresh")
                .build();

        when(idempotencyService.execute(eq("key-uuid-1"), eq("/payments/vnpay"), any(), eq(VnPayPaymentResponse.class), any()))
                .thenReturn(IdempotentResult.fresh(resp));

        VnPayPaymentRequest req = new VnPayPaymentRequest(100L);

        mockMvc.perform(post("/payments/vnpay")
                        .header("Idempotency-Key", "key-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.data.transactionRef").value("TXN_FRESH"));
    }

    @Test
    @DisplayName("POST /payments/vnpay có header Idempotency-Key retry -> trả về 200 OK kèm header Idempotent-Replayed: true")
    void createPaymentWithIdempotencyKeyReplayed() throws Exception {
        VnPayPaymentResponse resp = VnPayPaymentResponse.builder()
                .paymentId(1L)
                .transactionRef("TXN_REPLAYED")
                .paymentUrl("http://vnpay.vn/replayed")
                .build();

        when(idempotencyService.execute(eq("key-uuid-1"), eq("/payments/vnpay"), any(), eq(VnPayPaymentResponse.class), any()))
                .thenReturn(IdempotentResult.replayed(resp));

        VnPayPaymentRequest req = new VnPayPaymentRequest(100L);

        mockMvc.perform(post("/payments/vnpay")
                        .header("Idempotency-Key", "key-uuid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.transactionRef").value("TXN_REPLAYED"));
    }

    @Test
    @DisplayName("POST /payments/vnpay double-click đồng thời -> trả về 409 Conflict với IDEMPOTENCY_CONFLICT")
    void createPaymentWithConcurrentConflictShouldReturn409() throws Exception {
        when(idempotencyService.execute(eq("key-double-click"), eq("/payments/vnpay"), any(), eq(VnPayPaymentResponse.class), any()))
                .thenThrow(new IdempotencyConflictException("Yêu cầu với Idempotency-Key này đang được xử lý"));

        VnPayPaymentRequest req = new VnPayPaymentRequest(100L);

        mockMvc.perform(post("/payments/vnpay")
                        .header("Idempotency-Key", "key-double-click")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Yêu cầu với Idempotency-Key này đang được xử lý"));
    }

    @Test
    @DisplayName("POST /payments/vnpay cùng Idempotency-Key nhưng khác payload -> trả về 422 Unprocessable Entity với IDEMPOTENCY_PAYLOAD_MISMATCH")
    void createPaymentWithPayloadMismatchShouldReturn422() throws Exception {
        when(idempotencyService.execute(eq("key-same"), eq("/payments/vnpay"), any(), eq(VnPayPaymentResponse.class), any()))
                .thenThrow(new IdempotencyPayloadMismatchException("Idempotency-Key đã được sử dụng với payload khác"));

        VnPayPaymentRequest req = new VnPayPaymentRequest(200L);

        mockMvc.perform(post("/payments/vnpay")
                        .header("Idempotency-Key", "key-same")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_PAYLOAD_MISMATCH"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key đã được sử dụng với payload khác"));
    }
}
