package com.digitalbank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDto {
    private Long id;
    private String accountNumber;
    private Long customerId;
    private String accountType;
    private BigDecimal balance;
    private String accountStatus;
    private OffsetDateTime createdAt;
}
