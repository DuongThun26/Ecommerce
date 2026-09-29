package com.example.ecommerce.product.service.impl;

import com.example.ecommerce.common.exception.ResourceInUseException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.common.service.CloudinaryService;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.entity.ProductImageEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.product.repository.ProductImageRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        ProductEntity product = productMapper.toEntity(request);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.AVAILABLE);
        }
        product.setDeleted(false);

        attachImageUrlsToProduct(product, request.getImageUrls());

        productRepository.save(product);
        int imageCount = product.getImages() != null ? product.getImages().size() : 0;
        log.info("Created product #{}: {} with {} images", product.getId(), product.getName(), imageCount);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files) {
        List<String> uploadedUrls = new ArrayList<>();
        if (request.getImageUrls() != null) {
            uploadedUrls.addAll(request.getImageUrls());
        }
        if (files != null && !files.isEmpty()) {
            List<String> cloudinaryUrls = cloudinaryService.uploadMultiple(files);
            uploadedUrls.addAll(cloudinaryUrls);
        }
        request.setImageUrls(uploadedUrls);
        return createProduct(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        productMapper.updateEntity(request, product);

        if (request.getImageUrls() != null) {
            product.clearImages();
            attachImageUrlsToProduct(product, request.getImageUrls());
        }

        productRepository.save(product);
        int imageCount = product.getImages() != null ? product.getImages().size() : 0;
        log.info("Updated product #{}: {} with {} images", product.getId(), product.getName(), imageCount);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files) {
        List<String> uploadedUrls = new ArrayList<>();
        if (request.getImageUrls() != null) {
            uploadedUrls.addAll(request.getImageUrls());
        }
        if (files != null && !files.isEmpty()) {
            List<String> cloudinaryUrls = cloudinaryService.uploadMultiple(files);
            uploadedUrls.addAll(cloudinaryUrls);
        }
        request.setImageUrls(uploadedUrls);
        return updateProduct(id, request);
    }

    @Override
    @Transactional
    public void deleteProductById(Long id) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));
        if (orderItemRepository.countByProductId(id) > 0) {
            throw new ResourceInUseException("Product with id " + id + " has been used in orders and cannot be deleted.");
        }
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Soft deleted product #{}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseSummary> getProducts(ProductSearchRequest searchRequest, Pageable pageable) {
        Page<ProductEntity> page = productRepository.findAll(
                ProductSpecification.withFilters(searchRequest),
                pageable
        );
        Page<ProductResponseSummary> responsePage = page.map(productMapper::toResponseSummary);
        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional
    public ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files)  {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));
        if (files != null && !files.isEmpty()) {
            List<String> urls = cloudinaryService.uploadMultiple(files);
            int currentOrder = product.getImages() != null ? product.getImages().size() : 0;
            boolean hasPrimary = product.getImages() != null && product.getImages().stream().anyMatch(ProductImageEntity::isPrimary);

            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                boolean isPrimary = (!hasPrimary && i == 0 && currentOrder == 0);
                ProductImageEntity imageEntity = ProductImageEntity.builder()
                        .imageUrl(url)
                        .isPrimary(isPrimary)
                        .displayOrder(currentOrder + i)
                        .build();
                product.addImage(imageEntity);
            }
            productRepository.save(product);
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));
        ProductImageEntity target = null;
        if (product.getImages() != null) {
            for (ProductImageEntity img : product.getImages()) {
                if (img.getId() != null && img.getId().equals(imageId)) {
                    target = img;
                    break;
                }
            }
        }
        if (target != null) {
            product.removeImage(target);
            productRepository.save(product);
            log.info("Deleted image #{} from product #{}", imageId, productId);
        }
    }

    private void attachImageUrlsToProduct(ProductEntity product, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                if (url != null && !url.isBlank()) {
                    ProductImageEntity imageEntity = ProductImageEntity.builder()
                            .imageUrl(url.trim())
                            .isPrimary(i == 0)
                            .displayOrder(i)
                            .build();
                    product.addImage(imageEntity);
                }
            }
        }
    }
}
