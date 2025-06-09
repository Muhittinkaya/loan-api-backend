package com.example.LoanAPIBackend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateUserLoanRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Loan amount must be positive")
    private BigDecimal amount;
    @NotNull
    @DecimalMin(value = "0.10", message = "Interest rate must be at least 0.1")
    @DecimalMax(value = "0.50", message = "Interest rate must be at most 0.5")
    private BigDecimal interestRate;
    @NotNull
    private Integer numberOfInstallments;
}
