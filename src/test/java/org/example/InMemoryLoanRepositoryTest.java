package org.example;

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
}
