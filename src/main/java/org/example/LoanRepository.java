package org.example;

import java.util.List;

public interface LoanRepository {
    void save(MemberId memberId, BookId bookId);

    void delete(MemberId memberId, BookId bookId);

    List<BookId> findBooksByMember(MemberId memberId);

    boolean isBookOnLoan(BookId bookId);
}
