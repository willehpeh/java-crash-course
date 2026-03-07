package org.example.book;

public record BookId(String value) {
    public BookId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BookId cannot be null");
        }
    }
}
