package com.digitalbank.customer.serviceImpl;

import com.digitalbank.customer.dto.*;
import com.digitalbank.customer.entity.Customer;
import com.digitalbank.customer.exception.CustomerNotFoundException;
import com.digitalbank.customer.exception.DuplicateEmailException;
import com.digitalbank.customer.repository.CustomerJdbcRepository;
import com.digitalbank.customer.repository.CustomerJpaRepository;
import com.digitalbank.customer.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService
{

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerJdbcRepository customerJdbcRepository;

    public CustomerServiceImpl(CustomerJpaRepository customerJpaRepository, CustomerJdbcRepository customerJdbcRepository)
    {
        this.customerJpaRepository = customerJpaRepository;
        this.customerJdbcRepository = customerJdbcRepository;
    }

    @Override
    public CustomerResponseDto createCustomer(CustomerCreateRequestDto request)
    {

        if(customerJpaRepository.existsByEmail(request.getEmail()))
        {
            throw new DuplicateEmailException("A customer with this email already exists");
        }
        //DTO to entity mapping
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        //Save using JPA
        Customer savedCustomer = customerJpaRepository.save(customer);

        //Entity to Response DTO mapping
        CustomerResponseDto response = new CustomerResponseDto();
        response.setId(savedCustomer.getId());
        response.setFullName(savedCustomer.getFullName());
        response.setEmail(savedCustomer.getEmail());
        response.setPhone(savedCustomer.getPhone());

        //Return the response dto
        return response;
    }

    @Override
    public List<CustomerResponseDto> findAllCustomers()
    {
        // Ask the repository for every Customer row in the database.
        // The result is a plain list of entity objects that map directly to the table.
        List<Customer> customers = customerJdbcRepository.findAll();

        // Convert each Customer into a CustomerResponseDTO, the shape the caller or API receives.
        return customers.stream()
        // For each customer, build a new CustomerResponse and copy the fields across.
                .map(customer ->{
                    CustomerResponseDto response = new CustomerResponseDto(); // Empty response object for this customer
                    response.setId(customer.getId());
                    response.setFullName(customer.getFullName());
                    response.setEmail(customer.getEmail());
                    response.setPhone(customer.getPhone());
                    return response;
                })
        //Collect the stream into an unmodifiable list.
                .toList();
    }

    @Override
    public CustomerResponseDto findCustomerById(Long customerId)
    {
        Customer customer = customerJdbcRepository.findById(customerId);

        if(customer == null)
        {
            throw new CustomerNotFoundException("Customer not found with ID: "+customerId);
        }

        //Entity to DTO mapping
        CustomerResponseDto responseDto = new CustomerResponseDto();
        responseDto.setId(customer.getId());
        responseDto.setFullName(customer.getFullName());
        responseDto.setEmail(customer.getEmail());
        responseDto.setPhone(customer.getPhone());

        return responseDto;
    }

    @Override
    public CustomerPageResponseDto getCustomersPage(CustomerPageRequestDto requestDto)
    {
        //Get the customers for this page from the database
        List<Customer> customers = customerJdbcRepository.findPage(
                requestDto.getPage(), requestDto.getSize(), requestDto.getSortBy(),
                requestDto.getSortDirection()
        );

        // Count all customers in the table, so we know how many pages there are
        long totalRecords = customerJdbcRepository.countAll();

        // Work out the total number of pages.
        // Divide total records by page size and round up.
        // Example: 25 records with size 10 gives 3 pages.
        int totalPages = (int) Math.ceil((double) totalRecords / requestDto.getSize());

        // Convert each Customer(entity data) into a CustomerResponse(DTOs).
        // This hides internal data and sends only what the client needs.
        List<CustomerResponseDto> customerResponses = customers.stream()
                .map(customer -> {
                    CustomerResponseDto responseDto = new CustomerResponseDto();
                    responseDto.setId(customer.getId());
                    responseDto.setFullName(customer.getFullName());
                    responseDto.setEmail(customer.getEmail());
                    responseDto.setPhone(customer.getPhone());
                    return responseDto;
                }).toList();


        // Check if this is the first page (page numbers start at 0)
        boolean firstPage = requestDto.getPage() ==0;


        // Check if this is the last page.
        // It is also the last page when there are no records at all.
        boolean lastPage = totalPages == 0 || requestDto.getPage() >= totalPages -1;

        // Build the response with the customers and the page details
        //Complete pagination information is returned in a response DTO.
        return new CustomerPageResponseDto(customerResponses, requestDto.getPage(), requestDto.getSize(),
                totalRecords, totalPages, firstPage, lastPage);
    }

    @Override
    public CustomerResponseDto updateCustomer(Long customerId, CustomerUpdateRequestDto requestDto)
    {
        //Check whether the customer exists.
        Customer customer = customerJdbcRepository.findById(customerId);

        if(customer == null)
        {
            throw new CustomerNotFoundException("Customer not found with ID: "+customerId);
        }

        //Check whether another customer already uses this email.
        if(!customer.getEmail().equalsIgnoreCase(requestDto.getEmail())
        && customerJpaRepository.existsByEmail(requestDto.getEmail()))
        {
            //If duplicate email exists
            throw new DuplicateEmailException("A customer with this email already exists");
        }

        //Update the customer using JDBC.
        customerJdbcRepository.updateCustomer(customerId, requestDto.getFullName(), requestDto.getEmail(),
                requestDto.getPhone());

        //Return the updated customer
        CustomerResponseDto responseDto = new CustomerResponseDto();
        responseDto.setId(customerId);
        responseDto.setFullName(requestDto.getFullName());
        responseDto.setEmail(requestDto.getEmail());
        responseDto.setPhone(requestDto.getPhone());

        return responseDto;
    }

    @Override
    public void deleteCustomer(Long customerId) {
        // Look up the customer first to make sure they exist
        Customer customer = customerJdbcRepository.findById(customerId);

        // If no customer was found, stop here and report the error.
        // Nothing is deleted in this case.
        if (customer == null)
        {
            throw new CustomerNotFoundException("Customer not found with ID: "+customerId);
        }

        //The customer exists, so delete it from the database.
        customerJdbcRepository.deleteCustomer(customerId);
    }
}
