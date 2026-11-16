package com.digitalbank.account;

import com.digitalbank.account.dto.AccountCreateRequestDto;
import com.digitalbank.account.dto.AccountResponseDto;
import com.digitalbank.account.dto.AccountUpdateRequestDto;
import com.digitalbank.account.entity.Account;
import com.digitalbank.account.exception.AccountNotFoundException;
import com.digitalbank.account.repository.AccountJdbcRepository;
import com.digitalbank.account.repository.AccountJpaRepository;
import com.digitalbank.account.serviceImpl.AccountServiceImpl;
import com.digitalbank.account.util.AccountNumberGenerator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    //Test 3: Test account creation with multiple duplicate attempts.

    @Test
    void testCreateAccountAfterRetryingDuplicateNumbers()
    {
        //Arrange: make a request DTO for a savings account with 1000.00 to start, for customer 2.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Make a fake saved account, like the DB would give back.
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("123456789014");
        account.setCustomerId(2L);
        account.setAccountType("SAVINGS");
        account.setBalance(new BigDecimal("1000.00"));
        account.setAccountStatus("ACTIVE");

        //The generator gives 3 numbers, one after another
        //The first two are already used, and the third one is new.
        when(accountNumberGenerator.generateAccountNumber())
                .thenReturn("123456789012")
                .thenReturn("123456789013")
                .thenReturn("123456789014");

        //Say the first number is already taken
        when(accountJpaRepository.existsByAccountNumber("123456789012")).thenReturn(true);

        //Say the second number is already taken
        when(accountJpaRepository.existsByAccountNumber("123456789013")).thenReturn(true);

        //Say the third number is free to use
        when(accountJpaRepository.saveAndFlush(any(Account.class))).thenReturn(account);

        //Act: create the account. The service should skip the taken numbers.
        AccountResponseDto responseDto = accountServiceImpl.createAccount(requestDto);

        //Assert: we got a response, and it has the free number (the third one)
        assertNotNull(responseDto);
        assertEquals("123456789014", responseDto.getAccountNumber());

        //Check that the generator was called 3 times, once for each try
        verify(accountNumberGenerator, times(3)).generateAccountNumber();

        //Check that the account was saved one time
        verify(accountJpaRepository, times(1)).saveAndFlush(any(Account.class));

        //Check that the saved account was loaded again from the DB one time
        verify(entityManager, times(1)).refresh(any(Account.class));
    }

    //Test 4: Test account creation with invalid input.
    @Test
    void testCreateAccountWithZeroInitialDeposit()
    {
        // Arrange: Prepare account creation request with zero initial deposit
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(BigDecimal.ZERO);

        // Act & Assert: Verify that the service throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                ()-> accountServiceImpl.createAccount(requestDto));

        // Verify: Ensure that no account is saved in the database
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));
    }

    /* Important: This test assumes your createAccount() service method explicitly validates that the
    initial deposit is greater than zero.
    @Positive on the DTO alone does not run automatically when calling the service directly in a unit test.*/

    //Test 5: Test account creation with negative initial deposit.
    //Zero deposit ki tarah negative deposit bhi reject hona chahiye.

    @Test
    void testCreateAccountWithNegativeInitialDeposit()
    {
        //Arrange: Prepare account creation request with negative initial deposit.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("-500.00"));

        //Act and assert: Verify that the service throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the database.
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));
    }

    //Test 6: test account creation with null initial deposit.
    //Ab check karenge ki initialDeposit=null hone par service account create na kare.

    @Test
    void testCreateAccountWithNullInitialDeposit()
    {
        //Arrange: Prepare account creation request with null initial deposit.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(12L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(null);

        //Act and assert: Verify that the service rejects null initial deposit
        assertThrows(IllegalArgumentException.class, () -> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the database.
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));

        //Important: This test assumes your service explicitly checks for null.
        // @NotNull validation on the DTO won't automatically run during a direct service method call.
    }

    //Test 7: test account creation with null Customer ID.
    //Ab check karenge ki customerId = null hone par account create nahi hona chahiye.

    @Test
    void testCreateAccountWithNullCustomerId()
    {
        //Arrange: Prepare account creation request with null customerId.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(null);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Act & assert: Verify that the service rejects null customer ID.
        assertThrows(IllegalArgumentException.class, () -> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the database
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));
    }

    //Test 8: Test account creation with invalid Account Type
    // Ab check karenge ki invalid account type, jaise FIXED_DEPOSIT, reject hona chahiye.

    @Test
    void testCreateAccountWithInvalidAccountType()
    {
        //Arrange: Prepare account creation request with invalid account type.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("FIXED_DEPOSIT");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Act & assert: verify that the service rejects invalid account type.
        assertThrows(IllegalArgumentException.class, ()-> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the database
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));

        /* Note: This requires explicit account-type validation inside createAccount().
        DTO's @Pattern validation alone won't execute during a direct service unit test.*/
    }

    //Test 9: Test account creation with null Account Type.
    //Ab check karenge ki accountType = null hone par service account creation reject kare.

    @Test
    void testCreateAccountWithNullAccountType()
    {
        //Arrange: Prepare account creation request DTO with null account type.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType(null);
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Act & assert: Verify that the service rejects null account type
        assertThrows(IllegalArgumentException.class,
                ()-> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the DB.
        verify(accountJpaRepository, never())
                .saveAndFlush(any(Account.class));
    }

    //Test 10: Test account creation with invalid Customer ID

    @Test
    void testCreateAccountWithInvalidCustomerId()
    {
        //Arrange: Prepare account creation request with Customer ID as zero
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(0L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Act & assert: Verify that the service rejects invalid customer ID.
        assertThrows(IllegalArgumentException.class,
                ()-> accountServiceImpl.createAccount(requestDto));

        //Verify: Ensure that no account is saved in the DB.
        verify(accountJpaRepository, never()).saveAndFlush(any(Account.class));

        /*
        Expected result: IllegalArgumentException and no account saved.

Note: Iske liye service layer mein customerId positive hone ki explicit validation zaroori hai.
DTO ka @Positive direct service call par automatically execute nahi hota.
        * */
    }

    //Test 11: Account Not Found Unit Test.
    //Ab hum getAccountById() method test karenge.
    //Scenario: Jab account database mein exist nahi karta, repository AccountNotFoundException throw karti hai.
    //Service ko exception properly propagate karna chahiye.

    @Test
    void testGetAccountByIdWhenAccountDoesNotExist()
    {
        //Arrange: Define an account ID that does not exist.
        Long accountId = 999L;

        //Mock: Configure repository to throw AccountNotFoundException.
        when(accountJdbcRepository.findById(accountId))
                .thenThrow(new AccountNotFoundException("Account not found with ID: "+accountId));

        //Act & assert: Verify that service propagates the exception.
        assertThrows(AccountNotFoundException.class, () -> accountServiceImpl.getAccountById(accountId));

        //Verify: Ensure repository method was called exactly once.
        verify(accountJdbcRepository, times(1)).findById(accountId);
    }

    /*------------------------------------------*/
    /*Testing Account Closure Business Rules*/
    /*----------------------------------------*/

    //Test 1: Account already closed
    //This is the first scenario wherein we're going to test the already closed account.

    @Test
    void testCloseAccountWhenAccountIsAlreadyClosed()
    {
        //Arrange: Prepare an account that is already CLOSED.
        Long accountId = 1L;

        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("CLOSED");

        //Mock: Return the already closed account from repository.
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Act and Assert: Verify that closing an already closed account throws exception
        assertThrows(IllegalArgumentException.class,
                ()-> accountServiceImpl.closeAccount(accountId));

        //Verify: Ensure balance is not closed.
        verify(accountJdbcRepository, never()).findBalanceById(accountId);
    }

    //Test 2: Test account closure when balance is greater than zero -- Passed.
    //Scenario: Account "ACTIVE" hai, lekin uska balance Rs.1000 hai jo greater than zero hota hai.
    // Aise account ko close nahi karna chahiye.

    @Test
    void testCloseAccountWhenBalanceIsGreaterThanZero()
    {
        //Arrange: Prepare an active account.
        Long accountId = 1L;
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: Return the active account.
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Mock: Return account balance greater than zero.
        when(accountJdbcRepository.findBalanceById(accountId)).thenReturn(new BigDecimal("1000"));

        //Act & Assert: Verify that account closure is rejected.
        assertThrows(IllegalArgumentException.class, () -> accountServiceImpl.closeAccount(accountId));

        //Verify: Ensure account details are fetched.
        verify(accountJdbcRepository, times(1)).findBalanceById(accountId);

        //Verify: Ensure account is not closed.
        verify(accountJdbcRepository, never()).closeAccount(accountId);

        /*
    Expected behaviour:
    - Account details fetched.
    - Balance checked.
    - IllegalArgumentException thrown.
    - closeAccount() repository method never called.
        * */
    }


    //Test 3: Test successful account closure when balance is zero
    //Scenario: Account ACTIVE hai aur balance ₹0.00 hai. Is case mein account successfully close hona chahiye.

    @Test
    void testCloseAccountSuccessfullyWhenBalanceIsZero()
    {
        //Arrange: Prepare an active account
        Long accountId = 1L;

        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: Return the active account
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Mock: Return Zero account balance
        when(accountJdbcRepository.findBalanceById(accountId)).thenReturn(BigDecimal.ZERO);

        //Act: Close the account
        accountServiceImpl.closeAccount(accountId);

        //Verify: Ensure account details are fetched
        verify(accountJdbcRepository, times(1)).findById(accountId);

        //Verify: Ensure account balance is checked
        verify(accountJdbcRepository, times(1)).findBalanceById(accountId);

        //Verify: Ensure account closure is executed exactly once.
        verify(accountJdbcRepository, times(1)).closeAccount(accountId);

        /*
        Expected behaviour:
    - Account details fetched successfully.
    - Balance is verified as zero.
    - No exception is thrown.
    - Repository's closeAccount() is called exactly once.
        * */
    }

    /*----------------------*/
    /*Testing Update API*/
    /*---------------------*/
    //Test 1: Test updating an already CLOSED account.
    //Scenario: Account already CLOSED hai. Uss account ko update karne ki permission nahi honi chahiye.

    @Test
    void testUpdateAccountWhenAccountIsAlreadyClosed()
    {
        //Arrange: Prepare an account ID and update request.
        Long accountId = 1L;

        AccountUpdateRequestDto requestDto = new AccountUpdateRequestDto();
        requestDto.setAccountType("SAVINGS");
        requestDto.setAccountStatus("ACTIVE");

        //Arrange: Prepare an account that is already closed.
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("CLOSED");

        //Mock: Return the closed account from repository.
        when(accountJdbcRepository.findById(accountId))
                .thenReturn(accountResponseDto);
        
        //Act & Assert: Verify that updating a closed account throws exception.
        assertThrows(IllegalArgumentException.class,
                ()-> accountServiceImpl.updateAccount(accountId, requestDto));

        //Verify: Ensure account details are fetched exactly once.
        verify(accountJdbcRepository, times(1)).findById(accountId);

        //Verify: Ensure account update is never executed.
        verify(accountJdbcRepository, never()).updateAccount(anyLong(), anyString(), anyString());
    }

    //Test updating account with requested status 'CLOSED'.
    //Scenario: Account currently 'ACTIVE' hai, lekin update request DTO mein status 'CLOSED' diya gaya hai.
    /*Hamare business rule ke according, account close karne ke liye dedicated closeAccount() operation use
    karna chahiye, updateAccount() nahi.
     */

    @Test
    void testUpdateAccountWhenRequestedStatusIsClosed()
    {
        //Arrange: Prepare accountId and update request DTO.
        Long accountId = 1L;

        AccountUpdateRequestDto requestDto = new AccountUpdateRequestDto();
        requestDto.setAccountType("SAVINGS");
        requestDto.setAccountStatus("CLOSED");

        //Arrange: Prepare an account with ACTIVE status
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: Return the active account from repository.
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Act & Assert: Verify that CLOSED status is registered by update operation.
        assertThrows(IllegalArgumentException.class, ()-> accountServiceImpl.updateAccount(accountId, requestDto));

        //Verify: Ensure account details are fetched only once.
        verify(accountJdbcRepository, times(1)).findById(accountId);

        //Verify: Ensure update query is never executed.
        verify(accountJdbcRepository, never()).updateAccount(anyLong(), anyString(), anyString());
    }

    //Test successful account update.
    //Scenario: Account currently active hai. Hum uska status BLOCKED karna chahte hein.
    /*
    * Expected Behaviour:
    * -- Account details successfully fetched.
    * -- Account CLOSED nahi hai.
    * -- Requested status CLOSED nahi hai.
    * -- Repository update method exactly once execute hota hai.
    * -- Koi exception nahi aata.
    * */

    @Test
    void testUpdateAccountSuccessfully()
    {
        //Arrange: Prepare account ID.
        Long accountId = 1L;

        //Arrange: Prepare update request.
        AccountUpdateRequestDto requestDto = new AccountUpdateRequestDto();
        requestDto.setAccountStatus("BLOCKED");
        requestDto.setAccountType("SAVINGS");

        //Arrange: Prepare an existing ACTIVE account
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountType("SAVINGS");
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: Return the existing account from repository
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Mock: Simulate successful database update
        when(accountJdbcRepository.updateAccount(accountId, "SAVINGS", "BLOCKED"))
                .thenReturn(1);

        //Act: Execute account update
        assertDoesNotThrow(()-> accountServiceImpl.updateAccount(accountId, requestDto));

        //Verify: Ensure account details are fetched exactly once.
        verify(accountJdbcRepository, times(1)).findById(accountId);

        //Verify: Ensure account update is executed exactly once.
        verify(accountJdbcRepository, times(1)).updateAccount(accountId, "SAVINGS", "BLOCKED");
    }

    /*ArgumentCaptor lets us inspect the actual argument passed from our service to a dependency.
    This is especially useful when testing DTO-to-entity mapping and ensuring important banking values
    are not accidentally changed.*/
    //By using ArgumentCaptor, instead of only checking that saveAndFlush() was called, we can capture the actual object.
    @Test
    void testCreateAccountWithArgumentCaptor()
    {
        // Arrange: make the request that the user would send to open a savings account.
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setCustomerId(2L);
        requestDto.setAccountType("SAVINGS");
        requestDto.setInitialDeposit(new BigDecimal("1000.00"));

        //Arrange: Make a fake account, like the DB would return it after saving it.
        Account savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setAccountNumber("123456789012");
        savedAccount.setCustomerId(2L);
        savedAccount.setAccountType("SAVINGS");
        savedAccount.setBalance(new BigDecimal("1000.00"));
        savedAccount.setAccountStatus("ACTIVE");

        //Mock: Tell the number generator to give us this number.
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("123456789012");

        //Mock: Say this number is not used by anyone else yet.
        when(accountJpaRepository.existsByAccountNumber("123456789012")).thenReturn(false);

        //Mock: when the service saves any account, give back the fake account we made above.
        when(accountJpaRepository.saveAndFlush(any(Account.class))).thenReturn(savedAccount);

        //Act: run the create account method
        AccountResponseDto accountResponseDto = accountServiceImpl.createAccount(requestDto);

        //Assert: check that we got a response and it has the right account number.
        assertNotNull(accountResponseDto);
        assertEquals("123456789012", accountResponseDto.getAccountNumber());

        //Capture: a "catcher" that will grab the account the service sent to the database.
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //Verify: Check that save was called once, and catch the account that was sent to it.
        verify(accountJpaRepository).saveAndFlush(accountArgumentCaptor.capture());

        //Take the caught account out so we can look inside it.
        Account captureAccount = accountArgumentCaptor.getValue();

        //Assert: Check that the account sent to the database has the right details.
        assertEquals(2L, captureAccount.getCustomerId());
        assertEquals("SAVINGS", captureAccount.getAccountType());
        assertEquals(new BigDecimal("1000.00"), captureAccount.getBalance());
        assertEquals("ACTIVE", captureAccount.getAccountStatus());
        assertEquals("123456789012", captureAccount.getAccountNumber());

        //Verify: Check that the saved account was loaded again from the DB.
        verify(entityManager).refresh(savedAccount);

    }

    //Next step: Mockito doThrow()
    //We've used when(...).thenThrow(...) for methods that return values. But what about the method that returns void?
    //Suppose a repository method throws an exception while closing an account.
    // So we can simulate that failure using doThrow().

    //Test: Repository failure during account closure.

    @Test
    void testCloseAccountWhenRepositoryThrowsException()
    {
        //Arrange: Prepare an active account with zero balance.
        Long accountId = 1L;

        //Make a fake account that is currently "ACTIVE".
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: When the service looks up by the account ID, return the fake account above.
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Mock: When the service checks the balance, say the balance is zero.
        when(accountJdbcRepository.findBalanceById(accountId)).thenReturn(BigDecimal.ZERO);

        //When the service tries to close the account in the DB, make it fail with an error.
        doThrow(new RuntimeException("Database update failed")).when(accountJdbcRepository).closeAccount(accountId);

        //Act & Assert: Run the close method & check that the same error comes back out.
        RuntimeException exception = assertThrows(RuntimeException.class,
                ()-> accountServiceImpl.closeAccount(accountId));

        //Check that the error message is the same one we set up above.
        assertEquals("Database update failed", exception.getMessage());

        //Verify: Check that the service did all of its steps in the right order.
        //including trying to close the account in the DB.
        verify(accountJdbcRepository).findById(accountId);
        verify(accountJdbcRepository).findBalanceById(accountId);
        verify(accountJdbcRepository).closeAccount(accountId);

        /* This test matters because it verifies that a database failure is not silently ignored by the service.*/
    }

    //Next step: InOrder in Mockito
    /*In banking applications, the order of operations can matter. InOrder lets us verify that methods were called
    in a specific sequence.*/
    /*For account closure, our service should:
    * 1. Fetch account details
    * 2. Check the account balance.
    * 3. Close the account.*/

    @Test
    void testCloseAccountOperationsAreExecutedInOrder()
    {
        //Arrange: Prepare an active account with zero balance.
        Long accountId = 1L;

        // Make a fake account that is currently ACTIVE
        AccountResponseDto accountResponseDto = new AccountResponseDto();
        accountResponseDto.setId(accountId);
        accountResponseDto.setAccountStatus("ACTIVE");

        //Mock: When the service looks up the account by ID, return the fake account above.
        when(accountJdbcRepository.findById(accountId)).thenReturn(accountResponseDto);

        //Mock: When the service checks the balance, say the balance is zero.
        when(accountJdbcRepository.findBalanceById(accountId)).thenReturn(BigDecimal.ZERO);

        //Act: Close the account
        accountServiceImpl.closeAccount(accountId);

        //Verify: Check that the steps happened in this exact order:
        //first find the account, then check the balance, then close it.

        InOrder inOrder = inOrder(accountJdbcRepository);

        //Step 1: the account was looked up first
        inOrder.verify(accountJdbcRepository).findById(accountId);

        //Step 2: the balance was checked second
        inOrder.verify(accountJdbcRepository).findBalanceById(accountId);

        //Step 3: the account was closed.
        inOrder.verify(accountJdbcRepository).closeAccount(accountId);

/*One important distinction: this test verifies the order of calls in Java.
It does not prove that account closure is concurrency-safe at the database level;
we’ll address that separately when we work on transaction reliability.*/

    }


}