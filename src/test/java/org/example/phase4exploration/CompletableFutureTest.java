package org.example.phase4exploration;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletableFutureTest {
    @Test
    void shouldSupplyAndApply() {
        var future = CompletableFuture.supplyAsync(() -> 42).thenApply(x -> x * 2);
        assertThat(future.join()).isEqualTo(84);
    }

    @Test
    void shouldCompose() {
        var future = CompletableFuture.supplyAsync(() -> 42).thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 2));
        assertThat(future.join()).isEqualTo(84);
    }

    @Test
    void shouldRunAll() {
        var future1 = CompletableFuture.supplyAsync(() -> 42);
        var future2 = CompletableFuture.supplyAsync(() -> 24);
        CompletableFuture.allOf(future1, future2).join();
        assertThat(future1.join()).isEqualTo(42);
        assertThat(future2.join()).isEqualTo(24);
    }

    @Test
    void shouldFallbackOnException() {
        var future = CompletableFuture.supplyAsync(() -> { throw new RuntimeException("oops"); })
                .exceptionally(e -> 42);
        assertThat(future.join()).isEqualTo(42);
    }
}
