package org.example.lending;

import org.example.book.BookId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventSerializerTest {
    public static final String TEST_BOOK_ID = "book-1";
    public static final String TEST_BOOK_TITLE = "The Great Gatsby";
    public static final String TEST_BOOK_AUTHOR = "F. Scott Fitzgerald";
    public static final String TEST_MEMBER_ID = "member-1";
    EventSerializer eventSerializer = new EventSerializer();

    @Test
    void shouldSerializeBookAdded() {
        BookAdded event = new BookAdded(new BookId(TEST_BOOK_ID), TEST_BOOK_TITLE, TEST_BOOK_AUTHOR);
        String serialized = eventSerializer.serialize(event);
        assertThat(serialized).isEqualTo("BOOK_ADDED|%s|%s|%s".formatted(TEST_BOOK_ID, TEST_BOOK_TITLE, TEST_BOOK_AUTHOR));
    }

    @Test
    void shouldSerializeBookBorrowed() {
        BookBorrowed event = new BookBorrowed(new MemberId(TEST_MEMBER_ID), new BookId(TEST_BOOK_ID));
        String serialized = eventSerializer.serialize(event);
        assertThat(serialized).isEqualTo("BOOK_BORROWED|%s|%s".formatted(TEST_MEMBER_ID, TEST_BOOK_ID));
    }

    @Test
    void shouldSerializeBookReturned() {
        BookReturned event = new BookReturned(new MemberId(TEST_MEMBER_ID), new BookId(TEST_BOOK_ID));
        String serialized = eventSerializer.serialize(event);
        assertThat(serialized).isEqualTo("BOOK_RETURNED|%s|%s".formatted(TEST_MEMBER_ID, TEST_BOOK_ID));
    }

    @Test
    void shouldDeserializeBookAdded() {
        String serialized = "BOOK_ADDED|%s|%s|%s".formatted(TEST_BOOK_ID, TEST_BOOK_TITLE, TEST_BOOK_AUTHOR);
        LibraryEvent event = eventSerializer.deserialize(serialized);
        BookAdded expected = new BookAdded(new BookId(TEST_BOOK_ID), TEST_BOOK_TITLE, TEST_BOOK_AUTHOR);
        assertThat(event).isEqualTo(expected);
    }

    @Test
    void shouldDeserializeBookBorrowed() {
        String serialized = "BOOK_BORROWED|%s|%s".formatted(TEST_MEMBER_ID, TEST_BOOK_ID);
        LibraryEvent event = eventSerializer.deserialize(serialized);
        BookBorrowed expected = new BookBorrowed(new MemberId(TEST_MEMBER_ID), new BookId(TEST_BOOK_ID));
        assertThat(event).isEqualTo(expected);
    }

    @Test
    void shouldDeserializeBookReturned() {
        String serialized = "BOOK_RETURNED|%s|%s".formatted(TEST_MEMBER_ID, TEST_BOOK_ID);
        LibraryEvent event = eventSerializer.deserialize(serialized);
        BookReturned expected = new BookReturned(new MemberId(TEST_MEMBER_ID), new BookId(TEST_BOOK_ID));
        assertThat(event).isEqualTo(expected);
    }
}
