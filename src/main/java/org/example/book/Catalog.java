package org.example.book;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Catalog {

    private final List<Book> books;

    private Catalog(List<Book> books) {
        this.books = books;
    }

    public static Catalog fromBooks(List<Book> books) {
        return new Catalog(books);
    }

    public List<Book> booksBy(String author) {
        return books.stream()
                .filter(book -> book.author().equalsIgnoreCase(author))
                .toList();
    }

    public List<Book> booksWithGenre(BookGenre bookGenre) {
        return books.stream()
                .filter((Book book) -> book.genre().equals(bookGenre))
                .toList();
    }

    public Map<BookGenre, List<Book>> booksGroupedByGenre() {
        return books.stream()
                .collect(Collectors.groupingBy(Book::genre));
    }

    public Map<BookGenre, Long> countByGenre() {
        return books.stream()
                .collect(Collectors.groupingBy(Book::genre, Collectors.counting()));
    }

    public List<Book> search(String query) {
        return books.stream()
                .filter(book -> book.matches(query))
                .toList();
    }

    public Optional<Book> findById(BookId id) {
        return books.stream()
                .filter(book -> book.id().equals(id))
                .findFirst();
    }
}
