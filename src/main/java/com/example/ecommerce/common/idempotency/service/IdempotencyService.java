package com.example.ecommerce.common.idempotency.service;

import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.idempotency.dto.IdempotentResult;
import com.example.ecommerce.common.idempotency.entity.IdempotencyKeyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;

    /**
     * Thực thi một hành động có áp dụng kiểm tra Idempotency theo chuẩn API.
     *
     * @param idempotencyKey Chuỗi khóa phân biệt từ client gửi lên (hoặc null nếu client không truyền)
     * @param endpoint       Đường dẫn API (vd: /payments/vnpay)
     * @param requestPayload Request body hoặc dữ liệu đầu vào cần kiểm tra
     * @param responseType   Kiểu dữ liệu phản hồi (Class<T>)
     * @param action         Hàm thực thi nghiệp vụ chính
     * @return Kết quả IdempotentResult chứa data và cờ replayed
     */
    public <T> IdempotentResult<T> execute(
            String idempotencyKey,
            String endpoint,
            Object requestPayload,
            Class<T> responseType,
            Supplier<T> action
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return IdempotentResult.fresh(action.get());
        }

        String key = idempotencyKey.trim();
        String requestHash = computeHash(endpoint, requestPayload);

        // 1. Kiểm tra hoặc chiếm quyền xử lý (Claim Key)
        Optional<IdempotencyKeyEntity> cachedEntity = store.claimOrGet(key, endpoint, requestHash);

        // 2. Nếu đã có kết quả hoàn thành từ trước -> Replay ngay
        if (cachedEntity.isPresent()) {
            String savedBody = cachedEntity.get().getResponseBody();
            log.info("Idempotency key '{}' hit. Replaying cached response for endpoint '{}'.", key, endpoint);
            try {
                T cachedData = objectMapper.readValue(savedBody, responseType);
                return IdempotentResult.replayed(cachedData);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached response for key: {}", key, e);
                throw new OrderProcessingException("Không thể đọc kết quả giao dịch đã lưu trước đó.");
            }
        }

        // 3. Thực thi hành động
        T result;
        try {
            result = action.get();
        } catch (Exception ex) {
            log.warn("Execution failed under idempotency key: {}. Marking as FAILED.", key);
            store.markFailed(key);
            throw ex;
        }

        // 4. Lưu lại kết quả sau khi xử lý thành công
        try {
            String responseBody = objectMapper.writeValueAsString(result);
            store.markCompleted(key, 200, responseBody);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency key: {}", key, e);
        }

        return IdempotentResult.fresh(result);
    }

    private String computeHash(String endpoint, Object payload) {
        try {
            String raw;
            if (payload == null) {
                raw = endpoint + ":EMPTY";
            } else if (payload instanceof String str) {
                raw = endpoint + ":" + str;
            } else {
                raw = endpoint + ":" + objectMapper.writeValueAsString(payload);
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new RuntimeException("Error calculating payload hash for idempotency", e);
        }
    }
}
