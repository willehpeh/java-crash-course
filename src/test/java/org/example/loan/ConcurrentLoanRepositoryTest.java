package org.example.loan;

import org.example.book.BookId;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentLoanRepositoryTest {
    private final InMemoryLoanRepository repository = new InMemoryLoanRepository();

    @Test
    void shouldHitRaceConditionWithMultipleAddsForSameMember() {
        int threadCount = 100;
        var latch = new CountDownLatch(1);           // gate: initially closed
        var done = new CountDownLatch(threadCount);  // completion signal
        MemberId memberId = new MemberId("member-1");
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    latch.await();
                    BookId bookId = new BookId("book-%d".formatted(finalI));
                    repository.save(memberId, bookId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            }).start();
        }
        latch.countDown();
        try {
            done.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertThat(repository.findBooksByMember(memberId)).hasSize(threadCount);
    }
}
