package org.example;

import java.util.List;

public class LoanReceipt {
    private final MemberId memberId;
    private final List<BookId> bookIds;
    private String receiptText;

    LoanReceipt(MemberId memberId, List<BookId> bookIds) {
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
        receiptText = sb.toString();
        return receiptText;
    }
}
