package com.example.LoanAPIBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private Long id;
    private Long customerId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private BigDecimal totalAmountWithInterest;
    private Integer numberOfInstallments;
    private LocalDate createDate;
    private boolean isPaid;
    private List<LoanInstallmentResponse> installments;
}
