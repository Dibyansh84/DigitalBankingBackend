package com.digitalbank.customer.exception;

public class DuplicateEmailException extends RuntimeException
{
    public DuplicateEmailException(String message)
    {
        super(message);
    }
}

//Iss DuplicateEmailException ka use hum tab karenge jab email already registered hai.