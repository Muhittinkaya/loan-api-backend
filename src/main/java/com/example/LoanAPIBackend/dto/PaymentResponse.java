package com.example.LoanAPIBackend.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private int installmentsPaidCount;
    private BigDecimal totalBaseAmountDebitedFromPayment;
    private BigDecimal totalActualAmountAccountedForInstallments;
    private BigDecimal remainingPaymentAmount;
    private boolean loanFullyPaid;
    private String message;
}
