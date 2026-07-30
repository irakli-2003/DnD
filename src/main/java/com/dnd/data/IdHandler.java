package com.dnd.data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks generated entity ids to guarantee uniqueness across all catalogs in a
 * campaign, backed by a single JSON registry file.
 *
 * <p><b>Thread-safety:</b> every method that reads or mutates {@link #registry}
 * is {@code synchronized}, so concurrent calls from multiple threads against
 * the same {@code IdHandler} instance are safe. The registry is loaded once in
 * the constructor and the field is {@code final}, so a fully-constructed
 * instance is safely publishable to other threads (standard Java final-field
 * safe-publication guarantee) as long as the constructing thread does not leak
 * {@code this} before the constructor returns. In practice this class is only
 * ever used from the single CLI input thread today; the synchronization here
 * is a defensive guarantee for any future concurrent caller (e.g. a web
 * backend), not a requirement of the current CLI.</p>
 */
public class IdHandler {
    private static final Logger LOGGER = Logger.getLogger(IdHandler.class.getName());

    private final Path registryFile;
    private final ObjectMapper mapper;
    private final IdRegistry registry;

    public IdHandler(Path registryFile) {
        this.registryFile = registryFile;
        this.mapper = JsonMappers.create();
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
            } catch (IOException e) {
                throw new DataAccessException(
                    "Failed to read ID registry at " + registryFile + ". Refusing to silently start with an "
                        + "empty registry, since that could produce duplicate IDs across entities.", e);
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
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist ID registry at " + registryFile
                + ". In-memory registry state and disk state are now out of sync.", e);
            throw new DataAccessException("Failed to persist ID registry at " + registryFile, e);
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

