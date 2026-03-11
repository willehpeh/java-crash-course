package org.example.book;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    void groupsBooksByGenre() {
        Map<BookGenre, List<Book>> groupedBooks = catalog.booksGroupedByGenre();
        assertThat(groupedBooks).containsOnlyKeys(BookGenre.values());
        assertThat(groupedBooks.get(BookGenre.FICTION)).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice, TestBooks.nineteenEightyFour);
        assertThat(groupedBooks.get(BookGenre.NON_FICTION)).containsExactlyInAnyOrder(TestBooks.sapiens, TestBooks.educated, TestBooks.thinkingFastAndSlow);
        assertThat(groupedBooks.get(BookGenre.REFERENCE)).containsExactlyInAnyOrder(TestBooks.oxfordDictionary, TestBooks.merriamWebster);
    }
    @Test
    void countsBooksByGenre() {
        Map<BookGenre, Long> countsByGenre = catalog.countByGenre();
        assertThat(countsByGenre.get(BookGenre.FICTION)).isEqualTo(3L);
        assertThat(countsByGenre.get(BookGenre.NON_FICTION)).isEqualTo(3L);
        assertThat(countsByGenre.get(BookGenre.REFERENCE)).isEqualTo(2L);
    }
    @Test
    void matchesBooksByTitle() {
        List<Book> found = catalog.search("Pride and Prejudice");
        assertThat(found).containsExactly(TestBooks.prideAndPrejudice);
    }
    @Test
    void matchesBooksByAuthor() {
        List<Book> found = catalog.search("Jane Austen");
        assertThat(found).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice);
    }
    @Test
    void matchesBooksInsensitiveToCase() {
        List<Book> found = catalog.search("jane austen");
        assertThat(found).containsExactlyInAnyOrder(TestBooks.emma, TestBooks.prideAndPrejudice);
    }
    @Test
    void hasNoBooksForUnmatchedQuery() {
        List<Book> found = catalog.search("unknown");
        assertThat(found).isEmpty();
    }
    @Test
    void findsBookWithId() {
        Optional<Book> found = catalog.findById(TestBooks.nineteenEightyFour.id());
        assertThat(found).contains(TestBooks.nineteenEightyFour);
    }
    @Test
    void findsNothingForUnknownId() {
        Optional<Book> notFound = catalog.findById(new BookId("unknown"));
        assertThat(notFound).isEmpty();
    }
}
