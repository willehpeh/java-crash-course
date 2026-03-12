package org.example.loan;

public class LoanPersistenceException extends RuntimeException {
    public LoanPersistenceException(String message) {
        super(message);
    }
    public LoanPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
