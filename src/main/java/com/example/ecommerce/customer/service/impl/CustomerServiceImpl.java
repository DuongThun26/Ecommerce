package com.example.ecommerce.customer.service.impl;

import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.mapper.CustomerMapper;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.common.exception.ResourceInUseException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if(customerRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("Customer with email " + request.getEmail() + " already exists.");
        }
        CustomerEntity customer = customerMapper.toEntity(request);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setDeleted(false);
        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    public List<CustomerResponseSummary> getAllCustomers() {
        List<CustomerEntity> customers = customerRepository.findAllByDeletedFalse();
        return customerMapper.toResponseSummaryList(customers);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
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
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        if (orderRepository.existsByCustomerId(id)) {
            throw new ResourceInUseException("Customer with id " + id + " already has orders and cannot be deleted.");
        }
        customer.setDeleted(true);
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);
    }
}
