package org.example.lending;

import org.example.book.BookId;

public record BookAdded(BookId bookId, String title, String author) implements LibraryEvent {

    @Override
    public String asText() {
        return String.join("|", bookId.value(), title, author);
    }
}
