package org.example.lending;

public sealed interface LibraryEvent permits BookAdded, BookBorrowed, BookReturned {
    String asText();
}
