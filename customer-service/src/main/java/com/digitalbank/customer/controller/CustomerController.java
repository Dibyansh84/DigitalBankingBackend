package com.digitalbank.customer.controller;

import com.digitalbank.customer.dto.*;
import com.digitalbank.customer.entity.Customer;
import com.digitalbank.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController
{
    private final CustomerService customerService;
    public CustomerController(CustomerService customerService)
    {
        this.customerService = customerService;
    }
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testCustomerService()
    {
        return ResponseEntity.ok(Map.of("service", "customer-service", "message",
                "Customer service is working"));
    }

    @PostMapping("/createCustomer")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerCreateRequestDto requestDto)
    {
        CustomerResponseDto responseDto = customerService.createCustomer(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseDto);
    }

    @PostMapping("/getAllCustomers")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers()
    {
        List<CustomerResponseDto> customers = customerService.findAllCustomers();

        return ResponseEntity.ok(customers);
    }

    @PostMapping("/getCustomerById")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@RequestBody Map<String, Long> request)
    {
        Long customerId = request.get("customerId");

       CustomerResponseDto customer = customerService.findCustomerById(customerId);

       if(customer == null)
       {
           return ResponseEntity.notFound().build();
       }

       return ResponseEntity.ok(customer);
    }

    @PostMapping("/page")
    public ResponseEntity<CustomerPageResponseDto> getCustomersPage(@RequestBody(required = false) CustomerPageRequestDto requestDto)
    {
        if(requestDto == null)
        {
            requestDto = new CustomerPageRequestDto();
        }

        CustomerPageResponseDto responseDto = customerService.getCustomersPage(requestDto);

        return ResponseEntity.ok(responseDto);

        //Is endpoint par request body nahi bhejoge, toh DTO ke default values use honge: page 0, size 10, sorting id ASC.
    }

    @PostMapping("/update/{customerId}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(@PathVariable Long customerId, @Valid @RequestBody CustomerUpdateRequestDto requestDto)
    {
        CustomerResponseDto responseDto = customerService.updateCustomer(customerId, requestDto);

        return ResponseEntity.ok(responseDto);
    }

    //Delete customer
    @PostMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteCustomer(@RequestParam Long customerId)
    {
        customerService.deleteCustomer(customerId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Customer deleted successfully");
        response.put("customerId", customerId.toString());

        return ResponseEntity.ok(response);
    }

}
