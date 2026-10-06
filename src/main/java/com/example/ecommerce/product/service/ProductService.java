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
    ProductResponse createProduct(ProductRequest request, String username);
    ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files);
    ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files, String username);
    ProductResponse getProductById(Long id);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request, String username);
    ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files);
    ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files, String username);
    void deleteProductById(Long id);
    void deleteProductById(Long id, String username);
    PageResponse<ProductResponseSummary> getProducts(ProductSearchRequest searchRequest, Pageable pageable);
    ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files);
    ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files, String username);
    void deleteProductImage(Long productId, Long imageId);
    void deleteProductImage(Long productId, Long imageId, String username);
}
