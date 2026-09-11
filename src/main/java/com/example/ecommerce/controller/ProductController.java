package com.example.ecommerce.controller;

import com.example.ecommerce.converter.ProductSearchConverter;
import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductResponseSummary;
import com.example.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Tag(
        name = "Product",
        description = "APIs cho việc quản lý sản phẩm."
)
public class ProductController {

    private final ProductService productService;
    private final ProductSearchConverter productSearchConverter;

    @Operation(
            summary = "Tạo sản phẩm mới",
            description = "API cho phép tạo mới sản phẩm với thông tin được cung cấp."
    )
    @PostMapping
    public ProductResponse createProduct(@RequestBody @Valid ProductRequest request) {
        return productService.createProduct(request);
    }

    @Operation(
            summary = "Lấy danh sách sản phẩm với tìm kiếm và phân trang",
            description = "API cho phép lấy danh sách sản phẩm với các điều kiện tìm kiếm tùy chọn (keyword, status, giá) và hỗ trợ phân trang, sorting. " +
                    "Ví dụ: /products?keyword=iPhone&status=AVAILABLE&minPrice=1000000&maxPrice=50000000&page=0&size=20&sort=price,asc"
    )
    @GetMapping
    public PageResponse<ProductResponseSummary> getProducts(
            @RequestParam Map<String, Object> params,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        var searchRequest = productSearchConverter.toProductSearchRequest(params);
        return productService.getProducts(searchRequest, pageable);
    }

    @Operation(
            summary = "Lấy thông tin sản phẩm theo ID",
            description = "API cho phép lấy thông tin chi tiết của sản phẩm dựa trên ID."
    )
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @Operation(
            summary = "Cập nhật thông tin sản phẩm",
            description = "API cho phép cập nhật thông tin sản phẩm."
    )
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @Operation(
            summary = "Xóa sản phẩm theo ID",
            description = "API cho phép xóa sản phẩm theo ID."
    )
    @DeleteMapping("/{id}")
    public void deleteProductById(@PathVariable Long id) {
        productService.deleteProductById(id);
    }
}
