package com.digitalbank.customer.exception;

public class CustomerNotFoundException extends RuntimeException
{
    //Constructor
    public CustomerNotFoundException(String message)
    {
        super(message);
    }
}
