package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;

import static org.example.loan.LoanStatus.*;

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

