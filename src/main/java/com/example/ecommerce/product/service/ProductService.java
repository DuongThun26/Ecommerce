package com.example.ecommerce.product.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files);
    ProductResponse getProductById(Long id);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files);
    void deleteProductById(Long id);
    PageResponse<ProductResponseSummary> getProducts(ProductSearchRequest searchRequest, Pageable pageable);
    ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files);
    void deleteProductImage(Long productId, Long imageId);
}
