package com.dnd.ui;

import javafx.scene.image.Image;
import java.io.*;
import java.nio.file.*;

public final class ImageStore {

    private ImageStore() {}

    public static String copyImage(Path campaignRoot, String category, String entityId, File sourceFile) throws IOException {
        String ext = getExtension(sourceFile.getName());
        Path dir = campaignRoot.resolve("images").resolve(category);
        Files.createDirectories(dir);
        Path dest = dir.resolve(entityId + "." + ext);
        Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return campaignRoot.relativize(dest).toString().replace('\\', '/');
    }

    public static Image load(Path campaignRoot, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        Path abs = campaignRoot.resolve(relativePath);
        if (!Files.exists(abs)) return null;
        try {
            return new Image(abs.toUri().toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Image loadOrPlaceholder(Path campaignRoot, String relativePath) {
        Image img = load(campaignRoot, relativePath);
        if (img != null) return img;
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "png";
    }
}
