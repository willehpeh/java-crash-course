package org.example.Lending;

import org.example.Book.BookId;
import org.example.MemberId;

public record BookBorrowed(MemberId memberId, BookId bookId) implements LibraryEvent {
    @Override
    public String asText() {
        return String.join("|", memberId.value(), bookId.value());
    }
}
