package com.sketch2svg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Polls modification times of a set of files; simple and portable (no platform watch service quirks)
final class FileWatcher {

    private final Map<Path, FileTime> snapshot = new HashMap<>();

    // Replaces the watched set and records the files' current state
    void track(Collection<Path> files) {
        snapshot.clear();
        for (Path file : files) {
            snapshot.put(file, modifiedTime(file));
        }
    }

    // Whether any watched file was modified, created or deleted since the last track()
    boolean changed() {
        for (Map.Entry<Path, FileTime> entry : snapshot.entrySet()) {
            if (!Objects.equals(entry.getValue(), modifiedTime(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    // null when the file does not exist (or can't be read), so appearing and disappearing both count as changes
    private static FileTime modifiedTime(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return null;
        }
    }
}
