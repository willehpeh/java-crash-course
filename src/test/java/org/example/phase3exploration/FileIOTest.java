package org.example.phase3exploration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileIOTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndRead() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello, World!");
        String file = Files.readString(tempDir.resolve("test.txt"));
        assertThat(file).isEqualTo("Hello, World!");
    }
}
