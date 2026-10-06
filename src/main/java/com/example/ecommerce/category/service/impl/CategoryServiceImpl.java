package com.example.ecommerce.category.service.impl;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.category.dto.request.CategoryRequest;
import com.example.ecommerce.category.dto.response.CategoryResponse;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce.category.entity.CategoryEntity;
import com.example.ecommerce.category.mapper.CategoryMapper;
import com.example.ecommerce.category.repository.CategoryRepository;
import com.example.ecommerce.category.service.CategoryService;
import com.example.ecommerce.category.specification.CategorySpecification;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.common.exception.ResourceInUseException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCodeIgnoreCaseAndDeletedFalse(normalizedCode)) {
            throw new DuplicateResourceException("Mã loại hàng '" + normalizedCode + "' đã tồn tại trong hệ thống.");
        }

        UserEntity poUser = null;
        if (request.getManagedByUserId() != null) {
            poUser = userRepository.findById(request.getManagedByUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + request.getManagedByUserId()));
            validatePoUser(poUser);
        }

        CategoryEntity category = categoryMapper.toEntity(request);
        category.setCode(normalizedCode);
        category.setName(request.getName().trim());
        category.setManagedBy(poUser);
        category.setDeleted(false);

        CategoryEntity savedCategory = categoryRepository.save(category);
        log.info("Created category #{}: {} managed by PO: {}",
                savedCategory.getId(), savedCategory.getCode(), poUser != null ? poUser.getUsername() : "Unassigned");
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với ID: " + id));

        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedFalse(normalizedCode, id)) {
            throw new DuplicateResourceException("Mã loại hàng '" + normalizedCode + "' đã được sử dụng bởi loại hàng khác.");
        }

        UserEntity poUser = null;
        if (request.getManagedByUserId() != null) {
            poUser = userRepository.findById(request.getManagedByUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + request.getManagedByUserId()));
            validatePoUser(poUser);
        }

        categoryMapper.updateEntity(request, category);
        category.setCode(normalizedCode);
        category.setName(request.getName().trim());
        category.setManagedBy(poUser);

        CategoryEntity updatedCategory = categoryRepository.save(category);
        log.info("Updated category #{}: {} new managed PO: {}",
                updatedCategory.getId(), updatedCategory.getCode(), poUser != null ? poUser.getUsername() : "Unassigned");
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với ID: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByCode(String code) {
        CategoryEntity category = categoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với mã: " + code));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(Map<String, Object> params, Pageable pageable) {
        String keyword = null;
        Long managedByUserId = null;
        String managedByUsername = null;

        if (params != null) {
            Object kw = params.get("keyword");
            if (kw != null && !kw.toString().isBlank()) {
                keyword = kw.toString().trim();
            }
            Object poId = params.get("managedByUserId");
            if (poId != null && !poId.toString().isBlank()) {
                try {
                    managedByUserId = Long.parseLong(poId.toString().trim());
                } catch (NumberFormatException ignored) {}
            }
            Object poUsername = params.get("managedByUsername");
            if (poUsername != null && !poUsername.toString().isBlank()) {
                managedByUsername = poUsername.toString().trim();
            }
        }

        Specification<CategoryEntity> spec = CategorySpecification.withFilters(keyword, managedByUserId, managedByUsername);
        Page<CategoryEntity> page = categoryRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(categoryMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getMyManagedCategories(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }

        // Nếu là ADMIN thì trả về tất cả các category
        if (user.getRole() == UserRole.ADMIN) {
            List<CategoryEntity> allCategories = categoryRepository.findAllByDeletedFalse();
            return categoryMapper.toSummaryResponseList(allCategories);
        }

        // Nếu là PO thì trả về các category mà PO này trực tiếp quản lý
        List<CategoryEntity> categories = categoryRepository.findByManagedByUsernameAndDeletedFalse(username);
        return categoryMapper.toSummaryResponseList(categories);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại hàng với ID: " + id));

        // Kiểm tra xem loại hàng có sản phẩm chưa bị xóa hay không
        if (category.getProducts() != null && category.getProducts().stream().anyMatch(p -> !p.isDeleted())) {
            throw new ResourceInUseException("Không thể xóa loại hàng '" + category.getName() + "' vì đang có sản phẩm thuộc loại hàng này.");
        }

        category.setDeleted(true);
        categoryRepository.save(category);
        log.info("Soft deleted category #{}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserManagingCategory(String username, Long categoryId) {
        if (username == null || categoryId == null) {
            return false;
        }

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        if (user.getRole() != UserRole.PRODUCT_OWNER) {
            return false;
        }

        CategoryEntity category = categoryRepository.findByIdAndDeletedFalse(categoryId).orElse(null);
        if (category == null || category.getManagedBy() == null) {
            return false;
        }

        return user.getId().equals(category.getManagedBy().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserManagingAllProductCategories(String username, List<Long> productIds) {
        if (username == null || productIds == null || productIds.isEmpty()) {
            return false;
        }

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        if (user.getRole() != UserRole.PRODUCT_OWNER) {
            return false;
        }

        List<ProductEntity> products = productRepository.findAllById(productIds);
        if (products.isEmpty()) {
            return false;
        }

        for (ProductEntity product : products) {
            CategoryEntity category = product.getCategory();
            if (category == null || category.getManagedBy() == null || !user.getId().equals(category.getManagedBy().getId())) {
                return false;
            }
        }

        return true;
    }

    private void validatePoUser(UserEntity user) {
        if (user.getRole() != UserRole.PRODUCT_OWNER && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Người dùng '" + user.getUsername() + "' không có vai trò PRODUCT_OWNER hoặc ADMIN để quản lý loại hàng.");
        }
    }
}
