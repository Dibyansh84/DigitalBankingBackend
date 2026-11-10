package com.digitalbank.account.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// @Component marks this class as a Spring bean so it can be injected wherever account numbers are needed.
@Component
public class AccountNumberGenerator
{
    /* SecureRandom is a cryptographically strong number generator, which makes generated account numbers hard to predict.
     It is static so one instance is shared across all calls(it is thread safe).*/
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /* generateAccountNumber() generates a random 12-digit account number as String.
    * 12-digit numeric string, e.g. "493718265104"*/
    public String generateAccountNumber()
    {
        //Produces a random long in the range [100_000_000_000, 1_000_000_000_000).
        // The lower bound is inclusive and the upper bound is exclusive.
        // Lower bound 100 billion guarantees the number has exactly 12 digits,
        // so there are no leading zeros that would be lost when converting to a number.
        Long accountNumber = SECURE_RANDOM.nextLong(
                100_000_000_000L, // minimum value (inclusive): 12-digit number starts here
                1_000_000_000_000L // maximum value (exclusive): just above the largest 12-digit number
        );

        // Convert the numeric value to a String so it can be stored or returned as an account number
        return String.valueOf(accountNumber);
    }
}
