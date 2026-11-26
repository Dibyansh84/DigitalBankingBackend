package com.digitalbank.transaction.utility;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TransactionReferenceGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateTransactionReference()
    {
        long referenceNumber = SECURE_RANDOM.nextLong(1_000_000_000_000L, 10_000_000_000_000L);

        return "TXN" + referenceNumber;
    }

    /* This separate TransactionReferenceGenerator class keeps reference generation separate from the business logic.
    And makes unit testing easier through Mockito.
    Uses SecureRandom instead of predictable random numbers.
    Generates a reference with the TXN prefix.

    Important production consideration: Random generation alone cannot guarantee uniqueness.
    We'll enforce uniqueness through the database constraint and implement bounded retry handling in the service.*/
}
