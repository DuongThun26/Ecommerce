package com.example.ecommerce.category.controller;

import com.example.ecommerce.category.dto.request.CategoryRequest;
import com.example.ecommerce.category.dto.response.CategoryResponse;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce.category.service.CategoryService;
import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
@Tag(
        name = "Category Management",
        description = "APIs quản lý loại hàng hóa và phân công Product Owner sở hữu từng loại hàng."
)
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Tạo loại hàng mới và gán PO quản lý (Admin)",
            description = "API cho phép Admin tạo loại hàng hóa mới và chỉ định Product Owner phụ trách loại hàng đó."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo loại hàng thành công", category));
    }

    @Operation(
            summary = "Cập nhật loại hàng và thay đổi PO phụ trách (Admin)",
            description = "API cho phép Admin chỉnh sửa thông tin loại hàng hoặc chuyển quyền sở hữu sang PO khác."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật loại hàng thành công", category));
    }

    @Operation(
            summary = "Lấy danh sách các loại hàng mà PO hiện tại đang quản lý",
            description = "API trả về danh sách các loại hàng được phân công cho PO đang đăng nhập (hoặc toàn bộ nếu là Admin)."
    )
    @GetMapping("/my-categories")
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> getMyManagedCategories(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<CategorySummaryResponse> categories = categoryService.getMyManagedCategories(username);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách loại hàng phụ trách thành công", categories));
    }

    @Operation(
            summary = "Lấy danh sách loại hàng (Hỗ trợ tìm kiếm & phân trang)",
            description = "API lấy danh sách loại hàng hóa có lọc theo từ khóa, PO phụ trách và phân trang."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
            @RequestParam(required = false) Map<String, Object> params,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CategoryResponse> categories = categoryService.getCategories(params, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách loại hàng thành công", categories));
    }

    @Operation(
            summary = "Lấy chi tiết loại hàng theo ID",
            description = "API lấy thông tin chi tiết loại hàng theo ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin loại hàng thành công", category));
    }

    @Operation(
            summary = "Lấy chi tiết loại hàng theo mã code",
            description = "API lấy thông tin chi tiết loại hàng theo mã code."
    )
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByCode(@PathVariable String code) {
        CategoryResponse category = categoryService.getCategoryByCode(code);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin loại hàng theo code thành công", category));
    }

    @Operation(
            summary = "Xóa loại hàng theo ID (Admin)",
            description = "API cho phép Admin xóa mềm loại hàng (chỉ xóa được khi không có sản phẩm hoạt động)."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Xóa loại hàng thành công", null));
    }
}
