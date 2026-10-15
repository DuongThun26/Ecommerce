package com.example.ecommerce.product.service.impl;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.category.entity.CategoryEntity;
import com.example.ecommerce.category.repository.CategoryRepository;
import com.example.ecommerce.common.exception.BadRequestException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        return createProduct(request, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        CategoryEntity category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với ID: " + request.getCategoryId()));
        }

        validatePoCategoryAccess(username, category);

        ProductEntity product = productMapper.toEntity(request);
        product.setCategory(category);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        product.setDeleted(false);

        attachImageUrlsToProduct(product, request.getImageUrls());

        productRepository.save(product);
        int imageCount = product.getImages() != null ? product.getImages().size() : 0;
        log.info("Created product #{}: {} under category: {} by user: {}",
                product.getId(), product.getName(), category != null ? category.getCode() : "None", username);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files) {
        return createProductWithFiles(request, files, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public ProductResponse createProductWithFiles(ProductRequest request, List<MultipartFile> files, String username) {
        List<String> uploadedUrls = new ArrayList<>();
        if (request.getImageUrls() != null) {
            uploadedUrls.addAll(request.getImageUrls());
        }
        if (files != null && !files.isEmpty()) {
            List<String> cloudinaryUrls = cloudinaryService.uploadMultiple(files);
            uploadedUrls.addAll(cloudinaryUrls);
        }
        request.setImageUrls(uploadedUrls);
        return createProduct(request, username);
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
        return updateProduct(id, request, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String username) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));

        // Kiểm tra quyền đối với category hiện tại của sản phẩm
        validatePoCategoryAccess(username, product.getCategory());

        CategoryEntity newCategory = product.getCategory();
        if (request.getCategoryId() != null) {
            if (product.getCategory() == null || !request.getCategoryId().equals(product.getCategory().getId())) {
                newCategory = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với ID: " + request.getCategoryId()));
                // Kiểm tra quyền đối với category mới
                validatePoCategoryAccess(username, newCategory);
            }
        }

        productMapper.updateEntity(request, product);
        product.setCategory(newCategory);

        if (request.getImageUrls() != null) {
            product.clearImages();
            attachImageUrlsToProduct(product, request.getImageUrls());
        }

        productRepository.save(product);
        int imageCount = product.getImages() != null ? product.getImages().size() : 0;
        log.info("Updated product #{}: {} under category: {} by user: {}",
                product.getId(), product.getName(), newCategory != null ? newCategory.getCode() : "None", username);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files) {
        return updateProductWithFiles(id, request, files, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public ProductResponse updateProductWithFiles(Long id, ProductRequest request, List<MultipartFile> files, String username) {
        List<String> uploadedUrls = new ArrayList<>();
        if (request.getImageUrls() != null) {
            uploadedUrls.addAll(request.getImageUrls());
        }
        if (files != null && !files.isEmpty()) {
            List<String> cloudinaryUrls = cloudinaryService.uploadMultiple(files);
            uploadedUrls.addAll(cloudinaryUrls);
        }
        request.setImageUrls(uploadedUrls);
        return updateProduct(id, request, username);
    }

    @Override
    @Transactional
    public void deleteProductById(Long id) {
        deleteProductById(id, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public void deleteProductById(Long id, String username) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found."));

        validatePoCategoryAccess(username, product.getCategory());

        if (orderItemRepository.countByProductId(id) > 0) {
            throw new ResourceInUseException("Product with id " + id + " has been used in orders and cannot be deleted.");
        }
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Soft deleted product #{} by user: {}", id, username);
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
    public ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files) {
        return addImagesToProduct(productId, files, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public ProductResponse addImagesToProduct(Long productId, List<MultipartFile> files, String username) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));

        validatePoCategoryAccess(username, product.getCategory());

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
        deleteProductImage(productId, imageId, resolveCurrentUsername());
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId, String username) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));

        validatePoCategoryAccess(username, product.getCategory());

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
            log.info("Deleted image #{} from product #{} by user: {}", imageId, productId, username);
        }
    }

    /**
     * Xác thực phân quyền PO: Nếu user là PRODUCT_OWNER thì bắt buộc phải sở hữu loại hàng (Category) của sản phẩm.
     */
    private void validatePoCategoryAccess(String username, CategoryEntity category) {
        if (username == null || username.isBlank()) {
            return;
        }

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getRole() == UserRole.ADMIN) {
            return; // Admin có toàn quyền trên mọi loại hàng
        }

        if (user.getRole() == UserRole.PRODUCT_OWNER) {
            if (category == null) {
                throw new BadRequestException("Sản phẩm phải thuộc một loại hàng được phân công cho bạn quản lý.");
            }
            if (category.getManagedBy() == null || !user.getId().equals(category.getManagedBy().getId())) {
                String assignedPoName = category.getManagedBy() != null ? category.getManagedBy().getUsername() : "Chưa phân công";
                throw new AccessDeniedException(String.format(
                        "Bạn không có quyền quản lý sản phẩm thuộc loại hàng '%s' (Mã: %s). Loại hàng này thuộc PO phụ trách: %s.",
                        category.getName(), category.getCode(), assignedPoName
                ));
            }
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
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
