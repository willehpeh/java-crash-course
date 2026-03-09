package org.example.book;

import org.example.shared.Displayable;
import org.example.shared.Searchable;

import static java.util.UUID.randomUUID;

public class Book implements Searchable, Displayable {
    private final BookId id;
    private final String title;
    private final String author;
    private final BookGenre genre;

    private Book(BookId id, String title, String author, BookGenre genre) {
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
        this.genre = genre;
    }

    public static Book of(String title, String author) {
        return Book.of(new BookId(randomUUID().toString()), title, author);
    }

    public static Book of(String title, String author, BookGenre genre) {
        return Book.of(new BookId(randomUUID().toString()), title, author, genre);
    }

    public static Book of(BookId id, String title, String author) {
        return Book.of(id, title, author, BookGenre.FICTION);
    }

    public static Book of(BookId id, String title, String author, BookGenre genre) {
        return new Book(id, title, author, genre);
    }

    @Override
    public String searchableText() {
        return title + " " + author;
    }

    @Override
    public String display() {
        return title + " by " + author;
    }

    public BookId id() {
        return id;
    }

    public BookGenre genre() {
        return genre;
    }

    public String genreDisplayName() {
        return genre.displayName();
    }

    public boolean isLongLoan() {
        return genre.maxLoanDays() > 14;
    }

    String title() {
        return title;
    }

    String author() {
        return author;
    }
}

