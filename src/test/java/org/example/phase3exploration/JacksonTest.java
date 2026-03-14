package org.example.phase3exploration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.book.BookId;
import org.example.lending.BookBorrowed;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
