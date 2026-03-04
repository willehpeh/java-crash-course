package org.example.Loan;

import org.example.Book.BookId;
import org.example.MemberId;

import java.util.List;

public class LoanReceipt {
    private final MemberId memberId;
    private final List<BookId> bookIds;
    private String receiptText;

    LoanReceipt(MemberId memberId, List<BookId> bookIds) {
        if (bookIds.isEmpty()) {
            throw new IllegalArgumentException("Book list cannot be empty");
        }
        this.memberId = memberId;
        this.bookIds = bookIds;
    }

    public String asText() {
        if (receiptText != null) {
            return receiptText;
        }
        receiptText = """
                Loan Receipt
                Member ID: %s
                Books on loan: %s
                %s
                """
                .formatted(memberId.value(), bookIds.size(), bookIdListAsText());
        return receiptText;
    }

    private String bookIdListAsText() {
        return bookIds.stream().map(BookId::value).reduce("", (acc, bookId) -> acc + "- " + bookId + "\n");
    }
}
