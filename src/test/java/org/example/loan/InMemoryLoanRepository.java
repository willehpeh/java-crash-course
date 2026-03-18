package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLoanRepository implements LoanRepository {

    private final ConcurrentHashMap<MemberId, List<BookId>> loans = new ConcurrentHashMap<>();

    @Override
    public void save(MemberId memberId, BookId bookId) {
        loans.compute(memberId, (_, books) -> {
            if (books == null) {
                books = new ArrayList<>();
            }
            books.add(bookId);
            return books;
        });
    }

    @Override
    public void delete(MemberId memberId, BookId bookId) {
        loans.computeIfPresent(memberId, (_, books) -> {
            books.remove(bookId);
            return books.isEmpty() ? null : books;
        });
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
