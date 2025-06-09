package com.example.LoanAPIBackend.controller;

import com.example.LoanAPIBackend.dto.LoanResponse;
import com.example.LoanAPIBackend.dto.*;
import com.example.LoanAPIBackend.service.LoanService;
import com.example.LoanAPIBackend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final PaymentService paymentService;

    @PostMapping("/createLoan")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateUserLoanRequest createLoanRequest
            , Authentication authentication) {
        LoanResponse loanResponse = loanService.createLoanbyUser(createLoanRequest,authentication);
        return new ResponseEntity<>(loanResponse, HttpStatus.CREATED);
    }
    @PostMapping("/createLoanByAdmin")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<LoanResponse> createLoanByAdmin(@Valid @RequestBody CreateLoanRequest createLoanRequest
            , Authentication authentication) {
        LoanResponse loanResponse = loanService.createLoanWithCustomerbyAdmin(createLoanRequest,authentication);
        return new ResponseEntity<>(loanResponse, HttpStatus.CREATED);
    }
    @GetMapping("/listLoans")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<LoanResponse>> listLoans(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer numberOfInstallments,
            @RequestParam(required = false) Boolean isPaid,
            Authentication authentication) { // Spring Security provides the authentication object

        List<LoanResponse> loans = loanService.getLoans(
                Optional.ofNullable(customerId),
                authentication
        );
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable Long loanId, Authentication authentication) {
        LoanResponse loan = loanService.getLoanById(loanId, authentication);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/{loanId}/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<LoanInstallmentResponse>> listInstallmentsForLoan(
            @PathVariable Long loanId, Authentication authentication) {
        List<LoanInstallmentResponse> installments = loanService.getInstallmentsByLoanId(loanId, authentication);
        return ResponseEntity.ok(installments);
    }

    @PostMapping("/{loanId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<PaymentResponse> payLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody PayLoanRequest payLoanRequest,
            Authentication authentication) {
        PaymentResponse paymentResponse = paymentService.payLoanInstallments(loanId, payLoanRequest.getAmount(), authentication);
        return ResponseEntity.ok(paymentResponse);
    }
}
