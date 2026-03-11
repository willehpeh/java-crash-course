package org.example.loan;

import org.example.book.Book;
import org.example.book.BookGenre;
import org.example.book.Catalog;
import org.example.book.TestBooks;
import org.example.lending.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanReporterTest {

    static final MemberId alice = new MemberId("alice");
    static final MemberId bob = new MemberId("bob");

    LoanRepository repository = new InMemoryLoanRepository();
    Catalog catalog = Catalog.fromBooks(TestBooks.allBooks);
    LoanReporter loanReporter = new LoanReporter(repository, catalog);

    @BeforeEach
    void setUp() {
        repository.save(alice, TestBooks.emma.id());
        repository.save(alice, TestBooks.sapiens.id());
        repository.save(alice, TestBooks.oxfordDictionary.id());

        repository.save(bob, TestBooks.nineteenEightyFour.id());
        repository.save(bob, TestBooks.prideAndPrejudice.id());
        repository.save(bob, TestBooks.educated.id());
    }

    @Test
    void showsBooksOnLoanToMember() {
        List<Book> booksOnLoan = loanReporter.booksOnLoanTo(alice);
        assertThat(booksOnLoan).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.sapiens, TestBooks.oxfordDictionary);
    }
    @Test
    void countsBooksOnLoanByGenre() {
        Map<BookGenre, Long> countsPerGenre = loanReporter.loanCountByGenre();
        assertThat(countsPerGenre.get(BookGenre.FICTION)).isEqualTo(3);
        assertThat(countsPerGenre.get(BookGenre.NON_FICTION)).isEqualTo(2);
        assertThat(countsPerGenre.get(BookGenre.REFERENCE)).isEqualTo(1);
    }
    @Test
    void findsMostPopularGenre() {
        assertThat(loanReporter.mostPopularGenre()).contains(BookGenre.FICTION);
    }
    @Test
    void hasNoMostPopularGenreIfNoBooksOnLoan() {
        var emptyReporter = new LoanReporter(new InMemoryLoanRepository(), catalog);
        assertThat(emptyReporter.mostPopularGenre()).isEmpty();
    }
}
