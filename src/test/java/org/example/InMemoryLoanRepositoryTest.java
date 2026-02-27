package org.example;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryLoanRepositoryTest {
    InMemoryLoanRepository repository = new InMemoryLoanRepository();

    @Test
    void shouldRecordALoan() {
        repository.save("member1", "book1");
        assertThat(repository.findBooksByMember("member1")).containsExactly("book1");
    }

    @Test
    void shouldReturnEmptyListForMemberWithNoLoans() {
        assertThat(repository.findBooksByMember("empty-member")).isEmpty();
    }

    @Test
    void shouldTrackMultipleLoansPerMember() {
        repository.save("member1", "book1");
        repository.save("member1", "book2");
        assertThat(repository.findBooksByMember("member1")).containsExactlyInAnyOrder("book1", "book2");
    }

    @Test
    void shouldKnowWhenBookIsOnLoan() {
        repository.save("member1", "book1");
        assertThat(repository.isBookOnLoan("book1")).isTrue();
    }

    @Test
    void shouldRemoveLoanOnDelete() {
        repository.save("member1", "book1");
        repository.delete("member1", "book1");
        assertThat(repository.findBooksByMember("member1")).isEmpty();
        assertThat(repository.isBookOnLoan("book1")).isFalse();
    }
}
