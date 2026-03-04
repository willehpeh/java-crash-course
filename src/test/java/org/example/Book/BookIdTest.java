package org.example.Book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookIdTest {
    @Test
    void shouldBeEqual() {
        var bookId1 = new BookId("123");
        var bookId2 = new BookId("123");
        assertThat(bookId1).isEqualTo(bookId2);
    }

    @Test
    void shouldNotBeEqual() {
        var bookId1 = new BookId("123");
        var bookId2 = new BookId("456");
        assertThat(bookId1).isNotEqualTo(bookId2);
    }

    @Test
    void shouldContainValue() {
        var bookId = new BookId("123");
        assertThat(bookId.toString()).contains("123");
    }

    @Test
    void shouldNotAllowBlankValue() {
        assertThatThrownBy(() -> new BookId("  "));
    }

    @Test
    void shouldNotAllowNull() {
        assertThatThrownBy(() -> new BookId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
