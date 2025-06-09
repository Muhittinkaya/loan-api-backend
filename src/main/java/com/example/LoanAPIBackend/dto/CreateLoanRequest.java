package com.example.LoanAPIBackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLoanRequest extends CreateUserLoanRequest{
    @NotNull
    private Long customerId;
}
