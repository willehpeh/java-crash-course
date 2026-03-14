package org.example.book;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.shared.Displayable;
import org.example.shared.Searchable;

import static java.util.UUID.randomUUID;

public class Book implements Searchable, Displayable {
    @JsonProperty private final BookId id;
    @JsonProperty private final String title;
    @JsonProperty private final String author;
    @JsonProperty final BookGenre genre;

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

    @JsonCreator
    public static Book of(
            @JsonProperty("id") BookId id,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("genre") BookGenre genre
    ) {
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

    @JsonIgnore
    public boolean isLongLoan() {
        return genre.maxLoanDays() > 14;
    }

    String title() {
        return title;
    }

    String author() {
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return id.equals(book.id) && title.equals(book.title) && author.equals(book.author) && genre == book.genre;
    }

    @Override
    public int hashCode() {
        return id.hashCode() + title.hashCode() + author.hashCode() + genre.hashCode();
    }
}

