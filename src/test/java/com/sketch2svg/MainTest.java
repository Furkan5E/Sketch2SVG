package com.sketch2svg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void testSuccessfulConversionReturnsZero() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Path output = tempDir.resolve("out.svg");
        Files.writeString(input, "circle 10 0 0\n");

        assertEquals(0, Main.run(new String[]{"-i", input.toString(), "-o", output.toString()}));
        assertTrue(Files.exists(output));
    }

    @Test
    void testMissingInputFailsWithoutWritingOutput() {
        Path output = tempDir.resolve("out.svg");

        assertEquals(1, Main.run(new String[]{"-i", tempDir.resolve("missing.txt").toString(), "-o", output.toString()}));
        assertFalse(Files.exists(output));
    }

    @Test
    void testUnwritableOutputFails() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "circle 10 0 0\n");
        Path output = tempDir.resolve("no_such_dir").resolve("out.svg");

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o", output.toString()}));
    }

    @Test
    void testUnknownArgumentFails() {
        assertEquals(1, Main.run(new String[]{"a.txt", "b.svg", "c"}));
    }

    @Test
    void testOptionWithoutValueFails() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "circle 10 0 0\n");

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o"}));
        assertEquals(1, Main.run(new String[]{"-i", "-o", tempDir.resolve("out.svg").toString()}));
        assertFalse(Files.exists(tempDir.resolve("out.svg")));
    }

    @Test
    void testNoInputFails() {
        assertEquals(1, Main.run(new String[]{}));
        assertEquals(1, Main.run(new String[]{"-o", tempDir.resolve("out.svg").toString()}));
    }

    @Test
    void testInvalidBatchDirectoryFails() {
        assertEquals(1, Main.run(new String[]{"-d", tempDir.resolve("missing").toString()}));
    }
}
