package com.example.ecommerce.converter;

import com.example.ecommerce.dto.request.ProductSearchRequest;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.util.MapUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
@Component
public class ProductSearchConverter {
    public ProductSearchRequest toProductSearchRequest(Map<String, Object> map) {
        ProductSearchRequest request = new ProductSearchRequest();
        if (map == null) {
            return request;
        }
        request.setKeyword(MapUtil.get(map, "keyword", String.class));
        request.setStatus(MapUtil.get(map, "status", ProductStatus.class));
        request.setMinPrice(MapUtil.get(map, "minPrice", BigDecimal.class));
        request.setMaxPrice(MapUtil.get(map, "maxPrice", BigDecimal.class));

        return request;
    }
}

