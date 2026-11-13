package com.digitalbank.account.serviceImpl;

import com.digitalbank.account.dto.AccountCreateRequestDto;
import com.digitalbank.account.dto.AccountPageResponseDto;
import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.dto.AccountUpdateRequestDto;
import com.digitalbank.account.entity.Account;
import com.digitalbank.account.exception.AccountNotFoundException;
import com.digitalbank.account.repository.AccountJdbcRepository;
import com.digitalbank.account.repository.AccountJpaRepository;
import com.digitalbank.account.service.AccountService;
import com.digitalbank.account.util.AccountNumberGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountJpaRepository accountJpaRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AccountJdbcRepository accountJdbcRepository;

    //Constructor based dependency injection.
    public AccountServiceImpl(AccountJpaRepository accountJpaRepository, AccountNumberGenerator accountNumberGenerator,
                              AccountJdbcRepository accountJdbcRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.accountJdbcRepository = accountJdbcRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    @Transactional
    public AccountResponseDto createAccount(AccountCreateRequestDto requestDto) {
        /*Generate a unique account number.
        Retry a bounded number of times in case the generator produces a
        number that already exists in the database.*/

        String accountNumber = null;

        for (int attempt = 1; attempt <= 5; attempt++) {
            String generatedNumber = accountNumberGenerator.generateAccountNumber();

            //Check existence before insert.
            /*Check whether the generated account number is already in the database,
             * and if it isn't, keeps it as the account number and exits the retry loop.*/
            if (!accountJpaRepository.existsByAccountNumber(generatedNumber)) {
                accountNumber = generatedNumber;
                break;
            }
        }

        //If none of the 5 attempts produced an unused number, the code stops the account creation with an error.
        if (accountNumber == null) {
            throw new IllegalArgumentException("Unable to generate a unique account number");
        }

        //Create an account entity
        Account account = new Account();

        account.setAccountNumber(accountNumber);
        account.setCustomerId(requestDto.getCustomerId());
        account.setAccountType(requestDto.getAccountType());
        account.setBalance(requestDto.getInitialDeposit());
        account.setAccountStatus("ACTIVE");

        //Save account using JPA.
//            Account savedAccount = accountJpaRepository.save(account);

//        PostgreSQL generates the timestamp, but JPA may not
//        automatically populate the generated value in the entity immediately after save() for e.g: createdAt.
        //explicitly refresh the entity after saving.
        //So save and flush
        Account savedAccount = accountJpaRepository.saveAndFlush(account);
        entityManager.refresh(savedAccount);

        //Convert entity into Response DTO
        AccountResponseDto responseDto = new AccountResponseDto();

        responseDto.setId(savedAccount.getId());
        responseDto.setAccountNumber(savedAccount.getAccountNumber());
        responseDto.setCustomerId(savedAccount.getCustomerId());
        responseDto.setAccountType(savedAccount.getAccountType());
        responseDto.setBalance(savedAccount.getBalance());
        responseDto.setAccountStatus(savedAccount.getAccountStatus());
        responseDto.setCreatedAt(savedAccount.getCreatedAt());

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountById(Long accountId) {
        return accountJdbcRepository.findById(accountId);
    }

    @Override
    @Transactional(readOnly = true) // This method only reads data, it does not change anything in the database.
    public AccountPageResponseDto getAllAccounts(int page, int size) {
        // Page number starts from 0, so negative numbers are not allowed.
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        // Size means "how many accounts per page".
        // It must be at least 1 and not more than 100 (to avoid loading too much data at once).
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        // Offset = how many records to skip before starting
        // Example: page 2, size 10 -> skip first 20 records
        int offset = page * size;

        // Get only the accounts for this page from the database.
        List<AccountResponseDto> accounts = accountJdbcRepository.findAll(size, offset);

        // Get the total number of accounts in the database (all pages together)
        long totalElements = accountJdbcRepository.countAll();

        // Calculate how many pages are needed in total
        // Example: 25 accounts, size 10 -> 2.5 -> rounded up to 3 pages
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Check if this is the last page
        // Pages start at 0, so the last page number is (totalPages - 1)
        boolean last = page >= totalPages - 1;

        // Pack everything into one response object and send it back
        return new AccountPageResponseDto(accounts, page, size, totalElements, totalPages, last);
    }

    @Override
    public void updateAccount(Long accountId, AccountUpdateRequestDto requestDto)
    {

        //Once the account is already CLOSED, its account type or status should not be modified through the general update API.

        //Find an account.
        AccountResponseDto account = accountJdbcRepository.findById(accountId);

        String currentStatus = account.getAccountStatus();
        String requestedStatus = requestDto.getAccountStatus();

        //Once the account is already CLOSED,  its account type or status should not be modified through the general update API.
        if("CLOSED".equals(currentStatus))
        {
            throw new IllegalArgumentException("Closed account cannot be updated");
        }

        if("CLOSED".equals(requestedStatus))
        {
            throw new IllegalArgumentException("Use the close account operation or close API to close an account");
//            Account closure is a separate business operation with additional balance validation. We shouldn't bypass those rules through the general update API.
        }

        int updatedRows = accountJdbcRepository.updateAccount(accountId, requestDto.getAccountType(),
                requestDto.getAccountStatus());

        if (updatedRows == 0) {
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }
    }

    /*Why closeAccount() instead of deleteAccount()?
In banking applications, account closure is a business operation, not ordinary record deletion.
We preserve the account record and its history for audit and financial reconciliation.*/

    @Override
    @Transactional
    public void closeAccount(Long accountId) {
        // Fetch the account to check its current status
        AccountResponseDto account = accountJdbcRepository.findById(accountId);

        // If the account is already closed, tell the caller
        if ("CLOSED".equals(account.getAccountStatus()))
        {
            throw new IllegalArgumentException("Account is already closed");
        }
        //Validate Balance Before Closing Account.
        BigDecimal balance = accountJdbcRepository.findBalanceById(accountId);

        if (balance.compareTo(BigDecimal.ZERO) > 0)
        {
            throw new IllegalArgumentException("Account cannot be closed because it has an outstanding balance");
        }
        accountJdbcRepository.closeAccount(accountId);
    }
}
