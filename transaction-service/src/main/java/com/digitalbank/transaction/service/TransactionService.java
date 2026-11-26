package com.digitalbank.transaction.service;

import com.digitalbank.transaction.dto.TransactionCreateRequestDto;
import com.digitalbank.transaction.dto.TransactionResponseDto;

public interface TransactionService
{
    TransactionResponseDto createTransaction(TransactionCreateRequestDto requestDto);
}
