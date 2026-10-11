package com.example.ecommerce.common.idempotency.service;

import com.example.ecommerce.common.exception.IdempotencyConflictException;
import com.example.ecommerce.common.exception.IdempotencyPayloadMismatchException;
import com.example.ecommerce.common.idempotency.entity.IdempotencyKeyEntity;
import com.example.ecommerce.common.idempotency.enums.IdempotencyStatus;
import com.example.ecommerce.common.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyStore {

    private final IdempotencyKeyRepository repository;

    /**
     * Khởi tạo hoặc xác thực Idempotency Key trong một giao dịch độc lập (REQUIRES_NEW).
     * Đảm bảo bản ghi PROCESSING được commit ngay lập tức để chặn concurrent double-click.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {IdempotencyConflictException.class, IdempotencyPayloadMismatchException.class})
    public Optional<IdempotencyKeyEntity> claimOrGet(String key, String endpoint, String requestHash) {
        Optional<IdempotencyKeyEntity> existingOpt = repository.findByIdempotencyKey(key);

        if (existingOpt.isPresent()) {
            IdempotencyKeyEntity existing = existingOpt.get();

            // Kiểm tra tính toàn vẹn payload: cùng key nhưng khác nội dung request
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyPayloadMismatchException(
                        "Idempotency-Key '" + key + "' đã được sử dụng với nội dung request khác."
                );
            }

            // Nếu request trước đang được xử lý dở -> xung đột đồng thời (Double-click / Concurrent)
            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new IdempotencyConflictException(
                        "Yêu cầu với Idempotency-Key '" + key + "' đang được xử lý. Vui lòng thử lại sau giây lát."
                );
            }

            // Nếu đã hoàn thành trước đó -> trả về để replay response đã lưu
            if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                return Optional.of(existing);
            }

            // Nếu lần trước bị FAILED -> cho phép thử lại: cập nhật lại thành PROCESSING
            existing.setStatus(IdempotencyStatus.PROCESSING);
            existing.setEndpoint(endpoint);
            existing.setResponseBody(null);
            existing.setResponseStatus(null);
            repository.saveAndFlush(existing);
            return Optional.empty();
        }

        // Chưa tồn tại -> Tạo mới bản ghi với trạng thái PROCESSING
        IdempotencyKeyEntity newKey = IdempotencyKeyEntity.builder()
                .idempotencyKey(key)
                .endpoint(endpoint)
                .requestHash(requestHash)
                .status(IdempotencyStatus.PROCESSING)
                .build();

        try {
            repository.saveAndFlush(newKey);
            return Optional.empty(); // Rỗng nghĩa là vừa claim thành công, cần tiến hành execute action
        } catch (DataIntegrityViolationException ex) {
            // Race condition: Thread khác vừa insert cùng micro-giây
            log.warn("Concurrent insert race condition for idempotency key: {}", key);
            throw new IdempotencyConflictException(
                    "Yêu cầu với Idempotency-Key '" + key + "' đang được xử lý đồng thời. Vui lòng thử lại sau giây lát."
            );
        }
    }

    /**
     * Đánh dấu thành công và lưu lại kết quả response cho các lần gọi lại (Replay).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, int responseStatus, String responseBody) {
        repository.findByIdempotencyKey(key).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.COMPLETED);
            entity.setResponseStatus(responseStatus);
            entity.setResponseBody(responseBody);
            repository.saveAndFlush(entity);
        });
    }

    /**
     * Đánh dấu thất bại nếu quá trình thực thi ném ngoại lệ hệ thống.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        repository.findByIdempotencyKey(key).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.FAILED);
            repository.saveAndFlush(entity);
        });
    }
}
