package org.example.lending;

import org.example.book.BookId;
import org.example.MemberId;

public class EventSerializer {

    public String serialize(LibraryEvent event) {
        String prefix = switch (event) {
            case BookAdded _ -> "BOOK_ADDED";
            case BookBorrowed _ -> "BOOK_BORROWED";
            case BookReturned _ -> "BOOK_RETURNED";
        };
        return String.join("|", prefix, event.asText());
    }

    public LibraryEvent deserialize(String serialized) {
        String[] parts = serialized.split("\\|");
        String type = parts[0];
        return switch (type) {
            case "BOOK_ADDED" -> new BookAdded(new BookId(parts[1]), parts[2], parts[3]);
            case "BOOK_BORROWED" -> new BookBorrowed(new MemberId(parts[1]), new BookId(parts[2]));
            case "BOOK_RETURNED" -> new BookReturned(new MemberId(parts[1]), new BookId(parts[2]));
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
