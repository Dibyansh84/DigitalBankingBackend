package com.digitalbank.transaction.controller;

import com.digitalbank.transaction.dto.TransactionCreateRequestDto;
import com.digitalbank.transaction.dto.TransactionResponseDto;
import com.digitalbank.transaction.repository.TransactionJpaRepository;
import com.digitalbank.transaction.serviceImpl.TransactionServiceImpl;
import com.digitalbank.transaction.utility.TransactionReferenceGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// ----- MediaType for APPLICATION_JSON -----
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// @WebMvcTest	Loads only the MVC layer for testing
@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {
    //MockMvc lets us send fake HTTP requests (GET, POST, etc.)
    //to the controller, without starting a real server.
    @Autowired
    private MockMvc mockMvc;

    /* ObjectMapper changes Java objects into JSON text
    and JSON text back into Java objects.
    We use it to build the request body for our texts.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /* A fake(mock) service, because we do not want to run real business logic.
    The controller will use this fake service instead of the real one
    @MockitoBean puts this mock inside the Spring test setup*/
    //@MockitoBean: gives a fake service, so we control what it returns.
    @MockitoBean
    private TransactionServiceImpl transactionService;

    //Test 1: Successful Transaction API Test
    @Test
    void testCreateTransactionSuccessfully() throws Exception {
        //Step 1: Arrange(Prepare the data)
        //Make a request object, like a user filling a form
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send and a short note
        requestDto.setAmount(new BigDecimal("500.00"));
        requestDto.setDescription("Test transfer");

        //Make the response that the fake service will give back
        //This is what a real service would return after saving
        TransactionResponseDto responseDto = new TransactionResponseDto();
        responseDto.setId(1L);  //id
        responseDto.setTransactionReference("TXN123456789013");//reference number
        responseDto.setFromAccountId(1L);//from account id
        responseDto.setToAccountId(2L);//to account id
        responseDto.setAmount(new BigDecimal("500.00"));//amount
        responseDto.setTransactionType("TRANSFER");//type
        responseDto.setTransactionStatus("PENDING");//status
        responseDto.setDescription("Test transfer");//description
        responseDto.setCreatedAt(OffsetDateTime.now());// time of creation

        //Fake the service:
        //when the controller calls createTransaction with any request, give back our ready-made response.
        when(transactionService.createTransaction(any(TransactionCreateRequestDto.class)))
                .thenReturn(responseDto);

        // ---------- STEP 2 & 3: ACT AND ASSERT (send request and check) ----------

        // Send a fake HTTP POST request to the controller
        mockMvc.perform(post("/api/transactions/createTransaction")

                        // Tell the controller we are sending JSON
                        .contentType(MediaType.APPLICATION_JSON)

                        // Change our request object into JSON text and send it
                        .content(objectMapper.writeValueAsString(requestDto)))

                // HTTP status should be 201 (Created)
                .andExpect(status().isCreated())

                // The reply should be in JSON format
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                // Check each field in the JSON reply
                // "$" means the top of the JSON, "$.id" means the field "id"
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionReference")
                        .value("TXN123456789013"))
                .andExpect(jsonPath("$.fromAccountId").value(1))
                .andExpect(jsonPath("$.toAccountId").value(2))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.transactionStatus").value("PENDING"))
                .andExpect(jsonPath("$.description").value("Test transfer"));

        // ---------- STEP 4: VERIFY (check how the service was used) ----------

        // The controller should call the service exactly one time
        verify(transactionService, times(1))
                .createTransaction(any(TransactionCreateRequestDto.class));

    }

    //Test 2: Reject a zero transaction amount
    /*What are we testing?
    Your DTO's @DecimalMin(value = "0.00", inclusive = false) should reject zero before the controller calls the service.*/
    @Test
    void testCreateTransactionWithZeroAmount() throws Exception {
        //Step 1: Arrange (Prepare the data by making a request body)
        //We are making a fake transfer request.
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //Amount to send and a short note
        //The amount is zero. This is not allowed, so we expect an error.
        requestDto.setAmount(BigDecimal.ZERO);

        //A short note for the transfer
        requestDto.setDescription("Invalid transfer");

        //Step 2 and 3: Act and Assert (send request and check)

        //Send the request to the API
        //We send the POST call to the create transaction URL.
        //We turn the request object into JSON text and send it as the body.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //Check the answer
                //The server should say "Bad Request" (HTTP 400)
                //This means the zero amount was rejected
                .andExpect(status().isBadRequest());

//      STEP 4: VERIFY (check how the service was used)
        //Since the request was bad, the code should stop early.
        //The service layer should not be touched at all.
        //verifyNoInteractions(transactionService) confirms that invalid requests never reach the service layer.
        verifyNoMoreInteractions(transactionService);
    }

    //Test 3: Reject a negative transaction amount.
    @Test
    void testCreateTransactionWithNegativeAmount() throws Exception {
        //Step 1: Arrange (Prepare the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //The amount is -100. A negative amount makes no sense for transfer, so the server should reject it.
        requestDto.setAmount(new BigDecimal("-100.00"));

        //A short note for the transfer.
        requestDto.setDescription("Invalid transfer");

        //Step 2: Act & Assert (Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                //The server should answer "Bad Request" (HTTP 400).
                //This means the negative amount was refused.
                .andExpect(status().isBadRequest());

        //Step 3: Verify: Make sure the service was never called.
        //The request was bad, so the code should stop the service.
        verifyNoInteractions(transactionService);
    }

    //Test 4: Reject a missing transaction amount (or Test null transaction amount)
    /* Concept: @NotNull rejects a missing amount,
    while @DecimalMin rejects zero and negative amounts. These validations cover different cases.*/
    @Test
    void testCreateTransactionWithNullAmount() throws Exception {
        //Step 1: Arrange(Prepare the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account 1 to account 2
        requestDto.setFromAccountId(1L);
        requestDto.setToAccountId(2L);

        //The amount is null, which means "no amount was given".
        requestDto.setAmount(null);

        //A short note for the transfer.
        requestDto.setDescription("Invalid transfer");

        //Step 2: Act & Assert (Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "Bad Request" (HTTP 400)
                //This means the missing amount was refused.
                .andExpect(status().isBadRequest());

        //Step 3: Verify ---> Make sure the service was never called.
        //The request was bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);

    }

    //Test 5: Reject a missing source account ID.
    //This verifies the @NotNull validation on fromAccountId.
    @Test
    void testCreateTransactionWithNullAccountId() throws Exception {
        //Step 1: Arrange(Prepare the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The "fromAccountId" is missing (null).
        //A transfer needs to know which account the money comes from.
        //So we leave this empty on purpose to test the error.
        requestDto.setFromAccountId(null);

        //The money goes to account id 2.
        requestDto.setToAccountId(2L);

        //The amount is 500.00. This part is fine.
        requestDto.setAmount(new BigDecimal("500.00"));

        //A short note for the transfer.
        requestDto.setDescription("Test transfer");

        //Step 2: Act & Assert(Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "BAD REQUEST" (HTTP 400)
                //This means the missing "from" account was refused.
                .andExpect(status().isBadRequest());

        //Step 3: Verify(Make sure the service was never called)
        //The request was bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);
    }

    //Test 6: Reject a zero source account ID.
    //This checks the @Positive constraint on fromAccountId.
    @Test
    void testCreateTransactionWithZeroFromAccountId() throws Exception {
        //Step 1: Arrange(Build the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The "fromAccountId" is zero.
        //Account numbers start at 1, so 0 is not a real account.
        //We send it on purpose to test that the server says no.
        requestDto.setFromAccountId(0L);

        //The money goes to account 2.
        requestDto.setToAccountId(2L);

        //The amount is 500.00. This part is fine.
        requestDto.setAmount(new BigDecimal("500.00"));

        //A short note for the transfer.
        requestDto.setDescription("Test transfer");

        //Step 2: Act & Assert(Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "BAD REQUEST" (HTTP 400)
                //This means the fake "from" account(0) was refused.
                .andExpect(status().isBadRequest());

        //Step 3: Verify(Make sure the service was never called).
        //The request is bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);
    }

    //Test 7: Reject a missing destination account ID (or Reject toAccountId with null value).
    //This checks the @NotNull validation on toAccountId.
    @Test
    void testCreateTransactionWithNullToAccountId() throws Exception {
        //Step 1: Arrange(Build the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The money goes from account 1.
        requestDto.setFromAccountId(1L);

        //The "toAccountId" is missing (null).
        //A transfer needs to know which account the money goes to.
        //So we leave this empty on purpose to test the error.
        requestDto.setToAccountId(null);

        //The amount is 500.00. This part is fine.
        requestDto.setAmount(new BigDecimal("500.00"));

        //A short note for the transfer.
        requestDto.setDescription("Test transfer");

        //Step 2: Act & Assert(Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "BAD REQUEST" (HTTP 400)
                //This means the fake "from" account(0) was refused.
                .andExpect(status().isBadRequest());

        //Step 3: Verify
        // Make sure the service was never called.
        //The request is bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);
    }

    //Test 8: Reject a zero destination account ID.
    //This checks the @Positive constraint on toAccountId.
    @Test
    void testCreateTransactionWithZeroAccountId() throws Exception {
        //Step 1: Arrange(Build the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //The money goes from account 1.
        requestDto.setFromAccountId(1L);

        //The "toAccountId" is zero.
        //Account numbers start at 1, so 0 is not a real account.
        //We send it on purpose to test that the server says no.
        requestDto.setToAccountId(0L);

        //The amount is 500.00. This part is fine.
        requestDto.setAmount(new BigDecimal("500.00"));

        //A short note for the transfer.
        requestDto.setDescription("Test transfer");

        //Step 2 and 3: Act & Assert(Send the request and check the answer)
        //We send a POST call to the create transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post(("/api/transactions/createTransaction"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "BAD REQUEST" (HTTP 400)
                //This means the fake "from" account(0) was refused.
                .andExpect(status().isBadRequest());

        //Step 4: Verify
        //Make sure the service was never called.
        //The request is bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);
    }

    //Test 9: Reject an empty description longer than the allowed limit.
    /* Actually, an empty description is valid in the current DTO.
    Let's test the real constraint: description exceeding 255 characters.*/
    //This tests your DTO's @Size(max = 255) constraint.
    @Test
    void testCreateTransactionWithDescriptionExceeding255Characters() throws Exception {
        //Step 1:  Arrange(Build the request body that we will send)
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();

        //Money goes from account ID 1
        requestDto.setFromAccountId(1L);

        //Money goes to account ID 2
        requestDto.setToAccountId(2L);

        //The amount is 500.00.
        requestDto.setAmount(new BigDecimal("500.00"));

        //The note is too long on purpose.
        //We make a text with 256 letter "A" in a row.
        //The limit is 255, so this is one letter too many.
        requestDto.setDescription("A".repeat(256));

        //Step 2 and 3: Act & Assert
        //Send the request and check the answer.
        //We send a POST call to create the transaction URL.
        //The body is the request object turned into JSON text.
        mockMvc.perform(post("/api/transactions/createTransaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                //The server should answer "BAD REQUEST" (HTTP 400)
                //This means the long note is refused
                .andExpect(status().isBadRequest());

        //Step 4: Verify
        //Make sure the service was never called.
        //The request was bad, so the code should stop before the service.
        verifyNoInteractions(transactionService);

    }
}
