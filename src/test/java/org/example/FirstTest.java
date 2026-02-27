package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FirstTest {
    @Test
    void shouldAdd() {
        int result = 4 + 5;
        assertThat(result).isEqualTo(9);
    }

    @Test
    @DisplayName("My tests here")
    void shouldConcat() {

        String first = "thing";
        String second = "other thing";
        String result = first.concat(second);

        assertThat(result).isEqualTo("thingother thing");
        assertThat(result).hasSize(16);
    }

    @Test
    void shouldTestLists() {
        List<Character> list = List.of('a', 'b', 'c');
        assertThat(list).contains('b');
        assertThat(list).containsAll(List.of('b', 'c', 'a'));
    }

    @Test
    void shouldCheckRecordStuff() {
        var people = List.of(new Person("James", 23), new Person("John", 24), new Person("Jane", 25));
        assertThat(people).extracting(Person::age).containsAll(List.of(23, 24, 25));
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void shouldReverse(String input, String expected) {
        assertThat(new StringBuilder(input).reverse().toString()).isEqualTo(expected);
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("hello", "olleh"),   // 1st run: input="hello", expected="olleh"
                Arguments.of("Java", "avaJ"),     // 2nd run: input="Java",  expected="avaJ"
                Arguments.of("", "")              // 3rd run: input="",      expected=""
        );
    }
}

record Person(String name, int age) {
}