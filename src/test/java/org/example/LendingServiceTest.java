package org.example;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LendingServiceTest {
    LoanRepository loanRepository = new InMemoryLoanRepository();
    LendingServiceConfig config = new LendingServiceConfig(3);
    LendingService service = new LendingService(loanRepository, config);

    @Test
    void shouldBorrowBook() {
        service.borrowBook("member1", "book1");

        assertThat(loanRepository.findBooksByMember("member1")).containsExactly("book1");
    }

    @Test
    void shouldGetLoansForMember() {
        loanRepository.save("member1", "book1");
        loanRepository.save("member1", "book2");

        assertThat(service.loansFor("member1")).containsExactly("book1", "book2");
    }

    @Test
    void shouldReturnBook() {
        service.borrowBook("member1", "book1");
        service.returnBook("member1", "book1");

        assertThat(loanRepository.findBooksByMember("member1")).isEmpty();
    }

    @Test
    void shouldNotBorrowBookAlreadyOnLoan() {
        service.borrowBook("member1", "book1");

        assertThatThrownBy(() -> service.borrowBook("member2", "book1"));
    }

    @Test
    void shouldNotReturnBookNotOnLoan() {
        assertThatThrownBy(() -> service.returnBook("member1", "book1"));
    }

    @Test
    void shouldEnforceBorrowingLimit() {
        service.borrowBook("member1", "book1");
        service.borrowBook("member1", "book2");
        service.borrowBook("member1", "book3");

        assertThatThrownBy(() -> service.borrowBook("member1", "book4"));
    }
}
