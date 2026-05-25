package com.dnd.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class IdHandler {
    private final Path registryFile;
    private final ObjectMapper mapper;
    private IdRegistry registry;

    public IdHandler(Path registryFile) {
        this.registryFile = registryFile;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.registry = loadRegistry();
    }

    public synchronized String generateId(String base, String filePath) {
        String normalized = sanitize(base);
        if (normalized.isEmpty()) {
            normalized = "id";
        }
        String candidate = normalized;
        int index = 2;
        while (find(candidate).isPresent()) {
            candidate = normalized + "-" + index++;
        }
        addId(candidate, filePath);
        return candidate;
    }

    public synchronized boolean addId(String id, String filePath) {
        if (find(id).isPresent()) {
            return false;
        }
        registry.getEntries().add(new IdRecord(id, filePath));
        saveRegistry();
        return true;
    }

    public synchronized boolean removeId(String id) {
        boolean removed = registry.getEntries().removeIf(entry -> entry.getId().equals(id));
        if (removed) {
            saveRegistry();
        }
        return removed;
    }

    public synchronized Optional<IdRecord> find(String id) {
        return registry.getEntries().stream()
            .filter(entry -> entry.getId().equals(id))
            .findFirst();
    }

    public synchronized List<IdRecord> list() {
        return new ArrayList<>(registry.getEntries());
    }

    private IdRegistry loadRegistry() {
        if (Files.exists(registryFile)) {
            try {
                return mapper.readValue(registryFile.toFile(), IdRegistry.class);
            } catch (IOException ignored) {
                return new IdRegistry();
            }
        }
        IdRegistry empty = new IdRegistry();
        saveRegistry(empty);
        return empty;
    }

    private void saveRegistry() {
        saveRegistry(registry);
    }

    private void saveRegistry(IdRegistry source) {
        try {
            Files.createDirectories(registryFile.getParent());
            mapper.writeValue(registryFile.toFile(), source);
        } catch (IOException ignored) {
            // Persisting registry is best-effort for now.
        }
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("[^a-z0-9\\-_]+", "-")
            .replaceAll("(^-+|-+$)", "");
    }

    public static class IdRegistry {
        private List<IdRecord> entries = new ArrayList<>();

        public IdRegistry() {
        }

        public List<IdRecord> getEntries() {
            return entries;
        }

        public void setEntries(List<IdRecord> entries) {
            this.entries = entries;
        }
    }

    public static class IdRecord {
        private String id;
        private String file;

        public IdRecord() {
        }

        public IdRecord(String id, String file) {
            this.id = id;
            this.file = file;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }
}

