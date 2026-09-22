package com.example.ecommerce.controller;

import com.example.ecommerce.product.converter.ProductSearchConverter;
import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import com.example.ecommerce.product.controller.ProductController;
import com.example.ecommerce.product.service.ProductImportService;
import com.example.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductImportService productImportService;

    @MockBean
    private ProductSearchConverter productSearchConverter;

    @Test
    @DisplayName("POST /products should return wrapped product")
    void createProductShouldReturnWrappedData() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("IPhone");
        request.setPrice(BigDecimal.valueOf(1000));
        request.setStock(5);

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("IPhone")
                .price(BigDecimal.valueOf(1000))
                .stock(5)
                .build();
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products/{id} should return wrapped data")
    void getProductByIdShouldReturnWrappedData() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(2L)
                .name("MacBook")
                .price(BigDecimal.valueOf(2500))
                .stock(3)
                .build();
        when(productService.getProductById(2L)).thenReturn(response);

        mockMvc.perform(get("/products/2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products should return wrapped page")
    void getProductsShouldReturnWrappedPage() throws Exception {
        ProductSearchRequest searchRequest = new ProductSearchRequest();
        when(productSearchConverter.toProductSearchRequest(any())).thenReturn(searchRequest);

        PageResponse<ProductResponseSummary> page = PageResponse.<ProductResponseSummary>builder()
                .content(List.of(ProductResponseSummary.builder().id(1L).name("P1").price(BigDecimal.TEN).build()))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();
        when(productService.getProducts(any(ProductSearchRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }
}
