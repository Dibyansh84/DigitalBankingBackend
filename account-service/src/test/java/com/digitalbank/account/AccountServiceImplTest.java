package com.digitalbank.account;

import com.digitalbank.account.dto.AccountCreateRequestDto;
import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.entity.Account;
import com.digitalbank.account.repository.AccountJdbcRepository;
import com.digitalbank.account.repository.AccountJpaRepository;
import com.digitalbank.account.serviceImpl.AccountServiceImpl;
import com.digitalbank.account.util.AccountNumberGenerator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest
{
    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private AccountJdbcRepository accountJdbcRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    @BeforeEach
    void setup()
    {
        //Mockito initializes and injects mocks automatically
    }

    @Test
    void testAccountServiceInitialization()
    {
        //Verify that the test class and mocks initializes successfully.
        assertNotNull(accountServiceImpl);
    }

    //Test 1: Account Creation test
    @Test
    void testCreateAccountSuccessfully()
    {
        //Arrange: build the request DTO that the client would send to create a savings account
        //with an initial deposit of 1000.00 for customer 2
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Build the entity we expect the repository to return after saving.
        //This simulates what the database would produce: an ID assigned and the status is ACTIVE.
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("123456789012");
        account.setCustomerId(2L);
        account.setAccountType("SAVINGS");
        account.setBalance(new BigDecimal("1000.00"));
        account.setAccountStatus("ACTIVE");

        //Stub the account number generator so the service always gets a predictable number.
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("123456789012");

        // Stub the uniqueness check: the generated number does not exist yet, so creation can proceed.
        when(accountJpaRepository.existsByAccountNumber("123456789012")).thenReturn(false);

        //Stub the save call: any account passed in returns the pre-built entity above.
        when(accountJpaRepository.saveAndFlush(any(Account.class))).thenReturn(account);

        //Act: call the service method under test
        AccountResponseDto responseDto = accountServiceImpl.createAccount(requestDto);

        //Assert: verify the response is not null and every field maps correctly from the saved entity.
        assertNotNull(responseDto);
        assertEquals(1L, responseDto.getId());
        assertEquals("123456789012", responseDto.getAccountNumber());
        assertEquals(2L, responseDto.getCustomerId());
        assertEquals("SAVINGS", responseDto.getAccountType());
        assertEquals(new BigDecimal("1000.00"), responseDto.getBalance());
        assertEquals("ACTIVE", responseDto.getAccountStatus());

        //Verify that the account was saved exactly once.
        verify(accountJpaRepository, times(1))
                .saveAndFlush(any(Account.class));

        // Verify that the entity was refreshed from the database exactly once,
        // ensuring the returned data reflects the persisted state.
        verify(entityManager, times(1)).refresh(any(Account.class));
    }

    //Test 2: Test account number generation failure scenario

    @Test
    void testCreateAccountFailsWhenUniqueAccountNumberCannotBeGenerated()
    {
        //Arrange: create a request DTO for a savings account with 1000.00 as the starting money, for customer 2.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Make the number generator always give back the same number
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("123456789012");

        //Say that this number is already used by another account
        when(accountJpaRepository.existsByAccountNumber("123456789012")).thenReturn(true);

        //Act and assert: creating the account should fail with an error,
        // because the service cannot find a number that this is not yet.
        assertThrows(IllegalArgumentException.class, () -> accountServiceImpl.createAccount(requestDto));

        //Check that the service tried to make a number 5 times and then stopped.
        verify(accountNumberGenerator, times(5)).generateAccountNumber();

        //Check that nothing was saved to the database
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));
    }
}