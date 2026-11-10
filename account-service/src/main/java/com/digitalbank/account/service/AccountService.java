package com.digitalbank.account.service;

import com.digitalbank.account.dto.AccountCreateRequestDto;
import com.digitalbank.account.dto.AccountPageResponseDto;
import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.dto.AccountUpdateRequestDto;

public interface AccountService
{
    AccountResponseDto createAccount(AccountCreateRequestDto requestDto);

    AccountResponseDto getAccountById(Long accountId);

    AccountPageResponseDto getAllAccounts(int page, int size);

    void updateAccount(Long accountId, AccountUpdateRequestDto requestDto);

    void closeAccount(Long accountId);
}
