package org.example;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.LoanStatus.*;

public class LoanTest {
    @Nested
    class NewLoans {
        @Test
        void shouldBeActive() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");

            var loan = new Loan(memberId, bookId);
            assertThat(loan.status()).isEqualTo(ACTIVE);
        }
    }
    @Nested
    class ActiveLoans {
        @Test
        void canBeMarkedAsReturned() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");

            var loan = new Loan(memberId, bookId);
            loan.returnLoan();

            assertThat(loan.status()).isEqualTo(RETURNED);
        }
        @Test
        void canBeMarkedAsOverdue() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");

            var loan = new Loan(memberId, bookId);
            loan.markAsOverdue();

            assertThat(loan.status()).isEqualTo(OVERDUE);
        }
    }
    @Nested
    class OverdueLoans {
        @Test
        void canBeReturned() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");

            var loan = new Loan(memberId, bookId);
            loan.markAsOverdue();
            loan.returnLoan();
            assertThat(loan.status()).isEqualTo(RETURNED);
        }
        @Test
        void cannotBeMarkedAsOverdueAgain() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");

            var loan = new Loan(memberId, bookId);
            loan.markAsOverdue();
            assertThatThrownBy(loan::markAsOverdue);
        }
    }
    @Nested
    class ReturnedLoans {
        @Test
        void cannotBeReturnedAgain() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");
            var loan = new Loan(memberId, bookId);
            loan.returnLoan();
            assertThatThrownBy(loan::returnLoan);
        }
        @Test
        void cannotBeMarkedAsOverdue() {
            var memberId = new MemberId("member1");
            var bookId = new BookId("book1");
            var loan = new Loan(memberId, bookId);
            loan.returnLoan();
            assertThatThrownBy(loan::markAsOverdue);
        }
    }
}
