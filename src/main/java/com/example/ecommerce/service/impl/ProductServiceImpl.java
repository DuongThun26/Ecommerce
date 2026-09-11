package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductResponseSummary;
import com.example.ecommerce.entity.ProductEntity;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.exception.DuplicateResourceException;
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
        if (productRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Product with name " + request.getName() + " already exists.");
        }
        ProductEntity product = productMapper.toEntity(request);
        product.setStatus(ProductStatus.AVAILABLE);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponseSummary> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return productMapper.toResponseSummaryList(products);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        if (productRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Product with name " + request.getName() + " already exists.");
        }
        productMapper.updateEntity(request, product);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteProductById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        // Chưa xử lý logic không cho xóa sản phẩm đã được thêm vào giỏ hàng
        productRepository.delete(product);
    }
}
