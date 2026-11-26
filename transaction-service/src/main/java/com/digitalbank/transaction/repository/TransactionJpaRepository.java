package com.digitalbank.transaction.repository;

import com.digitalbank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionJpaRepository extends JpaRepository<Transaction, Long>
{
    //existsByTransactionReference() checks whether a transaction reference already exists.
    boolean existsByTransactionReference(String transactionReference);
}
