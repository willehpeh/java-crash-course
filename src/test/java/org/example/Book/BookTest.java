package org.example.Book;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookTest {

    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Nested
    class Title {
        @Test
        void shouldNotAllowBlank() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> Book.of(bookId, "  ", "Will Alexander"));
        }
        @Test
        void shouldNotAllowNull() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> Book.of(bookId, null, "Will Alexander"));
        }
        @Test
        void shouldAllow200Characters() {
            BookId bookId = new BookId("123");
            Book book = Book.of(bookId, "A".repeat(200), "Will Alexander");
            assertThat(book).isInstanceOf(Book.class);
        }
        @Test
        void shouldNotExceed200Characters() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> Book.of(bookId, "A".repeat(201), "Will Alexander"));
        }
    }
    @Nested
    class Author {
        @Test
        void shouldNotAllowBlank() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> Book.of(bookId, "The Great Gatsby", "  "));
        }
        @Test
        void shouldNotAllowNull() {
            BookId bookId = new BookId("123");
            assertThatThrownBy(() -> Book.of(bookId, "The Great Gatsby", null));
        }
    }
    @Test
    void shouldCreateABookWithANewId() {
        Book book = Book.of("The Great Gatsby", "Will Alexander");
        assertThat(book.id().value()).matches(UUID_REGEX);
    }
    @Nested
    class Searching {
        @Test
        void shouldMatchQueryForTitle() {
            Book book = Book.of("The Greatest Gatsby", "Bill Alexander");
            assertThat(book.matches("The Greatest Gatsby")).isTrue();
        }
        @Test
        void shouldMatchQueryForAuthor() {
            Book book = Book.of("The Greatest Gatsby", "Bill Alexander");
            assertThat(book.matches("Bill Alexander")).isTrue();
        }
        @Test
        void shouldMatchCaseInsensitive() {
            Book book = Book.of("The Greatest Gatsby", "Bill Alexander");
            assertThat(book.matches("bill alExaNder")).isTrue();
        }
        @Test
        void shouldNotMatchUnrelatedQuery() {
            Book book = Book.of("The Greatest Gatsby", "Bill Alexander");
            assertThat(book.matches("Herp Derp")).isFalse();
        }
    }
    @Nested
    class Displaying {
        @Test
        void shouldDisplayHumanReadable() {
            Book book = Book.of("The Greatest Gatsby", "Bill Alexander");
            String displayText = book.display();
            assertThat(displayText).contains("The Greatest Gatsby");
            assertThat(displayText).contains("Bill Alexander");
        }
    }
}
