package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    void save(MemberId memberId, BookId bookId);

    void delete(MemberId memberId, BookId bookId);

    List<BookId> findBooksByMember(MemberId memberId);

    boolean isBookOnLoan(BookId bookId);

    Optional<MemberId> borrowerOfBook(BookId bookId);

    List<BookId> allBooksOnLoan();
}
