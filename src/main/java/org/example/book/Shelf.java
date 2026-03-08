package org.example.book;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class Shelf {

    TreeSet<Book> books;

    private Shelf(List<Book> books, Comparator<Book> comparator) {
        this.books = new TreeSet<>(comparator);
        this.books.addAll(books);
    }

    public static Shelf sortedByTitle(List<Book> books) {
        return new Shelf(books, Comparator.comparing(Book::title));
    }
    public static Shelf sortedByTitle() {
        return Shelf.sortedByTitle(List.of());
    }

    public static Shelf sortedByAuthor(List<Book> books) {
        return new Shelf(books, Comparator
                .comparing((Book book) -> book.author().substring(book.author().lastIndexOf(' ') + 1))
                .thenComparing(Book::title)
        );
    }
    public static Shelf sortedByAuthor() {
        return Shelf.sortedByAuthor(List.of());
    }

    public List<Book> books() {
        return books.stream().toList();
    }

    public void addBook(Book book) {
        books.add(book);
    }
}
