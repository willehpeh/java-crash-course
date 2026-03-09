package org.example.book;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogTest {

    Catalog catalog = Catalog.fromBooks(TestBooks.allBooks);

    @Test
    void hasNoBooksForUnknownAuthor() {
        assertThat(catalog.booksBy("Unknown")).isEmpty();
    }
    @Test
    void findsBooksForKnownAuthor() {
        assertThat(catalog.booksBy("Jane Austen")).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice);
    }
    @Test
    void findsBooksOfRequestedGenre() {
        assertThat(catalog.booksWithGenre(BookGenre.FICTION)).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice, TestBooks.nineteenEightyFour);
    }
    @Test
    void showsAllGenres() {
        Map<BookGenre, List<Book>> groupedBooks = catalog.booksGroupedByGenre();
        assertThat(groupedBooks).containsOnlyKeys(BookGenre.values());
    }
    @Test
    void groupsBooksByGenre() {
        Map<BookGenre, List<Book>> groupedBooks = catalog.booksGroupedByGenre();
        assertThat(groupedBooks.get(BookGenre.FICTION)).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice, TestBooks.nineteenEightyFour);
        assertThat(groupedBooks.get(BookGenre.NON_FICTION)).containsExactlyInAnyOrder(TestBooks.sapiens, TestBooks.educated, TestBooks.thinkingFastAndSlow);
        assertThat(groupedBooks.get(BookGenre.REFERENCE)).containsExactlyInAnyOrder(TestBooks.oxfordDictionary, TestBooks.merriamWebster, TestBooks.chicagoManualOfStyle);
    }
    @Test
    void countsBooksByGenre() {
        Map<BookGenre, Long> countsByGenre = catalog.countByGenre();
        assertThat(countsByGenre.get(BookGenre.FICTION)).isEqualTo(3L);
        assertThat(countsByGenre.get(BookGenre.NON_FICTION)).isEqualTo(3L);
        assertThat(countsByGenre.get(BookGenre.REFERENCE)).isEqualTo(3L);
    }
}
