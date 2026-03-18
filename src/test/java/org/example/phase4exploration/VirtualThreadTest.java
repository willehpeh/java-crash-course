package org.example.phase4exploration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualThreadTest {
    @Test
    void shouldCreateVirtualThreads() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        var threads = new ArrayList<Thread>();
        for (int i = 0; i < 1000; i++) {
            threads.add(Thread.ofVirtual().start(counter::incrementAndGet));
        }
        for (var thread : threads) {
            thread.join();
        }
        assertThat(counter.get()).isEqualTo(1000);
    }
}
