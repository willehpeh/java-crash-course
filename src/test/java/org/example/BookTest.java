package org.example;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookTest {
    @Nested
    class Title {
        @Test
        void shouldNotAllowBlank() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> new Book(bookId, "  ", "Will Alexander"));
        }
        @Test
        void shouldNotAllowNull() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> new Book(bookId, null, "Will Alexander"));
        }
        @Test
        void shouldAllow200Characters() {
            BookId bookId = new BookId("123");
            Book book = new Book(bookId, "A".repeat(200), "Will Alexander");
            assertThat(book).isInstanceOf(Book.class);
        }
        @Test
        void shouldNotExceed200Characters() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> new Book(bookId, "A".repeat(201), "Will Alexander"));
        }
    }
    @Nested
    class Author {
        @Test
        void shouldNotAllowBlank() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> new Book(bookId, "The Great Gatsby", "  "));
        }
        @Test
        void shouldNotAllowNull() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> new Book(bookId, "The Great Gatsby", null));
        }
    }
}
