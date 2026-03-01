package org.example;

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
        StringBuilder sb = new StringBuilder();
        sb.append("Loan Receipt\n");
        sb.append("Member ID: ").append(memberId.value()).append("\n");
        sb.append("Books on loan: ").append(bookIds.size()).append("\n");
        sb.append(bookIdListAsText());
        receiptText = sb.toString();
        return receiptText;
    }

    private String bookIdListAsText() {
        return bookIds.stream().map(BookId::value).reduce("", (acc, bookId) -> acc + "- " + bookId + "\n");
    }
}
