package org.example.Loan;

import org.example.Book.BookId;
import org.example.MemberId;

import static org.example.Loan.LoanStatus.*;

public class Loan {

    private LoanStatus status;

    public Loan(MemberId memberId, BookId bookId) {
        status = ACTIVE;
    }

    public LoanStatus status() {
        return status;
    }

    public void returnLoan() {
        if (!status.canTransitionTo(RETURNED)) {
            throw new IllegalStateException("Loan cannot be returned");
        }
        status = RETURNED;
    }

    public void markAsOverdue() {
        if (!status.canTransitionTo(OVERDUE)) {
            throw new IllegalStateException("Loan cannot be marked as overdue");
        }
        status = OVERDUE;
    }
}

