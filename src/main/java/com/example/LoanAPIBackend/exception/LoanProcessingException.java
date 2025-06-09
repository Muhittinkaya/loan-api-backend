package com.example.LoanAPIBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LoanProcessingException extends RuntimeException {
    public LoanProcessingException(String message) {
        super(message);
    }

    public LoanProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}