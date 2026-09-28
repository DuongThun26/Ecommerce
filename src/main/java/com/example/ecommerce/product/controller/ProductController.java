package com.example.ecommerce.product.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.product.converter.ProductSearchConverter;
import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.response.ProductImportResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import com.example.ecommerce.product.service.ProductImportService;
import com.example.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
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
    private final ProductImportService productImportService;
    private final ProductSearchConverter productSearchConverter;

    @Operation(
            summary = "Tạo sản phẩm mới",
            description = "API cho phép tạo mới sản phẩm với thông tin được cung cấp."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody @Valid ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", product));
    }

    @Operation(
            summary = "Import sản phẩm từ file CSV",
            description = "API import sản phẩm bằng Spring Batch và trả về tổng số bản ghi, số thành công, số thất bại."
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Nhận file CSV và trả về kết quả import theo từng record.
    public ResponseEntity<ApiResponse<ProductImportResponse>> importProducts(@RequestParam("file") MultipartFile file) {
        ProductImportResponse products = productImportService.importProducts(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Import sản phẩm thành công", products));
    }

    @Operation(
            summary = "Lấy danh sách sản phẩm với tìm kiếm và phân trang",
            description = "API cho phép lấy danh sách sản phẩm với các điều kiện tìm kiếm tùy chọn (keyword, status, giá) và hỗ trợ phân trang, sorting. " +
                    "Ví dụ: /products?keyword=iPhone&status=AVAILABLE&minPrice=1000000&maxPrice=50000000&page=0&size=20&sortBy=price,asc"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseSummary>>> getProducts(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable effectivePageable = resolvePageable(params, sortByParams, pageable);
        var searchRequest = productSearchConverter.toProductSearchRequest(params);
        PageResponse<ProductResponseSummary> products = productService.getProducts(searchRequest, effectivePageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
    }

    @Operation(
            summary = "Lấy thông tin sản phẩm theo ID",
            description = "API cho phép lấy thông tin chi tiết của sản phẩm dựa trên ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
    }

    @Operation(
            summary = "Cập nhật thông tin sản phẩm",
            description = "API cho phép cập nhật thông tin sản phẩm."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật thông tin sản phẩm thành công", product));
    }

    @Operation(
            summary = "Xóa sản phẩm theo ID",
            description = "API cho phép xóa sản phẩm theo ID."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProductById(@PathVariable Long id) {
        productService.deleteProductById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    private Pageable resolvePageable(Map<String, Object> params, List<String> sortByParams, Pageable defaultPageable) {
        int page = defaultPageable.getPageNumber();
        int size = defaultPageable.getPageSize();

        if (params != null) {
            if (params.get("page") != null) {
                try {
                    page = Integer.parseInt(params.get("page").toString().trim());
                } catch (NumberFormatException ignored) {}
            }
            if (params.get("size") != null) {
                try {
                    size = Integer.parseInt(params.get("size").toString().trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        List<Sort.Order> orders = new ArrayList<>();

        List<String> sortStrings = new ArrayList<>();
        if (sortByParams != null && !sortByParams.isEmpty()) {
            sortStrings.addAll(sortByParams);
        } else if (params != null && params.get("sortBy") != null) {
            sortStrings.add(params.get("sortBy").toString());
        } else if (params != null && params.get("sort") != null) {
            sortStrings.add(params.get("sort").toString());
        }

        for (String sortStr : sortStrings) {
            if (sortStr == null || sortStr.isBlank()) continue;
            String[] tokens = sortStr.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String field = tokens[i].trim();
                if (field.isEmpty()) continue;
                String direction = "ASC";
                if (i + 1 < tokens.length && (tokens[i + 1].equalsIgnoreCase("asc") || tokens[i + 1].equalsIgnoreCase("desc"))) {
                    direction = tokens[i + 1].toUpperCase();
                    i++;
                }

                String property = resolveSortProperty(field);
                if (property != null) {
                    orders.add("DESC".equals(direction) ? Sort.Order.desc(property) : Sort.Order.asc(property));
                }
            }
        }

        if (orders.isEmpty()) {
            return PageRequest.of(page, size, defaultPageable.getSort());
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private String resolveSortProperty(String field) {
        if ("price".equalsIgnoreCase(field)) return "price";
        if ("name".equalsIgnoreCase(field)) return "name";
        if ("stock".equalsIgnoreCase(field)) return "stock";
        if ("status".equalsIgnoreCase(field)) return "status";
        if ("id".equalsIgnoreCase(field)) return "id";
        return null;
    }
}
