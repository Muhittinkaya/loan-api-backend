package com.example.LoanAPIBackend.service;

import com.example.LoanAPIBackend.exception.ResourceNotFoundException;
import com.example.LoanAPIBackend.model.Customer;
import com.example.LoanAPIBackend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    @Transactional
    public void updateUsedCreditLimit(Customer customer, BigDecimal amountChange) {
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(amountChange));
        customerRepository.save(customer);
    }
}