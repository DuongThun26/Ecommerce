package com.example.ecommerce.product.service.impl;

import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.common.exception.ResourceInUseException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
