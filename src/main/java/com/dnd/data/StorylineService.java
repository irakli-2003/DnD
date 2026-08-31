package com.dnd.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Filesystem-backed service for the Storyline feature: a per-campaign folder/file tree
 * (rooted at {@code <campaignRoot>/storyline/}) representing the campaign's story arc.
 * Folders group "sessions" (leaf text files); the {@link com.dnd.ui.scenes.StorylineScene}
 * renders this tree both as a plain file tree and as a nested-arc timeline.
 */
public class StorylineService {

    /**
     * Per-folder file recording the DM's manual ordering of that folder's children,
     * one name per line. Kept as a hidden sibling file so ordering survives restarts
     * without needing a database, and so the arc timeline and the file tree can share
     * one source of truth for sequence.
     */
    static final String ORDER_FILE = ".storyline-order";

    private final Path root;

    public StorylineService(Path campaignRoot) {
        this.root = campaignRoot.resolve("storyline");
    }

    /** Absolute path of the storyline root folder. */
    public Path getRoot() {
        return root;
    }

    /** Creates the storyline root folder if it does not already exist (self-healing for older campaigns). */
    public void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lists the direct children of {@code folder} in the DM's manual order (see
     * {@link #ORDER_FILE}). Children with no recorded position - newly created or
     * externally added ones - are appended after the ordered ones, folders first,
     * then files, both alphabetically.
     */
    public List<Path> listChildren(Path folder) {
        if (folder == null || !Files.isDirectory(folder)) return List.of();
        try (Stream<Path> stream = Files.list(folder)) {
            List<Path> children = new ArrayList<>(stream
                .filter(p -> !p.getFileName().toString().equals(ORDER_FILE))
                .toList());
            List<String> order = readOrder(folder);
            children.sort(Comparator
                .comparingInt((Path p) -> {
                    int idx = order.indexOf(p.getFileName().toString());
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                })
                .thenComparing(p -> Files.isDirectory(p) ? 0 : 1)
                .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
            return children;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads the recorded child ordering for {@code folder}; empty when none has been set. */
    public List<String> readOrder(Path folder) {
        Path orderFile = folder.resolve(ORDER_FILE);
        if (!Files.isRegularFile(orderFile)) return List.of();
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(orderFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) lines.add(trimmed);
            }
            return lines;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Records {@code names} as the manual ordering of {@code folder}'s children. */
    public void writeOrder(Path folder, List<String> names) {
        if (folder == null || !Files.isDirectory(folder)) return;
        try {
            Files.write(folder.resolve(ORDER_FILE), names, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Moves {@code child} to index {@code newIndex} among its siblings, persisting the
     * result so both the file tree and the arc timeline reflect the new sequence.
     */
    public void reorder(Path child, int newIndex) {
        if (child == null) return;
        Path parent = child.getParent();
        if (parent == null || !Files.isDirectory(parent)) return;
        List<String> names = new ArrayList<>();
        for (Path sibling : listChildren(parent)) {
            names.add(sibling.getFileName().toString());
        }
        String moving = child.getFileName().toString();
        int currentIndex = names.indexOf(moving);
        if (currentIndex < 0) return;
        names.remove(currentIndex);
        int clamped = Math.max(0, Math.min(newIndex, names.size()));
        names.add(clamped, moving);
        writeOrder(parent, names);
    }

    /** Shifts {@code child} one position earlier among its siblings. */
    public void moveUp(Path child) {
        int idx = indexOf(child);
        if (idx > 0) reorder(child, idx - 1);
    }

    /** Shifts {@code child} one position later among its siblings. */
    public void moveDown(Path child) {
        int idx = indexOf(child);
        if (idx >= 0) reorder(child, idx + 1);
    }

    /** @return {@code child}'s position among its siblings, or -1 if it has no parent folder. */
    public int indexOf(Path child) {
        if (child == null || child.getParent() == null) return -1;
        List<Path> siblings = listChildren(child.getParent());
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getFileName().equals(child.getFileName())) return i;
        }
        return -1;
    }

    /** Creates a new empty subfolder named {@code name} under {@code parent}. */
    public Path createFolder(Path parent, String name) {
        Path target = resolveNewChild(parent, name);
        try {
            Files.createDirectory(target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Creates a new empty text file named {@code name} under {@code parent}. */
    public Path createFile(Path parent, String name) {
        Path target = resolveNewChild(parent, name);
        try {
            Files.createFile(target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveNewChild(Path parent, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        String safe = name.trim();
        if (safe.indexOf('/') >= 0 || safe.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Name cannot contain path separators.");
        }
        if (safe.equals(ORDER_FILE)) {
            throw new IllegalArgumentException("That name is reserved.");
        }
        Path effectiveParent = (parent == null) ? root : parent;
        if (!Files.isDirectory(effectiveParent)) {
            throw new IllegalArgumentException("Selected item is not a folder.");
        }
        Path target = effectiveParent.resolve(safe);
        if (Files.exists(target)) {
            throw new IllegalArgumentException("An item named '" + safe + "' already exists here.");
        }
        return target;
    }

    /** Recursively deletes {@code target} (file or folder). */
    public void delete(Path target) {
        if (target == null || !Files.exists(target)) return;
        try {
            if (Files.isDirectory(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            } else {
                Files.delete(target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Moves {@code source} (file or folder, with all descendants if a folder) under
     * {@code destinationFolder}. Rejects moving a folder into itself or one of its own
     * descendants, and rejects moving an item into its current parent (no-op) folder.
     */
    public Path move(Path source, Path destinationFolder) {
        if (source == null || destinationFolder == null) {
            throw new IllegalArgumentException("Source and destination are required.");
        }
        if (!Files.isDirectory(destinationFolder)) {
            throw new IllegalArgumentException("Destination must be a folder.");
        }
        Path normalizedSource = source.normalize();
        Path normalizedDest = destinationFolder.normalize();
        if (normalizedDest.equals(normalizedSource) || normalizedDest.startsWith(normalizedSource)) {
            throw new IllegalArgumentException("Cannot move a folder into itself or one of its own subfolders.");
        }
        Path target = destinationFolder.resolve(source.getFileName());
        if (Files.exists(target)) {
            throw new IllegalArgumentException("An item named '" + source.getFileName() + "' already exists in the destination.");
        }
        try {
            Files.move(source, target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads the full text content of a session file. */
    public String readText(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Overwrites the full text content of a session file. */
    public void writeText(Path file, String content) {
        try {
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** True if {@code path} is a leaf "session" file (not the storyline root, not a folder). */
    public boolean isSessionFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    /** True if {@code path} is a folder under (or equal to) the storyline root. */
    public boolean isFolder(Path path) {
        return path != null && Files.isDirectory(path);
    }
}
