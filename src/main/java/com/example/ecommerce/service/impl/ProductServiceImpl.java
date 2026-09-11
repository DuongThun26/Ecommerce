package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductResponseSummary;
import com.example.ecommerce.entity.ProductEntity;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        ProductEntity product = productMapper.toEntity(request);
        product.setStatus(ProductStatus.AVAILABLE);
        product.setDeleted(false);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponseSummary> getAllProducts() {
        List<ProductEntity> products = productRepository.findAllByDeletedFalse();
        return productMapper.toResponseSummaryList(products);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        productMapper.updateEntity(request, product);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteProductById(Long id) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        product.setDeleted(true);
        // Chưa có xử lý không cho xóa sản phẩm đã xuất hiện trong order.
        productRepository.save(product);
    }
}
