package org.example.Loan;

import org.example.Book.BookId;
import org.example.MemberId;

import java.util.List;

public interface LoanRepository {
    void save(MemberId memberId, BookId bookId);

    void delete(MemberId memberId, BookId bookId);

    List<BookId> findBooksByMember(MemberId memberId);

    boolean isBookOnLoan(BookId bookId);
}
