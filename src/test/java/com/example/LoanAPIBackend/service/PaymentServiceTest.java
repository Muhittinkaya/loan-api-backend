package com.example.LoanAPIBackend.service;

import com.example.LoanAPIBackend.dto.PaymentResponse;
import com.example.LoanAPIBackend.enums.Role;
import com.example.LoanAPIBackend.exception.PaymentException;
import com.example.LoanAPIBackend.model.Customer;
import com.example.LoanAPIBackend.model.Loan;
import com.example.LoanAPIBackend.model.LoanInstallment;
import com.example.LoanAPIBackend.model.User;
import com.example.LoanAPIBackend.repository.LoanInstallmentRepository;
import com.example.LoanAPIBackend.repository.LoanRepository;
import com.example.LoanAPIBackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentService paymentService;

    private User customerUser;
    private Loan loan;
    private LoanInstallment installment;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer(1L, "John", "Doe", new BigDecimal("10000"), new BigDecimal("1000"));
        customerUser = new User(1L, "johndoe", "password", Role.ROLE_CUSTOMER, customer);
        loan = new Loan(1L, customer, new BigDecimal("1200"), new BigDecimal("0.10"), 12, LocalDate.now(), false, null); // Listeyi null veya boş bırakabiliriz, testte mockluyoruz.

        installment = new LoanInstallment();
        installment.setId(101L);
        installment.setLoan(loan);
        installment.setInstallmentAmount(new BigDecimal("110.00"));
        installment.setDueDate(LocalDate.now().plusMonths(1));
        installment.setPaid(false);
    }

    @Test
    void payLoanInstallments_WhenPaymentSufficient_ShouldPayInstallmentAndReturnSuccess() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(loan));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(customerUser));
        when(authentication.getName()).thenReturn("johndoe");

        ArrayList<LoanInstallment> installmentList = new ArrayList<>();
        installmentList.add(installment);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(anyLong()))
                .thenReturn(installmentList);
        when(loanInstallmentRepository.countByLoanIdAndIsPaidFalse(anyLong())).thenReturn(0L);
        doNothing().when(customerService).updateUsedCreditLimit(any(Customer.class), any(BigDecimal.class));

        BigDecimal paymentAmount = new BigDecimal("150.00");

        PaymentResponse response = paymentService.payLoanInstallments(1L, paymentAmount, authentication);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaidCount());
        assertTrue(response.isLoanFullyPaid());
        assertEquals("1 installment(s) paid successfully. The loan is now fully paid.", response.getMessage());
        assertTrue(response.getRemainingPaymentAmount().compareTo(new BigDecimal("40.00")) == 0);

        verify(loanInstallmentRepository, times(1)).save(any(LoanInstallment.class));
        verify(customerService, times(1)).updateUsedCreditLimit(any(Customer.class), any(BigDecimal.class));
        verify(loanRepository, times(1)).save(loan);
    }


    @Test
    void payLoanInstallments_WhenPaymentIsInsufficient_ShouldThrowPaymentException() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(loan));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(customerUser));
        when(authentication.getName()).thenReturn("johndoe");

        ArrayList<LoanInstallment> installmentList = new ArrayList<>();
        installmentList.add(installment);

        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseOrderByDueDateAsc(anyLong()))
                .thenReturn(installmentList);

        BigDecimal insufficientPaymentAmount = new BigDecimal("50.00");

        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.payLoanInstallments(1L, insufficientPaymentAmount, authentication);
        });

        assertTrue(exception.getMessage().contains("Payment amount is less than the earliest eligible due installment amount"));

        verify(loanInstallmentRepository, never()).save(any(LoanInstallment.class));
        verify(customerService, never()).updateUsedCreditLimit(any(Customer.class), any(BigDecimal.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }
}