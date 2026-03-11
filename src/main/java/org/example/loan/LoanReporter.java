package org.example.loan;

import org.example.book.Book;
import org.example.book.BookGenre;
import org.example.book.Catalog;
import org.example.lending.MemberId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LoanReporter {

    private final LoanRepository repository;
    private final Catalog catalog;

    public LoanReporter(LoanRepository repository, Catalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public List<Book> booksOnLoanTo(MemberId memberId) {
        return repository.findBooksByMember(memberId).stream()
                .map(catalog::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    public Map<BookGenre, Long> loanCountByGenre() {
        return repository.allBooksOnLoan().stream()
                .map(catalog::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(Book::genre, Collectors.counting()));
    }

    public Optional<BookGenre> mostPopularGenre() {
        return loanCountByGenre().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}
