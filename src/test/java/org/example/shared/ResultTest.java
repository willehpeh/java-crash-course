package org.example.shared;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {
    @Nested
    class SuccessTest {
        @Test
        void shouldBeSuccess() {
            Success<String> success = new Success<>(null);
            assertThat(success.isSuccess()).isTrue();
        }
        @Test
        void shouldGetValue() {
            Success<String> success = new Success<>("Value");
            String defaultValue = "Default";
            assertThat(success.getOrElse(defaultValue)).isEqualTo("Value");
        }
        @Test
        void shouldTransformTheValue() {
            Success<String> success = new Success<>("Value");
            assertThat(success.map(String::toUpperCase).getOrElse("")).isEqualTo("VALUE");
        }
    }

    @Nested
    class FailureTest {
        @Test
        void shouldBeFailure() {
            Failure<String> failure = new Failure<>("Message");
            assertThat(failure.isSuccess()).isFalse();
        }
        @Test
        void shouldReturnDefaultValue() {
            Failure<String> failure = new Failure<>("Message");
            String defaultValue = "Default";
            assertThat(failure.getOrElse(defaultValue)).isEqualTo(defaultValue);
        }
        @Test
        void shouldNotTransformTheValue() {
            Failure<String> failure = new Failure<>("Message");
            assertThat(failure.map(String::toUpperCase)).isEqualTo(failure);
        }
    }
}
