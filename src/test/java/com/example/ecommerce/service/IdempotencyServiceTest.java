package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.IdempotencyConflictException;
import com.example.ecommerce.common.exception.IdempotencyPayloadMismatchException;
import com.example.ecommerce.common.idempotency.dto.IdempotentResult;
import com.example.ecommerce.common.idempotency.entity.IdempotencyKeyEntity;
import com.example.ecommerce.common.idempotency.enums.IdempotencyStatus;
import com.example.ecommerce.common.idempotency.repository.IdempotencyKeyRepository;
import com.example.ecommerce.common.idempotency.service.IdempotencyService;
import com.example.ecommerce.common.idempotency.service.IdempotencyStore;
import com.example.ecommerce.payment.vnpay.dto.request.VnPayPaymentRequest;
import com.example.ecommerce.payment.vnpay.dto.response.VnPayPaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    private ObjectMapper objectMapper;
    private IdempotencyStore store;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        store = new IdempotencyStore(repository);
        idempotencyService = new IdempotencyService(store, objectMapper);
    }

    @Test
    @DisplayName("Lần đầu gọi: key mới chưa tồn tại -> thực thi action và lưu kết quả COMPLETED")
    void executeFirstTimeShouldRunActionAndComplete() {
        String key = "key-first-run";
        VnPayPaymentRequest request = new VnPayPaymentRequest(100L);

        IdempotencyKeyEntity saved = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .endpoint("/payments/vnpay")
                .requestHash("some-hash")
                .status(IdempotencyStatus.PROCESSING)
                .build();
        when(repository.findByIdempotencyKey(key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger callCount = new AtomicInteger(0);
        IdempotentResult<VnPayPaymentResponse> result = idempotencyService.execute(
                key,
                "/payments/vnpay",
                request,
                VnPayPaymentResponse.class,
                () -> {
                    callCount.incrementAndGet();
                    return VnPayPaymentResponse.builder()
                            .paymentId(1L)
                            .transactionRef("TXN_100")
                            .paymentUrl("http://vnpay.test/pay?txn=100")
                            .build();
                }
        );

        assertThat(result.isReplayed()).isFalse();
        assertThat(result.getData().getTransactionRef()).isEqualTo("TXN_100");
        assertThat(callCount.get()).isEqualTo(1);

        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Replay: gọi lại cùng key và payload khi đã COMPLETED -> trả về cached response và không chạy lại action")
    void executeReplayShouldReturnCachedDataWithoutExecutingAction() {
        String key = "key-replay";
        VnPayPaymentRequest request = new VnPayPaymentRequest(100L);

        // Giả lập key đã COMPLETED trước đó
        IdempotencyKeyEntity existing = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .endpoint("/payments/vnpay")
                .requestHash(calculateHash("/payments/vnpay", request))
                .status(IdempotencyStatus.COMPLETED)
                .responseStatus(200)
                .responseBody("{\"paymentId\":1,\"transactionRef\":\"TXN_CACHED\",\"paymentUrl\":\"http://vnpay.test/cached\"}")
                .build();

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        AtomicInteger callCount = new AtomicInteger(0);
        IdempotentResult<VnPayPaymentResponse> result = idempotencyService.execute(
                key,
                "/payments/vnpay",
                request,
                VnPayPaymentResponse.class,
                () -> {
                    callCount.incrementAndGet();
                    return VnPayPaymentResponse.builder().build();
                }
        );

        assertThat(result.isReplayed()).isTrue();
        assertThat(result.getData().getTransactionRef()).isEqualTo("TXN_CACHED");
        assertThat(callCount.get()).isEqualTo(0); // Không chạy lại action!
    }

    @Test
    @DisplayName("Double-click: gọi cùng key khi request trước vẫn đang PROCESSING -> ném IdempotencyConflictException (409)")
    void executeConcurrentProcessingShouldThrowConflictException() {
        String key = "key-concurrent";
        VnPayPaymentRequest request = new VnPayPaymentRequest(100L);

        IdempotencyKeyEntity inProgress = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .endpoint("/payments/vnpay")
                .requestHash(calculateHash("/payments/vnpay", request))
                .status(IdempotencyStatus.PROCESSING)
                .build();

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> idempotencyService.execute(
                key,
                "/payments/vnpay",
                request,
                VnPayPaymentResponse.class,
                () -> VnPayPaymentResponse.builder().build()
        )).isInstanceOf(IdempotencyConflictException.class)
          .hasMessageContaining("đang được xử lý");
    }

    @Test
    @DisplayName("Payload Mismatch: dùng cùng Idempotency-Key nhưng đổi payload (khác orderId) -> ném IdempotencyPayloadMismatchException (422)")
    void executeWithDifferentPayloadShouldThrowMismatchException() {
        String key = "key-mismatch";
        VnPayPaymentRequest originalRequest = new VnPayPaymentRequest(100L);
        VnPayPaymentRequest modifiedRequest = new VnPayPaymentRequest(200L);

        IdempotencyKeyEntity existing = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .endpoint("/payments/vnpay")
                .requestHash(calculateHash("/payments/vnpay", originalRequest))
                .status(IdempotencyStatus.COMPLETED)
                .build();

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> idempotencyService.execute(
                key,
                "/payments/vnpay",
                modifiedRequest, // payload khác!
                VnPayPaymentResponse.class,
                () -> VnPayPaymentResponse.builder().build()
        )).isInstanceOf(IdempotencyPayloadMismatchException.class)
          .hasMessageContaining("được sử dụng với nội dung request khác");
    }

    @Test
    @DisplayName("Race condition DB: 2 thread cùng insert cùng lúc dính Unique Constraint -> ném IdempotencyConflictException")
    void executeConcurrentInsertConstraintViolationShouldThrowConflictException() {
        String key = "key-race";
        VnPayPaymentRequest request = new VnPayPaymentRequest(100L);

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate key entry"));

        assertThatThrownBy(() -> idempotencyService.execute(
                key,
                "/payments/vnpay",
                request,
                VnPayPaymentResponse.class,
                () -> VnPayPaymentResponse.builder().build()
        )).isInstanceOf(IdempotencyConflictException.class)
          .hasMessageContaining("đồng thời");
    }

    private String calculateHash(String endpoint, Object payload) {
        try {
            String raw = endpoint + ":" + objectMapper.writeValueAsString(payload);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
