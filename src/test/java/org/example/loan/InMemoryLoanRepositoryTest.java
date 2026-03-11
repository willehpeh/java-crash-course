package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryLoanRepositoryTest {
    InMemoryLoanRepository repository = new InMemoryLoanRepository();

    @Test
    void shouldRecordALoan() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        repository.save(memberId, bookId);
        assertThat(repository.findBooksByMember(memberId)).containsExactly(bookId);
    }

    @Test
    void shouldReturnEmptyListForMemberWithNoLoans() {
        assertThat(repository.findBooksByMember(new MemberId("empty-member"))).isEmpty();
    }

    @Test
    void shouldTrackMultipleLoansPerMember() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        var bookId2 = new BookId("book2");
        repository.save(memberId, bookId);
        repository.save(memberId, bookId2);
        assertThat(repository.findBooksByMember(memberId)).containsExactlyInAnyOrder(bookId, bookId2);
    }

    @Test
    void shouldKnowWhenBookIsOnLoan() {
        var bookId = new BookId("book1");
        repository.save(new MemberId("member1"), bookId);
        assertThat(repository.isBookOnLoan(bookId)).isTrue();
    }

    @Test
    void shouldRemoveLoanOnDelete() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        repository.save(memberId, bookId);
        repository.delete(memberId, bookId);
        assertThat(repository.findBooksByMember(memberId)).isEmpty();
        assertThat(repository.isBookOnLoan(bookId)).isFalse();
    }

    @Test
    void shouldFindBorrowerOfBook() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        repository.save(memberId, bookId);
        assertThat(repository.borrowerOfBook(bookId).orElseThrow()).isEqualTo(memberId);
    }

    @Test
    void shouldReturnEmptyWhenBookIsNotOnLoan() {
        assertThat(repository.borrowerOfBook(new BookId("book1"))).isEmpty();
    }
    @Test
    void shouldReturnAllBooksOnLoan() {
        var memberId = new MemberId("member1");
        var memberId2 = new MemberId("member2");
        var bookId = new BookId("book1");
        var bookId2 = new BookId("book2");
        var bookId3 = new BookId("book3");
        repository.save(memberId, bookId);
        repository.save(memberId, bookId2);
        repository.save(memberId2, bookId3);

        assertThat(repository.allBooksOnLoan()).containsExactlyInAnyOrder(bookId, bookId2, bookId3);
    }
}
