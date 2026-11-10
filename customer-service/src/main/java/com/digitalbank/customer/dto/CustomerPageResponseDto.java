package com.digitalbank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPageResponseDto
{
    private List<CustomerResponseDto> customers;
    private int currentPage;
    private int pageSize;
    private long totalRecords;
    private int totalPages;
    private boolean firstPage;
    private boolean lastPage;
}
