package org.example.phase3exploration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.book.Book;
import org.example.book.BookId;
import org.example.lending.*;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    @Test
    void shouldSerializeBookAddedEvent() throws JsonProcessingException {
        BookId bookId = new BookId("123");
        BookAdded bookAdded = new BookAdded(bookId, "The Great Gatsby", "Will Alexander");
        String json = mapper.writeValueAsString(bookAdded);
        assertThat(json).contains("\"type\":\"BOOK_ADDED\"");
    }
    @Test
    void shouldRoundTripBookAddedEvent() throws JsonProcessingException {
        BookId bookId = new BookId("123");
        BookAdded bookAdded = new BookAdded(bookId, "The Great Gatsby", "Will Alexander");
        String json = mapper.writeValueAsString(bookAdded);
        BookAdded roundTrip = mapper.readValue(json, BookAdded.class);
        assertThat(roundTrip).isEqualTo(bookAdded);
    }

    @Test
    void shouldRoundTripListOfEvents() throws JsonProcessingException {
        BookId bookId = new BookId("123");
        BookAdded bookAdded = new BookAdded(bookId, "The Great Gatsby", "Will Alexander");
        BookBorrowed bookBorrowed = new BookBorrowed(new MemberId("member1"), bookId);
        BookReturned bookReturned = new BookReturned(new MemberId("member1"), bookId);
        List<LibraryEvent> events = List.of(bookAdded, bookBorrowed, bookReturned);
        String json = mapper
                .writerFor(new TypeReference<List<LibraryEvent>>() {})
                .writeValueAsString(events);
        System.out.println(json);
        List<LibraryEvent> roundTrip = mapper.readValue(json, new TypeReference<>() {});
        assertThat(roundTrip).containsExactly(bookAdded, bookBorrowed, bookReturned);
    }
}