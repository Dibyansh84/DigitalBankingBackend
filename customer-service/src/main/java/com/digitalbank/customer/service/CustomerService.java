package com.digitalbank.customer.service;

import com.digitalbank.customer.dto.*;
import com.digitalbank.customer.entity.Customer;

import java.util.List;

public interface CustomerService
{
    CustomerResponseDto createCustomer(CustomerCreateRequestDto request);

    List<CustomerResponseDto> findAllCustomers();

    CustomerResponseDto findCustomerById(Long customerId);

    CustomerPageResponseDto getCustomersPage(CustomerPageRequestDto requestDto);

    CustomerResponseDto updateCustomer(Long customerId, CustomerUpdateRequestDto requestDto);

    void deleteCustomer(Long customerId);
}
