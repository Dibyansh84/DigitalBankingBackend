package com.digitalbank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountPageResponseDto
{
    private List<AccountResponseDto> accounts;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
