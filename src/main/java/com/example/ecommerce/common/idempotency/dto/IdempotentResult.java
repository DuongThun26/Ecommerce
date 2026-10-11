package com.example.ecommerce.common.idempotency.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdempotentResult<T> {
    private final T data;
    private final boolean replayed;

    public static <T> IdempotentResult<T> fresh(T data) {
        return new IdempotentResult<>(data, false);
    }

    public static <T> IdempotentResult<T> replayed(T data) {
        return new IdempotentResult<>(data, true);
    }
}
