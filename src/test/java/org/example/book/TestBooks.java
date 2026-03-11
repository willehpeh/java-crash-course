package org.example.book;

import java.util.List;

public final class TestBooks {

    public static final Book prideAndPrejudice = Book.of("Pride and Prejudice", "Jane Austen", BookGenre.FICTION);
    public static final Book emma = Book.of("Emma", "Jane Austen", BookGenre.FICTION);
    public static final Book nineteenEightyFour = Book.of("Nineteen Eighty-Four", "George Orwell", BookGenre.FICTION);

    public static final Book sapiens = Book.of("Sapiens", "Yuval Noah Harari", BookGenre.NON_FICTION);
    public static final Book educated = Book.of("Educated", "Tara Westover", BookGenre.NON_FICTION);
    public static final Book thinkingFastAndSlow = Book.of("Thinking, Fast and Slow", "Daniel Kahneman", BookGenre.NON_FICTION);

    public static final Book oxfordDictionary = Book.of("Oxford English Dictionary", "Oxford University Press", BookGenre.REFERENCE);
    public static final Book merriamWebster = Book.of("Merriam-Webster's Dictionary", "Merriam-Webster", BookGenre.REFERENCE);

    public static final List<Book> allBooks = List.of(
            prideAndPrejudice, emma, nineteenEightyFour,
            sapiens, educated, thinkingFastAndSlow,
            oxfordDictionary, merriamWebster
    );

    private TestBooks() {}
}
