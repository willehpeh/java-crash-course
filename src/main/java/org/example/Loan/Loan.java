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
        status = status.transitionTo(RETURNED);
    }

    public void markAsOverdue() {
        status = status.transitionTo(OVERDUE);
    }
}

