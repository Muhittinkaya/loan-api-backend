package com.example.LoanAPIBackend.service;

import java.util.function.Predicate;

import com.example.LoanAPIBackend.dto.CreateLoanRequest;
import com.example.LoanAPIBackend.dto.CreateUserLoanRequest;
import com.example.LoanAPIBackend.dto.LoanResponse;
import com.example.LoanAPIBackend.dto.LoanInstallmentResponse;
import com.example.LoanAPIBackend.enums.AllowedInstallmentCounts;
import com.example.LoanAPIBackend.enums.Role;
import com.example.LoanAPIBackend.exception.InsufficientCreditException;
import com.example.LoanAPIBackend.exception.ResourceNotFoundException;
import com.example.LoanAPIBackend.model.Customer;
import com.example.LoanAPIBackend.model.Loan;
import com.example.LoanAPIBackend.model.LoanInstallment;
import com.example.LoanAPIBackend.model.User;
import com.example.LoanAPIBackend.repository.LoanRepository;
import com.example.LoanAPIBackend.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerService customerService;
    private final UserRepository userRepository;

    @Transactional
    public LoanResponse createLoanWithCustomerbyAdmin(CreateLoanRequest request, Authentication authentication) {
        if (!AllowedInstallmentCounts.isValid(request.getNumberOfInstallments())) {
            throw new ValidationException("Number of installments can only be 6, 9, 12, or 24.");
        }

        if (request.getInterestRate().compareTo(BigDecimal.valueOf(0.1)) < 0 ||
                request.getInterestRate().compareTo(BigDecimal.valueOf(0.5)) > 0) {
            throw new ValidationException("Interest rate must be between 0.1 (10%) and 0.5 (50%).");
        }

        Customer customer = customerService.getCustomerById(request.getCustomerId());

        Loan savedLoan = createLoan(customer,request.getAmount(),request.getInterestRate(),request.getNumberOfInstallments());
        return mapToLoanResponse(savedLoan, true);
    }


    private Loan createLoan(Customer customer,BigDecimal amount, BigDecimal interestRate , Integer numberOfInstallments ){
        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());
        if (availableCredit.compareTo(amount) < 0) {
            throw new InsufficientCreditException(
                    "Customer does not have enough credit limit. Available: " + availableCredit +
                            ", Requested: " + amount);
        }

        Loan loan= new Loan();
        loan.setCustomer(customer);
        loan.setLoanAmount(amount);
        loan.setInterestRate(interestRate);
        loan.setNumberOfInstallments(numberOfInstallments);
        loan.setCreateDate(LocalDate.now());
        loan.setPaid(false);

        BigDecimal singleInstallmentAmount = loan.getCalculatedInstallmentAmount();

        LocalDate firstDueDate = YearMonth.from(LocalDate.now().plusMonths(1)).atDay(1);
        for (int i = 0; i < loan.getNumberOfInstallments(); i++) {
            LoanInstallment installment = new LoanInstallment();
            installment.setInstallmentAmount(singleInstallmentAmount);
            installment.setDueDate(firstDueDate.plusMonths(i));
            installment.setPaid(false);
            loan.addInstallment(installment);
        }
         customerService.updateUsedCreditLimit(customer, amount);
        Loan savedLoan = loanRepository.save(loan);
        return savedLoan;
    }


    @Transactional
    public LoanResponse createLoanbyUser(CreateUserLoanRequest request, Authentication authentication) {
        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (authenticatedUser.getRole() == Role.ROLE_CUSTOMER) {
            if (authenticatedUser.getCustomer() == null) {
                throw new AccessDeniedException("Customer user is not associated with a customer record.");
        }

        if (!AllowedInstallmentCounts.isValid(request.getNumberOfInstallments())) {
            throw new ValidationException("Number of installments can only be 6, 9, 12, or 24.");
        }

        if (request.getInterestRate().compareTo(BigDecimal.valueOf(0.1)) < 0 ||
                request.getInterestRate().compareTo(BigDecimal.valueOf(0.5)) > 0) {
            throw new ValidationException("Interest rate must be between 0.1 (10%) and 0.5 (50%).");
        }

        Customer customer = customerService.getCustomerById(authenticatedUser.getCustomer().getId());

         Loan savedLoan = createLoan(customer,request.getAmount(),request.getInterestRate(),request.getNumberOfInstallments());
            return mapToLoanResponse(savedLoan, true);
        }

        return LoanResponse.builder().build();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans(Optional<Long> customerIdOpt,
                                       Authentication authentication) {

        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        List<Loan> list=null;
        Predicate<Loan> filteredListLoans = null ;

        if (authenticatedUser.getRole() == Role.ROLE_CUSTOMER) {
            if (authenticatedUser.getCustomer() == null) {
                throw new AccessDeniedException("Customer user is not associated with a customer record.");
            }
            if (customerIdOpt.isPresent() && !customerIdOpt.get().equals(authenticatedUser.getCustomer().getId())) {
                throw new AccessDeniedException("You can only view your own loans.");
            }

            filteredListLoans = loan -> authenticatedUser.getCustomer().getId().equals(loan.getCustomer().getId());

            list=loanRepository.findAll().stream().filter(filteredListLoans).toList();

        } else if (authenticatedUser.getRole() == Role.ROLE_ADMIN) {
            list=loanRepository.findAll();

        }

        return list.stream()
                .map(loan -> mapToLoanResponse(loan, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long loanId, Authentication authentication) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        authorizeAccessToLoan(loan, authentication);
        return mapToLoanResponse(loan, true);
    }

    @Transactional(readOnly = true)
    public List<LoanInstallmentResponse> getInstallmentsByLoanId(Long loanId, Authentication authentication) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        authorizeAccessToLoan(loan, authentication);

        return loan.getInstallments().stream()
                .map(this::mapToLoanInstallmentResponse)
                .collect(Collectors.toList());
    }

    private void authorizeAccessToLoan(Loan loan, Authentication authentication) {
        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (authenticatedUser.getRole() == com.example.LoanAPIBackend.enums.Role.ROLE_CUSTOMER) {
            if (authenticatedUser.getCustomer() == null ||
                    !loan.getCustomer().getId().equals(authenticatedUser.getCustomer().getId())) {
                throw new AccessDeniedException("You do not have permission to access this loan's details.");
            }
        }
    }

    private LoanResponse mapToLoanResponse(Loan loan, boolean includeInstallments) {
        LoanResponse.LoanResponseBuilder builder = LoanResponse.builder()
                .id(loan.getId())
                .customerId(loan.getCustomer().getId())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .totalAmountWithInterest(loan.getTotalLoanAmountWithInterest())
                .numberOfInstallments(loan.getNumberOfInstallments())
                .createDate(loan.getCreateDate())
                .isPaid(loan.isPaid());

        if (includeInstallments) {
            builder.installments(loan.getInstallments().stream()
                    .map(this::mapToLoanInstallmentResponse)
                    .collect(Collectors.toList()));
        }
        return builder.build();
    }

    private LoanInstallmentResponse mapToLoanInstallmentResponse(LoanInstallment installment) {
        return LoanInstallmentResponse.builder()
                .id(installment.getId())
                .loanId(installment.getLoan().getId())
                .installmentAmount(installment.getInstallmentAmount())
                .paidAmount(installment.getPaidAmount())
                .dueDate(installment.getDueDate())
                .paymentDate(installment.getPaymentDate())
                .isPaid(installment.isPaid())
                .build();
    }
}
