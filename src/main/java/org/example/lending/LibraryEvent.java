package org.example.lending;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookAdded.class, name = "BOOK_ADDED"),
        @JsonSubTypes.Type(value = BookBorrowed.class, name = "BOOK_BORROWED"),
        @JsonSubTypes.Type(value = BookReturned.class, name = "BOOK_RETURNED")
})
public sealed interface LibraryEvent permits BookAdded, BookBorrowed, BookReturned {
    String asText();
}
