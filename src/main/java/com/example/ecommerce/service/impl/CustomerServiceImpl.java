package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.CustomerRequest;
import com.example.ecommerce.dto.response.CustomerResponse;
import com.example.ecommerce.dto.response.CustomerResponseSummary;
import com.example.ecommerce.entity.CustomerEntity;
import com.example.ecommerce.enums.CustomerStatus;
import com.example.ecommerce.mapper.CustomerMapper;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CustomerRepository;
import com.example.ecommerce.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if(customerRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("Customer with email " + request.getEmail() + " already exists.");
        }
        CustomerEntity customer = customerMapper.toEntity(request);
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    public List<CustomerResponseSummary> getAllCustomers() {
        List<CustomerEntity> customers = customerRepository.findAll();
        return customerMapper.toResponseSummaryList(customers);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        if(customerRepository.existsByEmailAndIdNot(request.getEmail(), id)){
            throw new DuplicateResourceException("Customer with email " + request.getEmail() + " already exists.");
        }
        customerMapper.updateEntity(request, customer);
        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    public void deleteCustomerById(Long id) {
        // Chưa có logic kiểm tra ràng buộc tồn tại đơn hàng không được xóa
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        customerRepository.delete(customer);
    }
}
