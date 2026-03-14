package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoanFileTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadSerializedLoans() throws IOException {
        Loan loan1 = new Loan(new MemberId("member-1"), new BookId("book-1"));
        Loan loan2 = new Loan(new MemberId("member-2"), new BookId("book-2"));
        Loan loan3 = new Loan(new MemberId("member-3"), new BookId("book-3"));
        String serialized1 = loan1.toString();
        String serialized2 = loan2.toString();
        String serialized3 = loan3.toString();
        Files.writeString(tempDir.resolve("loans.txt"), serialized1 + "\n" + serialized2 + "\n" + serialized3);
        List<String> lines = Files.readAllLines(tempDir.resolve("loans.txt"));
        List<Loan> retrievedLoans = lines.stream()
                .map(Loan::fromString)
                .toList();
        assertEquals(List.of(loan1, loan2, loan3), retrievedLoans);
    }
}
