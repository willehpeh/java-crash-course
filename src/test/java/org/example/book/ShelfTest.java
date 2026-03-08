package org.example.book;

import org.junit.jupiter.api.Test;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

public class ShelfTest {

    static final Book prideAndPrejudice = Book.of("Pride and Prejudice", "Jane Austen");
    static final Book emma = Book.of("Emma", "Jane Austen");
    static final Book nineteenEightyFour = Book.of("Nineteen Eighty-Four", "George Orwell");
    static final Book sapiens = Book.of("Sapiens", "Yuval Noah Harari");
    static final Book educatedMemoir = Book.of("Educated", "Tara Westover");
    static final Book oxfordDictionary = Book.of("Oxford English Dictionary", "Oxford University Press");

    @Test
    void shouldBeEmpty() {
        Shelf shelf = Shelf.sortedByTitle();
        assertThat(shelf.books()).isEmpty();
    }

    @Test
    void shouldOrderBooksAlphabeticallyByTitle() {
        Shelf shelf = Shelf.sortedByTitle(List.of(prideAndPrejudice, sapiens, emma));
        assertThat(shelf.books()).containsExactly(emma, prideAndPrejudice, sapiens);
    }

    @Test
    void shouldMaintainTitleSortWhenAdding() {
        Shelf shelf = Shelf.sortedByTitle();
        shelf.addBook(sapiens);
        shelf.addBook(emma);
        shelf.addBook(prideAndPrejudice);
        assertThat(shelf.books()).containsExactly(emma, prideAndPrejudice, sapiens);
    }

    @Test
    void shouldOrderBooksAlphabeticallyByAuthor() {
        Shelf shelf = Shelf.sortedByAuthor(List.of(prideAndPrejudice, sapiens, emma));
        assertThat(shelf.books()).containsExactly(emma, prideAndPrejudice, sapiens);
    }

    @Test
    void shouldMaintainAuthorSortWhenAdding() {
        Shelf shelf = Shelf.sortedByAuthor();
        shelf.addBook(sapiens);
        shelf.addBook(emma);
        shelf.addBook(prideAndPrejudice);
        assertThat(shelf.books()).containsExactly(emma, prideAndPrejudice, sapiens);
    }
}
