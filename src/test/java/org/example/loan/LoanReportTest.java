package org.example.loan;

import org.example.book.Catalog;
import org.example.book.TestBooks;
import org.example.lending.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoanReportTest {

    static final MemberId alice = new MemberId("alice");
    static final MemberId bob = new MemberId("bob");

    LoanRepository repository = new InMemoryLoanRepository();
    Catalog catalog = Catalog.fromBooks(TestBooks.allBooks);

    @BeforeEach
    void setUp() {
        repository.save(alice, TestBooks.emma.id());
        repository.save(alice, TestBooks.sapiens.id());
        repository.save(alice, TestBooks.oxfordDictionary.id());

        repository.save(bob, TestBooks.nineteenEightyFour.id());
        repository.save(bob, TestBooks.educated.id());
    }

    @Test
    void showsBooksOnLoanToMember() {

    }
}
