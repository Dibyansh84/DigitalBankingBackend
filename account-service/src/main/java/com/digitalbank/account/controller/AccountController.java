package com.digitalbank.account.controller;

import com.digitalbank.account.dto.AccountCreateRequestDto;
import com.digitalbank.account.dto.AccountPageResponseDto;
import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.dto.AccountUpdateRequestDto;
import com.digitalbank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/createAccount")
    public ResponseEntity<AccountResponseDto> createAccount(@Valid @RequestBody AccountCreateRequestDto requestDto) {
        AccountResponseDto responseDto = accountService.createAccount(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/getAccountById")
    public ResponseEntity<AccountResponseDto> getAccountById(@RequestParam("accountId") Long accountId) {
        AccountResponseDto response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/getAllAccounts")
    public ResponseEntity<AccountPageResponseDto> getAllAccounts(@RequestParam int page, @RequestParam int size) {
        AccountPageResponseDto response = accountService.getAllAccounts(page, size);
        return ResponseEntity.ok(response);
    }

    //Update operation endpoint

    @PostMapping("/updateAccount")
    public ResponseEntity<String> updateAccount(@RequestParam("accountId") Long accountId,
                                                @Valid @RequestBody AccountUpdateRequestDto requestDto) {
        accountService.updateAccount(accountId, requestDto);

        return ResponseEntity.ok("Account updated successfully");
    }

    //Close Account API(It's a soft-delete)
    @PostMapping("/closeAccount")
    public ResponseEntity<String> closeAccount(@RequestParam("accountId") Long accountId)
    {
        accountService.closeAccount(accountId);
        return ResponseEntity.ok("Account closed successfully");
    }
}
