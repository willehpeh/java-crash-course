package org.example.lending;

import org.example.book.BookId;
import org.example.loan.LoanRepository;
import org.example.MemberId;

import java.util.List;

public class LendingService {

    private final LoanRepository loanRepository;
    private final int lendingLimit;

    public LendingService(LoanRepository loanRepository, LendingServiceConfig config) {
        this.loanRepository = loanRepository;
        lendingLimit = config.lendingLimit();
    }

    public void borrowBook(MemberId memberId, BookId bookId) {
        if (loanRepository.isBookOnLoan(bookId)) {
            throw new IllegalStateException("Book is already on loan");
        }
        if (loansFor(memberId).size() >= lendingLimit) {
            throw new IllegalStateException("Member has reached borrowing limit");
        }
        loanRepository.save(memberId, bookId);
    }

    public List<BookId> loansFor(MemberId memberId) {
        return loanRepository.findBooksByMember(memberId);
    }

    public void returnBook(MemberId memberId, BookId bookId) {
        loanRepository.delete(memberId, bookId);
    }
}
