package com.example.ecommerce.category.service;

import com.example.ecommerce.category.dto.request.CategoryRequest;
import com.example.ecommerce.category.dto.response.CategoryResponse;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryByCode(String code);

    PageResponse<CategoryResponse> getCategories(Map<String, Object> params, Pageable pageable);

    List<CategorySummaryResponse> getMyManagedCategories(String username);

    void deleteCategory(Long id);

    boolean isUserManagingCategory(String username, Long categoryId);

    boolean isUserManagingAllProductCategories(String username, List<Long> productIds);
}
