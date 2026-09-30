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
    void testEditDuringBuildIsNotMissed() throws IOException {
        Path sketch = tempDir.resolve("sketch.txt");
        Path include = tempDir.resolve("part.txt");
        Files.writeString(sketch, "include part.txt\n");
        Files.writeString(include, "circle 1 0 0\n");
        FileWatcher watcher = new FileWatcher();

        // Watch loop order: snapshot the known files, build (reading them), then track what the build used
        var before = watcher.snapshot(List.of(sketch, include));
        touch(include, 5); // saved while the build was still running
        watcher.track(List.of(sketch, include), before);
        assertTrue(watcher.changed(), "the edit made during the build triggers another build");

        // Tracking with post-build times (the old behaviour) would have swallowed that edit
        watcher.track(List.of(sketch, include));
        assertFalse(watcher.changed());

        // An include seen for the first time has no earlier snapshot, so it's recorded as it is now
        Path added = tempDir.resolve("added.txt");
        Files.writeString(added, "square 1 0 0\n");
        watcher.track(List.of(sketch, include, added), watcher.snapshot(List.of(sketch, include)));
        assertFalse(watcher.changed());
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
