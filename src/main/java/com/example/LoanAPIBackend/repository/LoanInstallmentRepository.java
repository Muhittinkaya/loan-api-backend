package com.example.LoanAPIBackend.repository;

import com.example.LoanAPIBackend.model.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {
    ArrayList<LoanInstallment> findByLoanIdAndIsPaidFalseOrderByDueDateAsc(Long loanId);
    long countByLoanIdAndIsPaidFalse(Long loanId);

}
