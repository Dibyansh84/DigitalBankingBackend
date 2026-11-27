package com.digitalbank.transaction.serviceImpl;

import com.digitalbank.transaction.dto.TransactionCreateRequestDto;
import com.digitalbank.transaction.dto.TransactionResponseDto;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.repository.TransactionJpaRepository;
import com.digitalbank.transaction.utility.TransactionReferenceGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest
{
    //TransactionServiceImplTest is for actual service with mocked dependencies.

    @Mock
    private TransactionJpaRepository transactionJpaRepository;

    @Mock
    private TransactionReferenceGenerator transactionReferenceGenerator;

    @InjectMocks
    private TransactionServiceImpl transactionServiceImpl;

    @Test
    void testTransactionServiceInitialization()
    {
        //@Test	marks a test method

        //Arrange: Mockito initializes the service dependencies.
        //Act: Verify the service instance.
        //Assert: Service should not be null.
        assertNotNull(transactionServiceImpl);
    }

    //test create transaction
    @Test
    void testCreateTransactionSuccessfully()
    {
        //Step 1: Arrange(Prepare the data)
        //Make a request object, like user filling a form.
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send a short note
        requestDto.setAmount(new BigDecimal("500.00"));
        requestDto.setDescription("Test transfer");

        //A fixed reference number we will use in this test
        String transactionReference = "TXN1234567890123";

        //Fake the reference generator
        //when it is asked for a reference, give our fixed one
        when(transactionReferenceGenerator.generateTransactionReference()).thenReturn(transactionReference);

        //Fake the DB check:
        //say "this reference is not used before", so it is safe to use
        when(transactionJpaRepository.existsByTransactionReference(transactionReference))
                .thenReturn(false);

        //Fake the DB save
        // when save is called, set id = 1 and give the same object back
        // (a real database would create the id for us)
        when(transactionJpaRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                   //Take the object that was sent to save
                    Transaction transaction = invocation.getArgument(0);
                    //Set an id like the DB would do
                    transaction.setId(1L);

                    //Return it back
                    return transaction;
                });

        //Step 3: Act (run the real method)
        //Call the method we want to test
        TransactionResponseDto response = transactionServiceImpl.createTransaction(requestDto);

        //Step 3: Assert (Check the result)
        //The response should not be empty
        assertNotNull(response);

        //The id should be the one we set in the fake save
        assertEquals(1L, response.getId());

        //The reference should be our fixed reference
        assertEquals(transactionReference, response.getTransactionReference());

        //Account ids should be the same as in the request
        assertEquals(1L, response.getFromAccountId());
        assertEquals(2L, response.getToAccountId());

        //Amount should not change
        assertEquals(new BigDecimal("500.00"), response.getAmount());

        //Type should be TRANSFER because money moves between accounts
        assertEquals("TRANSFER", response.getTransactionType());

        //A new transaction should start as PENDING
        assertEquals("PENDING", response.getTransactionStatus());

        //Description should be the same as in the request
        assertEquals("Test transfer", response.getDescription());

        //Step 4: VERIFY(Check what was saved)
        //Create a "catcher" to grab the object sent to the database
        ArgumentCaptor<Transaction> transactionArgumentCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        //Check that save was called, and catch the object that was saved
        verify(transactionJpaRepository).saveAndFlush(transactionArgumentCaptor.capture());

        //Take the caught object so we can check it
        Transaction capturedTransaction = transactionArgumentCaptor.getValue();

        //The saved object should have the correct reference
        assertEquals(transactionReference, capturedTransaction.getTransactionReference());

        //The saved object should be "PENDING"
        assertEquals("PENDING", capturedTransaction.getTransactionStatus());

        //The saved object should have the current amount
        assertEquals(new BigDecimal("500.00"), capturedTransaction.getAmount());

        //Reference generator should be called exactly one time
        verify(transactionReferenceGenerator, times(1)).generateTransactionReference();

        //Save should be called exactly one time(not more, not less)
        verify(transactionJpaRepository, times(1)).saveAndFlush(any(Transaction.class));


        /*Quick summary of the test:

    Arrange: create fake data and fake (mock) behavior.
    Act: call createTransaction.
    Assert: check the response is correct.
    Verify: check the right data was saved, and methods were called the right number of times.*/
    }

    //Test 2: Duplicate Transaction Reference Retry Test
    //Imagine reference generator ne pehli baar existing reference return kiya.
    // Service ko duplicate reference accept nahi karna chahiye.
    /*
    Expected behaviour:
    1. First attempt → Duplicate reference.
    2. Second attempt → Duplicate reference.
    3. Third attempt → Unique reference.
    4. Transaction successfully save honi chahiye.
    * */

    @Test
    void testCreateTransactionAfterRetryingDuplicateReferences()
    {
        //Step 1: Arrange(Prepare the data)
        //Make a request object, like a user filling a form
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send and a short note
        requestDto.setAmount(new BigDecimal("500.00"));
        requestDto.setDescription("Retry test");

        //Three reference numbers:
        //The first two are "already used"(duplicates)
        //The third one is new and safe to use
        String duplicateReference1 = "TXN1111111111111";
        String duplicateReference2 = "TXN2222222222222";
        String uniqueReference = "TXN3333333333333";

        //Fake the reference generator:
        //1st call gives duplicateReference1
        //2nd call gives duplicateReference2
        //3rd call gives unique reference
        when(transactionReferenceGenerator.generateTransactionReference())
                .thenReturn(duplicateReference1, duplicateReference2, uniqueReference);

        //Fake the DB check:
        //first reference is already used, so say "true" (it exists)
        when(transactionJpaRepository.existsByTransactionReference(duplicateReference1))
                .thenReturn(true);

        //second reference is also used, so say "true"
        when(transactionJpaRepository.existsByTransactionReference(duplicateReference2)).thenReturn(true);

        //third reference is new, so say "false"
        when(transactionJpaRepository.existsByTransactionReference(uniqueReference)).thenReturn(false);

        //Fake the DB save
        //when save is called, set id = 2 and give the same object back
        //(a real database would create the id for us)
        when(transactionJpaRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    //Take the object that was sent to save
                    Transaction transaction = invocation.getArgument(0);

                    //Set an id like the DB would go:
                    transaction.setId(2L);

                    //Return it back
                    return transaction;
                });

        //Step 2: ACT (run the real method)
        //Call the method we want to test
        //Inside, it will try 3 times to get a unique reference
        TransactionResponseDto responseDto = transactionServiceImpl.createTransaction(requestDto);

        //Step 3: Assert(Check the result)
        //The response should not be empty
        assertNotNull(responseDto);

        //The id should be the one we set in the fake save
        assertEquals(2L, responseDto.getId());

        //The final reference should be unique
        assertEquals(uniqueReference, responseDto.getTransactionReference());

        //A new transaction should start as "PENDING"
        assertEquals("PENDING", responseDto.getTransactionStatus());

        //Step 4: Verify(check how methods were called)
        //Reference generator should be called 3 times
        verify(transactionReferenceGenerator, times(3)).generateTransactionReference();

        //Save should be called only 1 time,
        //because we save only after we find a unique reference
        verify(transactionJpaRepository, times(1)).saveAndFlush(any(Transaction.class));
    }

    //Test 3: Maximum 5 retry attempts failure test
    //Suppose reference generator continuously duplicate references return karta hai.
    /*Expected behaviour:
    Attempt 1: Duplicate
    Attempt 2: Duplicate
    Attempt 3: Duplicate
    Attempt 4: Duplicate
    Attempt 5: Duplicate
    Service: IllegalStateException

    Transaction save nahi hona chahiye.
     */

    @Test
    void testCreateTransactionFailsWhenUniqueTransactionCannotBeGenerated()
    {
        //Step 1: Arrange(prepare the data)
        //Make a request object
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send and a short note
        requestDto.setAmount(new BigDecimal("500.00"));
        requestDto.setDescription("Retry Test");

        //One reference number that is already used
        String duplicateReference = "TXN1111111111111";

        //Fake the reference generator:
        //Every time it is called, it gives the same used reference
        when(transactionReferenceGenerator.generateTransactionReference()).thenReturn(duplicateReference);

        //Fake the DB check:
        //always say "true"
        //so the service can never find a new one
        when(transactionJpaRepository.existsByTransactionReference(duplicateReference)).thenReturn(true);

        //Step 2 & 3: Act and Assert (run and check the error)
        //Call the method and expect it to fail with IllegalStateException
        //If no error comes, the test will fail
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                ()-> transactionServiceImpl.createTransaction(requestDto));

        //Check the error message is exactly what we expect
        //It says the service gave up after 5 tries
        assertEquals("Unable to generate a unique transaction reference after 5 attempts",
                exception.getMessage());

        //Step 4: Verify (Check how methods are called)
        //The generator should be called exactly 5 times
        //the service tries 5 times
        verify(transactionReferenceGenerator, times(5))
                .generateTransactionReference();

        //The DB check should also run exactly 5 times
        verify(transactionJpaRepository, times(5))
                .existsByTransactionReference(duplicateReference);

        //Save should NEVER be called
        //because we never found a safe reference
        verify(transactionJpaRepository, never())
                .saveAndFlush(any(Transaction.class));

        /*
        Important learning
This is called negative-path testing.
We're not testing what happens when everything works.
We're testing whether the service behaves safely when an operation repeatedly fails.

In banking systems, failure handling is just as important as successful execution.
        * */
    }

    //Test 4: Repository Persistence Failure Test
    //Ab simulate karenge ki transaction reference successfully generate ho gaya,
    // lekin DB mein save karte waqt exception aa gaya.

    /*
    * What are we verifying?
    We're checking that:
    - The service attempts to save the transaction.
    - A database exception is not silently swallowed.
    - The original exception propagates to the caller.*/
    @Test
    void testCreateTransactionWhenRepositoryThrowsException()
    {
        //Step 1: Arrange(Prepare the data)
        //Make a request object, like a user filling a form.
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send and a short note
        requestDto.setAmount(new BigDecimal("500.00"));
        requestDto.setDescription("Database failure test");

        //A fixed reference number we will use in the test
        String transactionReference = "TXN1234567890123";

        //Fake the reference generator
        //when it is asked for a reference, give our fixed one.
        when(transactionReferenceGenerator.generateTransactionReference()).thenReturn(transactionReference);

        //Fake the DB check:
        //say "this reference is not used before", so it is safe to use
        when(transactionJpaRepository.existsByTransactionReference(transactionReference))
                .thenReturn(false);
        //Fake the DB save
        //when save is called, throw an error
        //(this pretends the DB is broken)
        when(transactionJpaRepository.saveAndFlush(any(Transaction.class)))
                .thenThrow(new RuntimeException("Database save failed"));

        //Step 2 & 3: Act & Assert(run and check the error)
        //call the method and expect it to fail with RuntimeException
        //If no error comes, the test will fail
        RuntimeException exception = assertThrows(RuntimeException.class, ()->transactionServiceImpl.createTransaction(requestDto));

        //Check the error message is the same one we threw in the fake save
        //This shows the service does not hide or change the error
        assertEquals("Database save failed", exception.getMessage());

        //Step 4: Verify(Check how methods were called)
        //The reference generator should be called one time
        verify(transactionReferenceGenerator).generateTransactionReference();

        //The DB check for the reference should run one time
        verify(transactionJpaRepository).existsByTransactionReference(transactionReference);

        //Save should be called one time
        verify(transactionJpaRepository).saveAndFlush(any(Transaction.class));

    }

    //Test 10: Test same source and destination account
    //There's an important business rule: A user must not transfer money from an account to the same account.
    //Now let's verify this business rule with a TransactionServiceImpl unit test.
    @Test
    void testCreateTransactionWithSameSourceAndDestinationAccount()
    {
        //Step 1: Arrange -- Build the request body that we will send.
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The money comes from account 1.
        requestDto.setFromAccountId(1L);

        //The money also goes to account 1.
        //Sending money from account to itself makes no sense.
        //So we use this on purpose to test the error.
        requestDto.setToAccountId(1L);

        //The amount is 500.00.
        requestDto.setAmount(new BigDecimal("500.00"));

        //A short note for the transfer.
        requestDto.setDescription("Invalid self transfer");

        //Step 2 & 3: Act & Assert -- Call the service and check the answer.
        //We expect the service to throw an error called IllegalArgumentException.
        //This means it saw the same account twice and said no.
        assertThrows(IllegalArgumentException.class, () -> transactionServiceImpl.createTransaction(requestDto));

        //Step 4: Verify --Make sure the helper parts were never used.
        //The service should stop early, before it makes a reference number.
        //or saves anything to the DB.
        verifyNoInteractions(transactionReferenceGenerator);
        verifyNoInteractions(transactionJpaRepository);

        /*
        Why this test matters:
        - Same source and destination account must be rejected.
        - No transaction reference should be generated.
        - No database operation should take place.
        * */
    }

    //Test 11: Test create transaction with a valid request after these changes.
    //Let's make sure the new validation condition hasn't affected normal transactions.
    @Test
    void testCreateTransactionWithDifferentSourceAndDestinationAccounts()
    {
        //Step 1: Arrange -- Build the request body that we will send
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        // The money comes from account 1.
        requestDto.setFromAccountId(1L);

        // The money goes to account 2.
        // These are two different accounts, so this transfer is allowed.
        requestDto.setToAccountId(2L);
        // The amount is 500.00.
        requestDto.setAmount(new BigDecimal("500.00"));

        // A short note for the transfer.
        requestDto.setDescription("Valid transfer");

        // Fake setup: When the code asks for a new reference number,
        // give it this fixed number.
        when(transactionReferenceGenerator.generateTransactionReference()).thenReturn("TXN123456789013");

        // Fake setup: Say that this reference number is not used yet.
        // So it is safe to save the transfer with it.
        when(transactionJpaRepository.existsByTransactionReference("TXN123456789013")).thenReturn(false);

        // Fake setup: Make a pretend "saved" transfer.
        // This is what the database would give back after saving.
        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setTransactionReference("TXN123456789013");
        savedTransaction.setFromAccountId(1L);
        savedTransaction.setToAccountId(2L);
        savedTransaction.setAmount(new BigDecimal("500.00"));
        savedTransaction.setTransactionType("TRANSFER");
        savedTransaction.setTransactionStatus("PENDING");
        savedTransaction.setDescription("Valid transfer");

        // Fake setup: When the code tries to save any transfer,
        // return the pretend saved transfer above.
        when(transactionJpaRepository.saveAndFlush(any(Transaction.class))).thenReturn(savedTransaction);

        //Act: Call the real service with our request
        TransactionResponseDto responseDto = transactionServiceImpl.createTransaction(requestDto);

        //Assert: Check that the answer looks correct.
        //The answer should not be empty.
        assertNotNull(responseDto);

        //The "from" account in the answer should be 1.
        assertEquals(1L, responseDto.getFromAccountId());

        //The "to" account in the answer should be 2.
        assertEquals(2L, responseDto.getToAccountId());

        //The transfer should be marked as "PENDING" (waiting)
        assertEquals("PENDING", responseDto.getTransactionStatus());

        //Verify: Make sure the save function was called exactly one time.
        //This means the transfer was saved to the DB once.
        verify(transactionJpaRepository, times(1))
                .saveAndFlush(any(Transaction.class));

    }

    /*We've now verified both sides of the business rule:
    - Same source and destination account → rejected.
    - Different source and destination accounts → accepted.*/

    //Test 12: Verify the saved transaction status
    //A newly created transaction should start with 'PENDING' not 'SUCCESS',
    // because we haven't implemented the actual fund transfer yet.

    @Test
    void testNewTransactionStatusShouldBePending()
    {
        //Arrange: Build the request body that we will send
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The money comes from account 1
        requestDto.setFromAccountId(1L);

        //The money goes to the account 2.
        requestDto.setToAccountId(2L);

        //The amount is 500.00
        requestDto.setAmount(new BigDecimal("500.00"));

        //Fake setup: When the code asks for a new reference number
        //give it this fixed number.
        when(transactionReferenceGenerator.generateTransactionReference()).thenReturn("TXN123456789013");

        //Fake setup: Say that every reference number is not used yet.
        //So it is safe to save the transfer.
        when(transactionJpaRepository.existsByTransactionReference(anyString())).thenReturn(false);

        //Fake setup: When the code tries to save a transfer,
        // just give back the same transfer that was sent in.
        //This lets us check what the service set on it.
        when(transactionJpaRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //Act: Call the real service with our request
        TransactionResponseDto responseDto = transactionServiceImpl.createTransaction(requestDto);

        //Assert: Check the answer.
        //A new transfer should start with status "PENDING"(waiting)
        assertEquals("PENDING", responseDto.getTransactionStatus());

        //A new transfer should have the type "TRANSFER"
        assertEquals("TRANSFER", responseDto.getTransactionType());

        //Verify: Make sure the save function was called one time.
        //This proves the transfer was actually saved to the DB.
        verify(transactionJpaRepository).saveAndFlush(any(Transaction.class));
    }
}
