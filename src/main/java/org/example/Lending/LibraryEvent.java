package org.example.Lending;

public sealed interface LibraryEvent permits BookAdded, BookBorrowed, BookReturned {
    String asText();
}
