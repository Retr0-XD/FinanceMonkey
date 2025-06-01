package com.jpa.financemonkey.repository;

import com.jpa.financemonkey.entity.FinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {
    // Custom queries for analytics can be added here
}

