package com.sketch2svg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
    void testCheckModeWritesNothingAndFailsOnErrors() throws IOException {
        Path clean = tempDir.resolve("clean.txt");
        Path warned = tempDir.resolve("warned.txt");
        Path broken = tempDir.resolve("broken.txt");
        Files.writeString(clean, "circle 10 0 0\n");
        Files.writeString(warned, "circle 10 0 0 colour=red\n");
        Files.writeString(broken, "circle 10 0 0\nsquircle 5 0 0\n");

        assertEquals(0, Main.run(new String[]{"--check", "-i", clean.toString()}));
        assertEquals(0, Main.run(new String[]{"-c", "-i", warned.toString()})); // warnings don't fail
        assertEquals(1, Main.run(new String[]{"-c", "-i", broken.toString()}));
        assertEquals(1, Main.run(new String[]{"-c", "-d", tempDir.toString()}));

        try (var files = Files.list(tempDir)) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".svg")));
        }
    }

    @Test
    void testVersionFlag() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            assertEquals(0, Main.run(new String[]{"--version"}));
        } finally {
            System.setOut(originalOut);
        }
        // Tests run from compiled classes, which have no jar manifest
        assertEquals("Sketch2SVG dev", out.toString(StandardCharsets.UTF_8).strip());
    }

    @Test
    void testInvalidBatchDirectoryFails() {
        assertEquals(1, Main.run(new String[]{"-d", tempDir.resolve("missing").toString()}));
    }
}
