package com.digitalbank.account;

import com.digitalbank.account.repository.AccountJdbcRepository;
import com.digitalbank.account.repository.AccountJpaRepository;
import com.digitalbank.account.serviceImpl.AccountServiceImpl;
import com.digitalbank.account.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class) //@ExtendWith(MockitoExtension.class) -- Integrates Mockito with JUnit 5
public class AccountServiceImplTest
{
    //@Mock	creates a mock dependency
    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private AccountJdbcRepository accountJdbcRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

//    @InjectMocks	injects mocks into the class being tested i.e., AccountServiceImpl class.
    @InjectMocks
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp()
    {
        //@BeforeEach runs the setUp() method before every test
        //Mockito initializes and injects mocks automatically
    }

    @Test
    void testAccountServiceInitialization()
    {
        //@Test annotation identifies a test method.

        //Verify that the test class and mocks initializes successfully.
        assertNotNull(accountService);
    }
}
