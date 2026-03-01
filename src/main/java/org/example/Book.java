package org.example;

import static java.util.UUID.randomUUID;

public class Book implements Searchable {
    private final BookId id;
    private String title;
    private String author;

    private Book(BookId id, String title, String author) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank or null");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("Title cannot be longer than 200 characters");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author cannot be blank or null");
        }
        this.id = id;
        this.title = title;
        this.author = author;
    }
    static Book of(String title, String author) {
        BookId id = new BookId(randomUUID().toString());
        return new Book(id, title, author);
    }
    static Book of(BookId id, String title, String author) {
        return new Book(id, title, author);
    }

    @Override
    public String searchableText() {
        return title + " " + author;
    }

    public BookId id() {
        return id;
    }
}
