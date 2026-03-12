package com.example.loanoriginationsystem.exception;

public class LoanOriginationException extends RuntimeException {
    public LoanOriginationException(String message) {
        super(message);
    }

    public LoanOriginationException(String message, Throwable cause) {
        super(message, cause);
    }
}
