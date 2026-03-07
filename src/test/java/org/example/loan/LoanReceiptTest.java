package org.example.loan;

import org.example.book.BookId;
import org.example.MemberId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoanReceiptTest {
    @Test
    void shouldDisplayMemberId() {
        MemberId memberId = new MemberId("member1");
        List<BookId> books = List.of(new BookId("book1"));
        LoanReceipt receipt = new LoanReceipt(memberId, books);
        assertThat(receipt.asText()).contains(memberId.value());
    }
    @Test
    void shouldDisplayTotalBooksOnLoan() {
        MemberId memberId = new MemberId("member1");
        List<BookId> books = List.of(new BookId("book1"), new BookId("book2"));
        LoanReceipt receipt = new LoanReceipt(memberId, books);
        assertThat(receipt.asText()).contains("Books on loan: 2");
    }
    @Test
    void shouldDisplayBookIds() {
        MemberId memberId = new MemberId("member1");
        List<BookId> books = List.of(new BookId("book1"), new BookId("book2"));
        LoanReceipt receipt = new LoanReceipt(memberId, books);
        String receiptText = receipt.asText();
        assertThat(receiptText).contains("book1");
        assertThat(receiptText).contains("book2");
    }
    @Test
    void shouldRejectEmptyBookList() {
        MemberId memberId = new MemberId("member1");
        List<BookId> books = List.of();
        assertThatThrownBy(() -> new LoanReceipt(memberId, books));
    }
}
