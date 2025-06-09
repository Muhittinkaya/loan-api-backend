package com.example.LoanAPIBackend.service;

import com.example.LoanAPIBackend.dto.LoanResponse;
import com.example.LoanAPIBackend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import com.example.LoanAPIBackend.exception.ResourceNotFoundException;
import com.example.LoanAPIBackend.model.Customer;
import com.example.LoanAPIBackend.model.Loan;
import com.example.LoanAPIBackend.model.User;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoanService loanService;

    private User adminUser;
    private User customerUser1;
    private User customerUser2;
    private Customer customer1;
    private Customer customer2;
    private Loan loan1;
    private Loan loan2;
    private Loan loan3;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ROLE_ADMIN);

        customer1 = new Customer();
        customer1.setId(101L);

        customerUser1 = new User();
        customerUser1.setId(2L);
        customerUser1.setUsername("customer1");
        customerUser1.setRole(Role.ROLE_CUSTOMER);
        customerUser1.setCustomer(customer1);

        customer2 = new Customer();
        customer2.setId(102L);

        customerUser2 = new User();
        customerUser2.setId(3L);
        customerUser2.setUsername("customer2");
        customerUser2.setRole(Role.ROLE_CUSTOMER);
        customerUser2.setCustomer(customer2);

        // === Loan Setup ===
        loan1 = new Loan();
        loan1.setId(1001L);
        loan1.setCustomer(customer1);
        loan1.setLoanAmount(new BigDecimal("5000"));
        loan1.setInterestRate(new BigDecimal("0.15"));
        loan1.setNumberOfInstallments(12);

        loan2 = new Loan();
        loan2.setId(1002L);
        loan2.setCustomer(customer1);
        loan2.setLoanAmount(new BigDecimal("2000"));
        loan2.setInterestRate(new BigDecimal("0.20"));
        loan2.setNumberOfInstallments(6);

        loan3 = new Loan();
        loan3.setId(1003L);
        loan3.setCustomer(customer2);
        loan3.setLoanAmount(new BigDecimal("10000"));
        loan3.setInterestRate(new BigDecimal("0.10"));
        loan3.setNumberOfInstallments(24);
    }


    @Test
    void getLoans_AsAdmin_NoFilters_ReturnsAllLoans() {
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(loanRepository.findAll()).thenReturn(List.of(loan1, loan2, loan3));


        List<LoanResponse> result = loanService.getLoans( Optional.empty(), authentication);


        assertNotNull(result);
        assertEquals(3, result.size(), "Admin should see all loans");
        verify(loanRepository, times(1)).findAll();
    }

    @Test
    void getLoans_AsAdmin_WithCustomerIdFilter_ReturnsAllLoansDueToCurrentImplementation() {
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(loanRepository.findAll()).thenReturn(List.of(loan1, loan2, loan3));

        List<LoanResponse> result = loanService.getLoans(Optional.of(101L),  authentication);

        assertEquals(3, result.size(), "Admin filter is not applied in the current code, so all loans should be returned");
    }

    @Test
    void getLoans_AsCustomer_ReturnsOnlyOwnLoans() {

        when(authentication.getName()).thenReturn("customer1");
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customerUser1));
        when(loanRepository.findAll()).thenReturn(List.of(loan1, loan2, loan3));

        List<LoanResponse> result = loanService.getLoans( Optional.empty(), authentication);

        assertNotNull(result);
        assertEquals(2, result.size(), "Customer should only see their own loans");
        assertTrue(result.stream().allMatch(loan -> loan.getCustomerId().equals(customer1.getId())), "All returned loans must belong to customer1");
    }

    @Test
    void getLoans_AsCustomer_WithAnotherCustomerId_ThrowsAccessDeniedException() {
        Long otherCustomerId = customer2.getId();
        when(authentication.getName()).thenReturn("customer1");
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(customerUser1));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            loanService.getLoans(Optional.of(otherCustomerId),  authentication);
        });

        assertEquals("You can only view your own loans.", exception.getMessage());
    }


    @Test
    void getLoans_WhenUserNotFound_ThrowsResourceNotFoundException() {
        when(authentication.getName()).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            loanService.getLoans(Optional.empty(), authentication);
        });

        assertEquals("Authenticated user not found.", exception.getMessage());
    }
}
