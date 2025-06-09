package com.example.LoanAPIBackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayLoanRequest {
    @NotNull
    @DecimalMin(value = "0.0", message = "Payment amount must be positive")
    private BigDecimal amount;
}
