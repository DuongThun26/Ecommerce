package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.ProductEntity;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.exception.ResourceInUseException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.impl.ProductServiceImpl;
import com.example.ecommerce.testdata.ProductTestDataBuilder;
import com.example.ecommerce.testdata.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("createProduct should set default status and deleted=false")
    void createProductShouldSetDefaults() {
        ProductRequest request = TestDataFactory.createProductRequest("P1", BigDecimal.valueOf(120_000), 5);
        ProductEntity entity = ProductTestDataBuilder.aProduct().withName("P1").withStatus(null).withDeleted(true).build();
        ProductResponse response = TestDataFactory.createProductResponse(1L, "P1", BigDecimal.valueOf(120_000), 5);

        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertThat(result.getName()).isEqualTo("P1");
        assertThat(entity.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(entity.isDeleted()).isFalse();
        verify(productRepository).save(entity);
    }

    @Test
    @DisplayName("getProductById should throw when product is deleted or missing")
    void getProductByIdShouldThrowWhenMissing() {
        when(productRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteProductById should reject product already used in orders")
    void deleteProductByIdShouldRejectUsedProduct() {
        ProductEntity entity = ProductTestDataBuilder.aProduct().withId(3L).build();
        when(productRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        when(orderItemRepository.countByProductId(3L)).thenReturn(1L);

        assertThatThrownBy(() -> productService.deleteProductById(3L))
                .isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("deleteProductById should soft delete when unused")
    void deleteProductByIdShouldSoftDelete() {
        ProductEntity entity = ProductTestDataBuilder.aProduct().withId(3L).withDeleted(false).build();
        when(productRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        when(orderItemRepository.countByProductId(3L)).thenReturn(0L);

        productService.deleteProductById(3L);

        assertThat(entity.isDeleted()).isTrue();
        verify(productRepository).save(entity);
    }
}
