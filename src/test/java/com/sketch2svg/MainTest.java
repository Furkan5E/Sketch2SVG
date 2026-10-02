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
import java.nio.file.attribute.FileTime;
import java.util.function.BooleanSupplier;

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
    void testOutputOverInputIsRefused() throws IOException {
        Path input = tempDir.resolve("drawing.txt");
        Files.writeString(input, "circle 10 0 0\n");

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o", input.toString()}));
        // The same file under another spelling
        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o", tempDir.resolve("./drawing.txt").toString()}));
        assertEquals("circle 10 0 0\n", Files.readString(input));
    }

    @Test
    void testOutputOverIncludedFileIsRefused() throws IOException {
        Path input = tempDir.resolve("main.txt");
        Path part = tempDir.resolve("part.txt");
        Files.writeString(input, "include part.txt\n");
        Files.writeString(part, "circle 10 0 0\n");

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o", part.toString()}));
        assertEquals("circle 10 0 0\n", Files.readString(part));
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
    void testRecursiveBatchMirrorsFolders() throws IOException {
        Path in = tempDir.resolve("in");
        Files.createDirectories(in.resolve("scenes/night"));
        Files.writeString(in.resolve("top.txt"), "circle 1 0 0\n");
        Files.writeString(in.resolve("scenes/night/moon.TXT"), "circle 5 0 0\n");
        Files.writeString(in.resolve("scenes/notes.md"), "not a sketch\n");
        Path out = tempDir.resolve("out");

        // Without -r only the top level is converted
        assertEquals(0, Main.run(new String[]{"-d", in.toString(), "-o", out.toString()}));
        assertTrue(Files.exists(out.resolve("top.svg")));
        assertFalse(Files.exists(out.resolve("scenes")));

        assertEquals(0, Main.run(new String[]{"-d", in.toString(), "-r", "-o", out.toString()}));
        assertTrue(Files.exists(out.resolve("scenes/night/moon.svg")));
        assertFalse(Files.exists(out.resolve("scenes/notes.svg")));
    }

    // Polls until the condition holds or the deadline passes (keeps the timing-based test from being flaky)
    private static boolean eventually(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(50);
        }
        return false;
    }

    private static String readOrEmpty(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "";
        }
    }

    @Test
    void testWatchRebuildsOnChangesUntilInterrupted() throws Exception {
        Path input = tempDir.resolve("in.txt");
        Path part = tempDir.resolve("part.txt");
        Path output = tempDir.resolve("out.svg");
        Files.writeString(input, "circle 1 0 0\ninclude part.txt\n");
        Files.writeString(part, "square 2 0 0\n");

        int[] exit = {-1};
        Thread watcher = new Thread(() -> exit[0] = Main.run(new String[]{"-w", "-i", input.toString(), "-o", output.toString()}));
        watcher.start();
        try {
            assertTrue(eventually(() -> readOrEmpty(output).contains("<circle")), "first build");

            // Editing the main file and an included file both trigger a rebuild
            Files.writeString(input, "rect 3 3 0 0 fill=gold\ninclude part.txt\n");
            Files.setLastModifiedTime(input, FileTime.fromMillis(System.currentTimeMillis() + 5_000));
            assertTrue(eventually(() -> readOrEmpty(output).contains("fill:#FFD700")), "rebuilt after main file change");

            Files.writeString(part, "circle 7 0 0\n");
            Files.setLastModifiedTime(part, FileTime.fromMillis(System.currentTimeMillis() + 10_000));
            assertTrue(eventually(() -> readOrEmpty(output).contains("r=\"7\"")), "rebuilt after include change");
        } finally {
            watcher.interrupt();
            watcher.join(5_000);
        }
        assertFalse(watcher.isAlive());
        assertEquals(0, exit[0]);
    }

    @Test
    void testWatchRejectsUnwatchableInputs() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "circle 1 0 0\n");
        // These return straight away instead of blocking in the watch loop
        assertEquals(1, Main.run(new String[]{"-w", "-d", tempDir.toString()}));
        assertEquals(1, Main.run(new String[]{"-w", "-i", "-"}));
        assertEquals(1, Main.run(new String[]{"-w", "-i", input.toString(), "-o", "-"}));
        assertEquals(1, Main.run(new String[]{"-w", "-c", "-i", input.toString()}));
    }

    @Test
    void testSizeAndFitOptions() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "canvas 300 100\ncircle 10 0 0\n");

        String[] sized = runWithStdio("", "-i", input.toString(), "-o", "-", "--size", "600");
        assertEquals("0", sized[0]);
        // 300x100 canvas at 600px wide is 200px tall
        assertTrue(sized[1].startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"200\" viewBox=\"-150 -50 300 100\">"), sized[1]);

        // --fit replaces the script's canvas with the drawing's bounds (circle r=10 + half stroke + 10 margin)
        String[] fitted = runWithStdio("", "-i", input.toString(), "-o", "-", "--fit");
        assertTrue(fitted[1].contains("viewBox=\"-21 -21 42 42\""), fitted[1]);

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "--size", "0"}));
        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "--size", "wide"}));
    }

    @Test
    void testPrettyAndMinifiedOutput() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "group at=5,0\n  circle 1 0 0\n  text 0 0 5 \"hi\"\nend\n");

        String pretty = runWithStdio("", "-i", input.toString(), "-o", "-", "--pretty")[1];
        // Each level (svg > g > circle/text > text content) indents by two more spaces
        assertTrue(pretty.contains("\n  <g transform=\"translate(5 0)\">"
                + "\n    <circle style=\"fill:none;stroke-width:1;stroke:#000000\" cx=\"0\" cy=\"0\" r=\"1\"/>"
                + "\n    <text"), pretty);
        assertTrue(pretty.contains("\n      hi\n    </text>\n  </g>\n</svg>"), pretty);

        String minified = runWithStdio("", "-i", input.toString(), "-o", "-", "--minify")[1].strip();
        assertFalse(minified.contains("\n"), minified);
        assertTrue(minified.contains("<g transform=\"translate(5 0)\"><circle"), minified);
        assertTrue(minified.contains(">hi</text></g></svg>"), minified);

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "--pretty", "--minify"}));
    }

    @Test
    void testGridOverlay() throws IOException {
        Path input = tempDir.resolve("in.txt");
        Files.writeString(input, "circle 10 0 0\n");

        String svg = runWithStdio("", "-i", input.toString(), "-o", "-", "--grid")[1];
        // Default 200-unit view: 10-unit steps from -100 to 100 inclusive is 21 positions per direction,
        // minus 0 (drawn as an axis instead): 20 vertical + 20 horizontal lines + 2 axes
        assertEquals(42, svg.split("<polyline").length - 1, svg);
        assertTrue(svg.contains("points=\"-100,0 100,0 \""), svg);   // x axis
        assertTrue(svg.contains("stroke:#E63946"), svg);
        // Drawn on top of the shapes
        assertTrue(svg.indexOf("<circle") < svg.indexOf("<g>"), svg);

        // A 2000-unit canvas gets 100-unit steps
        Files.writeString(input, "canvas 2000 2000\ncircle 10 0 0\n");
        String large = runWithStdio("", "-i", input.toString(), "-o", "-", "--grid")[1];
        assertTrue(large.contains("points=\"100,1000 100,-1000 \""), large);
        assertFalse(large.contains("points=\"10,"), large);
    }

    @Test
    void testOverflowingOutputFailsCleanly() throws IOException {
        // Each number is valid on its own, but radius x scale overflows float range when rendered
        Path input = tempDir.resolve("huge.txt");
        Files.writeString(input, "circle 1e30 0 0 scale=1e30\n");
        Path output = tempDir.resolve("huge.svg");

        assertEquals(1, Main.run(new String[]{"-i", input.toString(), "-o", output.toString()}));
        assertFalse(Files.exists(output), "no half-written file is left behind");

        // --check renders in memory too, so it no longer passes a file that can't be converted
        String[] checked = runWithStdio("", "--check", "-i", input.toString());
        assertEquals("1", checked[0]);
        assertTrue(checked[1].contains("1 error(s)"), checked[1]);
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
