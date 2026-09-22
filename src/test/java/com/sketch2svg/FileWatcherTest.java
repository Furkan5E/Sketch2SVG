package com.sketch2svg;

import com.sketch2svg.parser.Sketch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileWatcherTest {

    @TempDir
    Path tempDir;

    // Move a file's timestamp forward explicitly; real edits can land within the filesystem's timestamp resolution
    private static void touch(Path file, long secondsLater) throws IOException {
        FileTime now = Files.getLastModifiedTime(file);
        Files.setLastModifiedTime(file, FileTime.fromMillis(now.toMillis() + secondsLater * 1000));
    }

    @Test
    void testDetectsModificationCreationAndDeletion() throws IOException {
        Path a = tempDir.resolve("a.txt");
        Path missing = tempDir.resolve("later.txt");
        Files.writeString(a, "circle 1 0 0\n");

        FileWatcher watcher = new FileWatcher();
        watcher.track(List.of(a, missing));
        assertFalse(watcher.changed());

        touch(a, 5);
        assertTrue(watcher.changed());

        watcher.track(List.of(a, missing));
        Files.writeString(missing, "square 1 0 0\n");
        assertTrue(watcher.changed(), "a file appearing counts as a change");

        watcher.track(List.of(a, missing));
        Files.delete(a);
        assertTrue(watcher.changed(), "a file disappearing counts as a change");
    }

    @Test
    void testSketchReportsIncludedAndMissingFiles() throws IOException {
        Files.writeString(tempDir.resolve("main.txt"), "include part.txt\ninclude todo.txt\n");
        Files.writeString(tempDir.resolve("part.txt"), "circle 1 0 0\n");

        Sketch sketch = new Sketch();
        sketch.fromFile(tempDir.resolve("main.txt").toString());
        List<String> names = sketch.getSourceFiles().stream().map(p -> p.getFileName().toString()).toList();
        assertEquals(List.of("main.txt", "part.txt", "todo.txt"), names);
    }
}
