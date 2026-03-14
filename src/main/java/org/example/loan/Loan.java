package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;

import static org.example.loan.LoanStatus.*;

public class Loan {

    private LoanStatus status;
    private final MemberId memberId;
    private final BookId bookId;

    public Loan(MemberId memberId, BookId bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
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

    @Override
    public String toString() {
        return String.join("|", memberId.value(), bookId.value(), status.toString());
    }

    public static Loan fromString(String inputString) {
        String[] parts = inputString.split("\\|");
        return new Loan(new MemberId(parts[0]), new BookId(parts[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return memberId.equals(loan.memberId) && bookId.equals(loan.bookId) && status == loan.status;
    }

    @Override
    public int hashCode() {
        return memberId.hashCode() + bookId.hashCode() + status.hashCode();
    }
}

