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
        var memberId = new MemberId("member1");
        service.borrowBook(memberId, new BookId("book1"));

        assertThat(loanRepository.findBooksByMember(memberId)).containsExactly(new BookId("book1"));
    }

    @Test
    void shouldGetLoansForMember() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        var bookId2 = new BookId("book2");
        loanRepository.save(memberId, bookId);
        loanRepository.save(memberId, bookId2);

        assertThat(service.loansFor(new MemberId("member1"))).containsExactly(bookId, bookId2);
    }

    @Test
    void shouldReturnBook() {
        var memberId = new MemberId("member1");
        var bookId = new BookId("book1");
        service.borrowBook(memberId, bookId);
        service.returnBook(memberId, bookId);

        assertThat(loanRepository.findBooksByMember(memberId)).isEmpty();
    }

    @Test
    void shouldNotBorrowBookAlreadyOnLoan() {
        service.borrowBook(new MemberId("member1"), new BookId("book1"));

        assertThatThrownBy(() -> service.borrowBook(new MemberId("member2"), new BookId("book1")));
    }

    @Test
    void shouldNotReturnBookNotOnLoan() {
        assertThatThrownBy(() -> service.returnBook(new MemberId("member1"), new BookId("book1")));
    }

    @Test
    void shouldEnforceBorrowingLimit() {
        service.borrowBook(new MemberId("member1"), new BookId("book1"));
        service.borrowBook(new MemberId("member1"), new BookId("book2"));
        service.borrowBook(new MemberId("member1"), new BookId("book3"));

        assertThatThrownBy(() -> service.borrowBook(new MemberId("member1"), new BookId("book4")));
    }
}
