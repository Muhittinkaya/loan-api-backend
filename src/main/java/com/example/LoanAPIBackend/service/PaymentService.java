package com.example.LoanAPIBackend.service;

import com.example.LoanAPIBackend.dto.PaymentResponse;
import com.example.LoanAPIBackend.exception.PaymentException;
import com.example.LoanAPIBackend.exception.ResourceNotFoundException;
import com.example.LoanAPIBackend.model.Customer;
import com.example.LoanAPIBackend.model.Loan;
import com.example.LoanAPIBackend.model.LoanInstallment;
import com.example.LoanAPIBackend.model.User;

import com.example.LoanAPIBackend.repository.LoanInstallmentRepository;
import com.example.LoanAPIBackend.repository.LoanRepository;
import com.example.LoanAPIBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final CustomerService customerService;
    private final UserRepository userRepository;

    private static final BigDecimal PENALTY_DISCOUNT_RATE_PER_DAY = new BigDecimal("0.001");

    @Transactional
    public PaymentResponse payLoanInstallments(Long loanId, BigDecimal paymentAmountFromUser, Authentication authentication) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        authorizeAccessToLoanPayment(loan, authentication);

        if (loan.isPaid()) {
            throw new PaymentException("This loan is already fully paid.");
        }
        if (paymentAmountFromUser == null || paymentAmountFromUser.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Payment amount must be positive.");
        }

        List<LoanInstallment> unpaidInstallments = loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(loanId);
        if (unpaidInstallments.isEmpty()) {
            if (!loan.isPaid()) {
                loan.setPaid(true);
                loanRepository.save(loan);
            }
            throw new PaymentException("No unpaid installments found for this loan. It might be fully paid.");
        }

        int installmentsPaidThisTransaction = 0;
        BigDecimal totalBaseAmountDebitedFromPayment = BigDecimal.ZERO;
        BigDecimal totalActualAmountRecordedAsPaid = BigDecimal.ZERO;
        BigDecimal remainingUserPayment = paymentAmountFromUser;

        LocalDate today = LocalDate.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        YearMonth maxPayableYearMonth = currentYearMonth.plusMonths(2);

        LoanInstallment firstEligibleInstallment = null;
        for (LoanInstallment inst : unpaidInstallments) {
            YearMonth installmentYearMonth = YearMonth.from(inst.getDueDate());
            if (!installmentYearMonth.isAfter(maxPayableYearMonth)) {
                firstEligibleInstallment = inst;
                break;
            }
        }

        if (firstEligibleInstallment == null) {
            throw new PaymentException("No installments are currently payable within the 3-month window.");
        }

        if (paymentAmountFromUser.compareTo(firstEligibleInstallment.getInstallmentAmount()) < 0) {
            throw new PaymentException("Payment amount is less than the earliest eligible due installment amount ("
                    + firstEligibleInstallment.getInstallmentAmount().setScale(2, RoundingMode.HALF_UP) +
                    "). No installments can be paid.");
        }

        for (LoanInstallment installment : unpaidInstallments) {
            YearMonth installmentYearMonth = YearMonth.from(installment.getDueDate());

            if (installmentYearMonth.isAfter(maxPayableYearMonth)) {
                break;
            }

            BigDecimal baseInstallmentValue = installment.getInstallmentAmount();

            if (remainingUserPayment.compareTo(baseInstallmentValue) >= 0) {

                BigDecimal actualPaidForThisInstallment = baseInstallmentValue;
                long daysDifference = ChronoUnit.DAYS.between(today, installment.getDueDate());

                if (daysDifference > 0) {
                    BigDecimal discount = baseInstallmentValue.multiply(PENALTY_DISCOUNT_RATE_PER_DAY)
                            .multiply(BigDecimal.valueOf(daysDifference))
                            .setScale(2, RoundingMode.HALF_UP);
                    actualPaidForThisInstallment = baseInstallmentValue.subtract(discount);
                }
                else if (daysDifference < 0) {
                    BigDecimal penalty = baseInstallmentValue.multiply(PENALTY_DISCOUNT_RATE_PER_DAY)
                            .multiply(BigDecimal.valueOf(Math.abs(daysDifference)))
                            .setScale(2, RoundingMode.HALF_UP);
                    actualPaidForThisInstallment = baseInstallmentValue.add(penalty);
                }

                installment.setPaidAmount(actualPaidForThisInstallment);
                installment.setPaymentDate(today);
                installment.setPaid(true);
                loanInstallmentRepository.save(installment);

                remainingUserPayment = remainingUserPayment.subtract(baseInstallmentValue);
                totalBaseAmountDebitedFromPayment = totalBaseAmountDebitedFromPayment.add(baseInstallmentValue);
                totalActualAmountRecordedAsPaid = totalActualAmountRecordedAsPaid.add(actualPaidForThisInstallment);
                installmentsPaidThisTransaction++;

                Customer customer = loan.getCustomer();
                BigDecimal principalPerInstallment = loan.getLoanAmount()
                        .divide(BigDecimal.valueOf(loan.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);
                customerService.updateUsedCreditLimit(customer, principalPerInstallment.negate());
            } else {
                break;
            }
        }

        long remainingUnpaidCount = loanInstallmentRepository.countByLoanIdAndIsPaidFalse(loanId);
        if (remainingUnpaidCount == 0 && installmentsPaidThisTransaction > 0) {
            loan.setPaid(true);
            loanRepository.save(loan);
        } else if (remainingUnpaidCount == 0 && unpaidInstallments.size() == 0 && !loan.isPaid()){
            loan.setPaid(true);
            loanRepository.save(loan);
        }

        String message;
        if (installmentsPaidThisTransaction > 0) {
            message = installmentsPaidThisTransaction + " installment(s) paid successfully.";
            if (loan.isPaid()) {
                message += " The loan is now fully paid.";
            }
        } else {
            message = "Payment processed, but no installments were covered. Please check the payment amount and installment status.";
        }

        return PaymentResponse.builder()
                .installmentsPaidCount(installmentsPaidThisTransaction)
                .totalActualAmountAccountedForInstallments(totalActualAmountRecordedAsPaid)
                .totalBaseAmountDebitedFromPayment(totalBaseAmountDebitedFromPayment)
                .remainingPaymentAmount(remainingUserPayment)
                .loanFullyPaid(loan.isPaid())
                .message(message)
                .build();
    }

    private void authorizeAccessToLoanPayment(Loan loan, Authentication authentication) {
        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (authenticatedUser.getRole() == com.example.LoanAPIBackend.enums.Role.ROLE_CUSTOMER) {
            if (authenticatedUser.getCustomer() == null ||
                    !loan.getCustomer().getId().equals(authenticatedUser.getCustomer().getId())) {
                throw new AccessDeniedException("You do not have permission to pay this loan.");
            }
        }
    }
}
