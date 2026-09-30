package com.sketch2svg;

import com.sketch2svg.parser.Sketch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Renders every examples/*.txt and compares it with the committed examples/*.svg (the images in the README).
// Any change to the output fails here until the snapshots are regenerated on purpose:
//   mvn test -Dsnapshots.update=true
public class SnapshotTest {

    private static final Path EXAMPLES = Path.of("examples");

    @Test
    void testExamplesMatchSnapshots() throws IOException {
        assertTrue(Files.isDirectory(EXAMPLES), "run tests from the project root");
        boolean update = Boolean.getBoolean("snapshots.update");

        List<Path> sketches;
        try (Stream<Path> files = Files.list(EXAMPLES)) {
            sketches = files.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }
        assertFalse(sketches.isEmpty());

        List<String> mismatches = new ArrayList<>();
        for (Path source : sketches) {
            Sketch sketch = new Sketch();
            assertTrue(sketch.fromFile(source.toString()), source.toString());
            assertEquals(0, sketch.getErrorCount() + sketch.getWarningCount(), source + " should parse cleanly");

            String actual = sketch.toSVGString();
            Path snapshot = source.resolveSibling(source.getFileName().toString().replace(".txt", ".svg"));
            if (update) {
                Files.writeString(snapshot, actual);
                continue;
            }
            if (!Files.exists(snapshot)) {
                mismatches.add(snapshot + " is missing");
                continue;
            }
            // Git may check the snapshot out with CRLF line endings
            String expected = Files.readString(snapshot).replace("\r\n", "\n");
            if (!expected.equals(actual)) {
                mismatches.add(snapshot + " differs from the current output" + firstDifference(expected, actual));
            }
        }
        assertTrue(mismatches.isEmpty(), String.join("\n", mismatches)
                + "\nIf the change is intended, regenerate with: mvn test -Dsnapshots.update=true");
    }

    private static String firstDifference(String expected, String actual) {
        String[] a = expected.split("\n", -1), b = actual.split("\n", -1);
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (!a[i].equals(b[i])) {
                return "\n  line " + (i + 1) + " expected: " + a[i] + "\n  line " + (i + 1) + " actual:   " + b[i];
            }
        }
        return "\n  (lengths differ: " + a.length + " vs " + b.length + " lines)";
    }
}
