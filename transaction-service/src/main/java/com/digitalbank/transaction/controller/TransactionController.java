package com.digitalbank.transaction.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController
{
    @PostMapping("/test")
    public String testTransactionService()
    {
        return "Transaction Service is working!";
    }
}
