package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanReceiptTest {
    @Test
    void shouldDisplayMemberId() {
        MemberId memberId = new MemberId("member1");
        List<BookId> books = List.of(new BookId("book1"));
        LoanReceipt receipt = new LoanReceipt(memberId, books);
        assertThat(receipt.asText()).contains(memberId.value());
    }
}
