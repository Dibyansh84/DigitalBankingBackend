package com.digitalbank.transaction.controller;

import com.digitalbank.transaction.dto.TransactionCreateRequestDto;
import com.digitalbank.transaction.dto.TransactionResponseDto;
import com.digitalbank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController
{

    private TransactionService transactionService;

    //Constructor based dependency injection
    public TransactionController(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }
    @PostMapping("/test")
    public String testTransactionService()
    {
        return "Transaction Service is working!";
    }


    //This API creates only a transaction record. No account balance is debited or credited yet.
    @PostMapping("/createTransaction")
    public ResponseEntity<TransactionResponseDto> createTransaction(@RequestBody @Valid TransactionCreateRequestDto requestDto)
    {
        TransactionResponseDto responseDto = transactionService.createTransaction(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }



}
