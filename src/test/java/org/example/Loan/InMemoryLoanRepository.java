package org.example.Loan;

import org.example.Book.BookId;
import org.example.MemberId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryLoanRepository implements LoanRepository {

    private final Map<MemberId, List<BookId>> loans = new HashMap<>();

    @Override
    public void save(MemberId memberId, BookId bookId) {
        loans.computeIfAbsent(memberId, _ -> new ArrayList<>()).add(bookId);
    }

    @Override
    public void delete(MemberId memberId, BookId bookId) {
        loans.getOrDefault(memberId, List.of()).remove(bookId);
    }

    @Override
    public List<BookId> findBooksByMember(MemberId memberId) {
        return loans.getOrDefault(memberId, List.of());
    }

    @Override
    public boolean isBookOnLoan(BookId bookId) {
        return loans.values().stream().anyMatch(books -> books.contains(bookId));
    }
}
