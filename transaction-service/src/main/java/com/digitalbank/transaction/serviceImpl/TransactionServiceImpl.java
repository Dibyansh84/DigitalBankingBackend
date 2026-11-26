package com.digitalbank.transaction.serviceImpl;

import com.digitalbank.transaction.dto.TransactionCreateRequestDto;
import com.digitalbank.transaction.dto.TransactionResponseDto;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.repository.TransactionJpaRepository;
import com.digitalbank.transaction.service.TransactionService;
import com.digitalbank.transaction.utility.TransactionReferenceGenerator;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService
{
    // This talks to the database
    private final TransactionJpaRepository transactionJpaRepository;

    //This makes new transaction codes
    private final TransactionReferenceGenerator transactionReferenceGenerator;

    //Constructors
    public TransactionServiceImpl(TransactionJpaRepository transactionJpaRepository, TransactionReferenceGenerator transactionReferenceGenerator)
    {
        this.transactionJpaRepository = transactionJpaRepository;
        this.transactionReferenceGenerator = transactionReferenceGenerator;
    }


    // Maximum number of attempts to generate a unique reference before giving up
    //or How many times we try to find a free code.
    private static final int MAX_RETRY_ATTEMPTS = 5;


    @Override
    public TransactionResponseDto createTransaction(TransactionCreateRequestDto requestDto)
    {
        //Get a code that is not used in the database.
        String transactionReference = generateUniqueReference();

        //Copy the data from the request into a new transaction.
        Transaction transaction = new Transaction();

        //Get the transaction from the request dto and set it in the entity.
        //DTO to entity.
        transaction.setTransactionReference(transactionReference);
        transaction.setFromAccountId(requestDto.getFromAccountId());
        transaction.setToAccountId(requestDto.getToAccountId());
        transaction.setAmount(requestDto.getAmount());
        transaction.setTransactionType("TRANSFER"); // This method only makes transfers
        transaction.setTransactionStatus("PENDING");  //It waits until it is processed
        transaction.setDescription(requestDto.getDescription());

        //Save it to the DB.
        //This also gives us the id and the time it was created.
        Transaction savedTransaction = transactionJpaRepository.saveAndFlush(transaction);

        //Change it into the answer we send back.
        //Return the response dto
        return mapEntityToResponseDto(savedTransaction);
    }

    /**
     * Finds a code that is not in the DB yet.
     * If the code is already used, it tries again with a new one.
     **/

    private String generateUniqueReference()
    {
        for(int attempt = 1; attempt<=MAX_RETRY_ATTEMPTS; attempt++)
        {
            //Make a new code
            String reference = transactionReferenceGenerator.generateTransactionReference();

            //Check if the DB already has this code
            //If the DB doesn't contain the code, then return the code.
            if(!transactionJpaRepository.existsByTransactionReference(reference))
            {
                return reference;//The code is free so we use it.
            }
            //The code was used, so we try again.
        }
        //We tried many times and every code was used, so we stop with an error.
        throw new IllegalStateException("Unable to generate a unique transaction reference after "+MAX_RETRY_ATTEMPTS+" attempts");
    }

    //Convert entity to response dto.
    //Changes a saved transaction into the answer we send back.
    //We only copy the parts that the user is allowed to see.

    private TransactionResponseDto mapEntityToResponseDto(Transaction transaction)
    {
        TransactionResponseDto responseDto = new TransactionResponseDto();
        responseDto.setId(transaction.getId());
        responseDto.setTransactionReference(transaction.getTransactionReference());
        responseDto.setFromAccountId(transaction.getFromAccountId());
        responseDto.setToAccountId(transaction.getToAccountId());
        responseDto.setAmount(transaction.getAmount());
        responseDto.setTransactionType(transaction.getTransactionType());
        responseDto.setTransactionStatus(transaction.getTransactionStatus());
        responseDto.setDescription(transaction.getDescription());
        responseDto.setCreatedAt(transaction.getCreatedAt());

        return responseDto;
    }


}
