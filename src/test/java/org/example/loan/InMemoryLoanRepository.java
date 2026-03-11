package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;

import java.util.*;

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

    @Override
    public Optional<MemberId> borrowerOfBook(BookId bookId) {
        return loans.entrySet().stream()
                .filter(entry -> entry.getValue().contains(bookId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public List<BookId> allBooksOnLoan() {
        return loans.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
