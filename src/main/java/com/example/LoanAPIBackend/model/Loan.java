package com.example.LoanAPIBackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer numberOfInstallments;

    @Column(nullable = false, updatable = false)
    private LocalDate createDate;

    @Column(columnDefinition = "boolean default false")
    private boolean isPaid = false;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanInstallment> installments = new ArrayList<>();

    @Transient
    public BigDecimal getTotalLoanAmountWithInterest() {
        if (loanAmount == null || interestRate == null) {
            return BigDecimal.ZERO;
        }
        // Total Amount = Principal * (1 + Interest Rate)
        return loanAmount.multiply(BigDecimal.ONE.add(interestRate)).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getCalculatedInstallmentAmount() {
        if (loanAmount == null || interestRate == null || numberOfInstallments == null || numberOfInstallments == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalWithInterest = getTotalLoanAmountWithInterest();
        return totalWithInterest.divide(BigDecimal.valueOf(numberOfInstallments), 2, RoundingMode.HALF_UP);
    }

    public void addInstallment(LoanInstallment installment) {
        installments.add(installment);
        installment.setLoan(this);
    }
}
