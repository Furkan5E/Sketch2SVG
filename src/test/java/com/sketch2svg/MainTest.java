package com.sketch2svg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // Runs Main with the given stdin, returning {exit code, stdout}
    private static String[] runWithStdio(String stdin, String... args) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            int code = Main.run(args);
            return new String[]{String.valueOf(code), out.toString(StandardCharsets.UTF_8)};
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void testStdinToStdout() throws IOException {
        Files.writeString(tempDir.resolve("part.txt"), "square 4 0 0\n");
        // Includes from stdin resolve against the working directory; an absolute path works anywhere
        String script = "circle 10 0 0\ninclude \"" + tempDir.resolve("part.txt") + "\"\n";

        String[] result = runWithStdio(script, "-i", "-");
        assertEquals("0", result[0]);
        // stdout carries only the SVG document, so it can be piped straight into a file or another tool
        String svg = result[1].strip();
        assertTrue(svg.startsWith("<svg") && svg.endsWith("</svg>"), svg);
        assertTrue(svg.contains("<circle") && svg.contains("<polygon"), svg);
    }

    @Test
    void testFileToStdoutAndStdinToFile() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "circle 10 0 0\n");
        String[] toStdout = runWithStdio("", "-i", input.toString(), "-o", "-");
        assertEquals("0", toStdout[0]);
        assertTrue(toStdout[1].strip().startsWith("<svg"), toStdout[1]);

        Path output = tempDir.resolve("from-stdin.svg");
        String[] toFile = runWithStdio("rect 10 10 0 0\n", "-", output.toString());
        assertEquals("0", toFile[0]);
        assertTrue(Files.readString(output).contains("<polygon"));

        String[] checked = runWithStdio("squircle 1 0 0\n", "--check", "-i", "-");
        assertEquals("1", checked[0]);
        assertTrue(checked[1].contains("<stdin>: 1 error(s)"), checked[1]);
    }

    @Test
    void testBatchRejectsStdout() {
        assertEquals(1, Main.run(new String[]{"-d", tempDir.toString(), "-o", "-"}));
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
