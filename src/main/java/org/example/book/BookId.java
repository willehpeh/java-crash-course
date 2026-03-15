package org.example.book;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record BookId(@JsonValue String value) {
    @JsonCreator
    public BookId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BookId cannot be null");
        }
    }
}
