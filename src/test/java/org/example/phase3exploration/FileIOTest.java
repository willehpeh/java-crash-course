package org.example.phase3exploration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void shouldWriteAndReadLines() throws IOException {
        List<String> lines = List.of("Line 1", "Line 2", "Line 3");
        Files.write(tempDir.resolve("test.txt"), lines);
        List<String> readLines = Files.readAllLines(tempDir.resolve("test.txt"));
        assertThat(readLines).isEqualTo(lines);
    }

    @Test
    void shouldDeleteFile() throws IOException {
        assertThat(Files.exists(tempDir.resolve("test.txt"))).isFalse();
        Files.writeString(tempDir.resolve("test.txt"), "Hello, World!");
        assertThat(Files.exists(tempDir.resolve("test.txt"))).isTrue();
        Files.delete(tempDir.resolve("test.txt"));
        assertThat(Files.exists(tempDir.resolve("test.txt"))).isFalse();
    }

    @Test
    void shouldCreateDirectories() throws IOException {
        Path dir = tempDir.resolve("data");
        Files.createDirectories(dir);
        Path subdir = dir.resolve("subdir");
        Files.createDirectories(subdir);
        assertThat(Files.exists(subdir)).isTrue();
    }

    @Test
    void shouldListFiles() throws IOException {
        Files.writeString(tempDir.resolve("test1.txt"), "Hello, World!");
        Files.writeString(tempDir.resolve("test2.txt"), "Goodbye, World!");
        Files.writeString(tempDir.resolve("test.json"), "{ \"key\": \"value\" }");
        try (var files = Files.list(tempDir)) {
            assertThat(files).hasSize(3);
        }
        try (var files = Files.list(tempDir)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith(".json"))).hasSize(1);
        }
    }
}
