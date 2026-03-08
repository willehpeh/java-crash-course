package org.example.lending;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MemberIdTest {
    @Test
    void shouldNotAllowBlank() {
        assertThatThrownBy(() -> new MemberId("  "));
    }
    @Test
    void shouldNotAllowNull() {
        assertThatThrownBy(() -> new MemberId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
