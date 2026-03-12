package org.example.loan;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanPersistenceExceptionTest {
    @Test
    void containsCorrectMessage() {
        LoanPersistenceException exception = new LoanPersistenceException("Test exception");
        assertThat(exception.getMessage()).isEqualTo("Test exception");
    }
    @Test
    void carriesCause() {
        IOException cause = new IOException("Test cause");
        LoanPersistenceException exception = new LoanPersistenceException("Test exception", cause);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
