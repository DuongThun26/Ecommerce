package com.example.ecommerce.customer.controller;

import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public CustomerResponse createCustomer(@RequestBody @Valid CustomerRequest request){
        return customerService.createCustomer(request);
    }

    @Operation(
            summary = "Lấy danh sách khách hàng",
            description = "API cho phép lấy danh sách khách hàng."
    )
    @GetMapping
    public List<CustomerResponseSummary> getAllCustomers(){
        return customerService.getAllCustomers();
    }

    @Operation(
            summary = "Lấy thông tin khách hàng theo ID",
            description = "API cho phép lấy thông tin chi tiết của khách hàng dựa trên ID."
    )
    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id){
        return customerService.getCustomerById(id);
    }

    @Operation(
            summary = "Cập nhật thông tin khách hàng",
            description = "API cho phép cập nhật thông tin khách hàng."
    )
    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @RequestBody @Valid CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @Operation(
            summary = "Xóa khách hàng theo ID",
            description = "API cho phép xóa khách hàng theo ID."
    )
    @DeleteMapping("/{id}")
    public void deleteCustomerById(@PathVariable Long id) {
        customerService.deleteCustomerById(id);
    }
}
