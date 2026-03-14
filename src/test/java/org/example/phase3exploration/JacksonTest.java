package org.example.phase3exploration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.book.Book;
import org.example.book.BookId;
import org.example.lending.BookBorrowed;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JacksonTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldRoundTripBookId() throws JsonProcessingException {
        BookId bookId = new BookId("123");
        String json = mapper.writeValueAsString(bookId);
        BookId roundTrip = mapper.readValue(json, BookId.class);
        assertThat(roundTrip).isEqualTo(bookId);
    }

    @Test
    void shouldRoundTripBookBorrowed() throws JsonProcessingException {
        BookId bookId = new BookId("123");
        MemberId memberId = new MemberId("member1");
        BookBorrowed bookBorrowed = new BookBorrowed(memberId, bookId);
        String json = mapper.writeValueAsString(bookBorrowed);
        BookBorrowed roundTrip = mapper.readValue(json, BookBorrowed.class);
        assertThat(roundTrip).isEqualTo(bookBorrowed);
    }

    @Test
    void shouldRoundTripBook() throws JsonProcessingException {
        Book book = Book.of("The Great Gatsby", "Will Alexander");
        String json = mapper.writeValueAsString(book);
        Book roundTrip = mapper.readValue(json, Book.class);
        assertThat(roundTrip).isEqualTo(book);
    }

    @Test
    void shouldFailToDeserializeInvalidBook() {
        String json = "{\"id\":{\"value\":\"123\"},\"title\":\"\",\"author\":\"Will Alexander\",\"genre\":\"FICTION\"}";
        assertThatThrownBy(() -> mapper.readValue(json, Book.class));
    }
}
