package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.request.ProductSearchRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductResponseSummary;
import com.example.ecommerce.entity.ProductEntity;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.exception.ResourceInUseException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
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
        if (orderItemRepository.countByProductId(id) > 0) {
            throw new ResourceInUseException("Product with id " + id + " has been used in orders and cannot be deleted.");
        }
        product.setDeleted(true);
        productRepository.save(product);
    }

    @Override
    public PageResponse<ProductResponseSummary> getProducts(ProductSearchRequest searchRequest, Pageable pageable) {
        Page<ProductEntity> page = productRepository.findAll(
                ProductSpecification.withFilters(searchRequest),
                pageable
        );
        Page<ProductResponseSummary> responsePage = page.map(productMapper::toResponseSummary);
        return PageResponse.of(responsePage);
    }
}
