package com.example.ecommerce.customer.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
@Tag(
        name = "Customer",
        description = "APIs cho việc quản lý khách hàng."
)
public class CustomerController {
    private final CustomerService customerService;

    @Operation(
            summary = "Tạo khách hàng mới",
            description = "API cho phép tạo mới khách hàng với thông tin được cung cấp."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@RequestBody @Valid CustomerRequest request){
        CustomerResponse customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khách hàng được tạo thành công", customer));
    }

    @Operation(
            summary = "Lấy danh sách khách hàng",
            description = "API cho phép lấy danh sách khách hàng."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponseSummary>>> getAllCustomers(){
        List<CustomerResponseSummary> customers = customerService.getAllCustomers();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách khách hàng thành công", customers));
    }

    @Operation(
            summary = "Lấy thông tin khách hàng theo ID",
            description = "API cho phép lấy thông tin chi tiết của khách hàng dựa trên ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id){
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin khách hàng thành công", customer));
    }

    @Operation(
            summary = "Cập nhật thông tin khách hàng",
            description = "API cho phép cập nhật thông tin khách hàng."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable Long id, @RequestBody @Valid CustomerRequest request) {
        CustomerResponse customer = customerService.updateCustomer(id, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật thông tin khách hàng thành công", customer));
    }

    @Operation(
            summary = "Xóa khách hàng theo ID",
            description = "API cho phép xóa khách hàng theo ID."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerById(@PathVariable Long id) {
        customerService.deleteCustomerById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Xóa khách hàng thành công", null));
    }
}
