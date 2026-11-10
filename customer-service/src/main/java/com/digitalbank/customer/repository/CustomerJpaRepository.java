package com.digitalbank.customer.repository;

import com.digitalbank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<Customer, Long>
{
    boolean existsByEmail(String email);
    //Spring Data JPA method name se automatically email existence check karne wali query generate karega.
}
