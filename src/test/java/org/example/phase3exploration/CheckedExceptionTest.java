package org.example.phase3exploration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckedExceptionTest {

    @TempDir Path tempDir;

    String readFileContent(Path path) {
        return safeReadString(path);
    }

    private String safeReadString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldShowFileContents() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello, World!");
        Files.writeString(tempDir.resolve("test2.txt"), "Goodbye, World!");
        Files.writeString(tempDir.resolve("test3.txt"), "Hello again, World!");
        List<String> fileContents = Stream.of("test.txt", "test2.txt", "test3.txt")
                .map(path -> readFileContent(tempDir.resolve(path)))
                .toList();
        assertThat(fileContents).containsExactly("Hello, World!", "Goodbye, World!", "Hello again, World!");
    }
}
