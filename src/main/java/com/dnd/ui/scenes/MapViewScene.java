package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.world.map.*;
import com.dnd.ui.*;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.List;

public class MapViewScene extends BaseScene {

    private static final double CELL_SIZE = 48.0;

    public MapViewScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        CampaignRepositories repos = new CampaignRepositories(uiSession.campaignRoot());
        GameMap map = repos.maps().getById(uiSession.getActiveMapId());

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(uiSession.isDm() ? SceneType.ENTITY_LIST : SceneType.PLAYER_HOME));

        if (map == null) {
            root.getChildren().add(body("Map not found."));
            return wrapInScene(root);
        }

        // Defensive: repair maps whose stored grid doesn't match their width/height (e.g. maps
        // created before GameMap.ensureGridSize() existed) so they can still be viewed here.
        map.ensureGridSize();

        Label titleLabel = title("Map: " + map.getName());
        HBox header = new HBox(12, titleLabel);
        header.setPadding(new Insets(10, 20, 0, 20));
        root.getChildren().add(header);

        double canvasW = map.getWidth() * CELL_SIZE;
        double canvasH = map.getHeight() * CELL_SIZE;
        Canvas canvas = new Canvas(canvasW, canvasH);
        renderMap(canvas, map);

        Label infoLabel = body("");
        canvas.setOnMouseClicked(e -> {
            int cx = (int)(e.getX() / CELL_SIZE);
            int cy = (int)(e.getY() / CELL_SIZE);
            if (cx >= 0 && cx < map.getWidth() && cy >= 0 && cy < map.getHeight()) {
                GridCell cell = map.getCell(cx, cy);
                if (!cell.getOccupants().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (MapObject obj : cell.getOccupants()) sb.append(obj).append("  ");
                    infoLabel.setText("Cell (" + cx + "," + cy + "): " + sb);
                } else {
                    infoLabel.setText("Cell (" + cx + "," + cy + "): empty");
                }
            }
        });

        ScrollPane scroll = new ScrollPane(new Group(canvas));
        scroll.setStyle("-fx-background-color: #1a1a2e;");
        scroll.setPrefViewportWidth(860);
        scroll.setPrefViewportHeight(500);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox infoBar = new HBox(infoLabel);
        infoBar.setPadding(new Insets(6, 20, 6, 20));
        infoBar.setStyle("-fx-background-color: #0f0f1e;");

        root.getChildren().addAll(scroll, infoBar);
        return wrapInScene(root);
    }

    private void renderMap(Canvas canvas, GameMap map) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (map.getLayers() != null) {
            List<MapLayer> sorted = map.getLayers().stream()
                .sorted(Comparator.comparingInt(MapLayer::getZOrder))
                .toList();
            for (MapLayer layer : sorted) {
                double lx = layer.getX() * CELL_SIZE, ly = layer.getY() * CELL_SIZE;
                double lw = layer.getWidth() * CELL_SIZE, lh = layer.getHeight() * CELL_SIZE;
                boolean drewImage = false;
                if (layer.getImagePath() != null && uiSession.campaignRoot() != null) {
                    Image img = ImageStore.load(uiSession.campaignRoot(), layer.getImagePath());
                    if (img != null) {
                        gc.drawImage(img, lx, ly, lw, lh);
                        drewImage = true;
                    }
                }
                if (!drewImage && layer.getFillColor() != null) {
                    try {
                        gc.setFill(Color.web(layer.getFillColor()));
                        gc.fillRect(lx, ly, lw, lh);
                    } catch (Exception ignored) { }
                }
            }
        }

        gc.setStroke(Color.web("#ffffff22"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= map.getWidth(); x++)
            gc.strokeLine(x * CELL_SIZE, 0, x * CELL_SIZE, map.getHeight() * CELL_SIZE);
        for (int y = 0; y <= map.getHeight(); y++)
            gc.strokeLine(0, y * CELL_SIZE, map.getWidth() * CELL_SIZE, y * CELL_SIZE);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (!cell.isPassable()) {
                    gc.setFill(Color.web("#33333388"));
                    gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        if (map.getDrawings() != null) {
            for (Drawing d : map.getDrawings()) {
                renderDrawing(gc, d);
            }
        }

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                int slot = 0;
                for (MapObject obj : cell.getOccupants()) {
                    drawToken(gc, obj, x, y, slot);
                    slot++;
                }
            }
        }
    }

    private void renderDrawing(GraphicsContext gc, Drawing d) {
        List<Double> gp = d.getPoints();
        if (gp.size() < 2) return;
        List<double[]> pts = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < gp.size(); i += 2) {
            pts.add(new double[]{gp.get(i) * CELL_SIZE, gp.get(i + 1) * CELL_SIZE});
        }
        Color color;
        try { color = Color.web(d.getColor()); } catch (Exception ex) { color = Color.web("#c9a84c"); }
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(d.getLineWidth());
        switch (d.getType()) {
            case FREEHAND -> {
                for (int i = 1; i < pts.size(); i++) {
                    gc.strokeLine(pts.get(i - 1)[0], pts.get(i - 1)[1], pts.get(i)[0], pts.get(i)[1]);
                }
            }
            case LINE -> {
                double[] a = pts.get(0), b = pts.get(pts.size() - 1);
                gc.strokeLine(a[0], a[1], b[0], b[1]);
            }
            case RECTANGLE -> {
                double[] a = pts.get(0), b = pts.get(pts.size() - 1);
                double x = Math.min(a[0], b[0]), y = Math.min(a[1], b[1]);
                double w = Math.abs(b[0] - a[0]), h = Math.abs(b[1] - a[1]);
                if (d.isFilled()) gc.fillRect(x, y, w, h); else gc.strokeRect(x, y, w, h);
            }
            case OVAL -> {
                double[] a = pts.get(0), b = pts.get(pts.size() - 1);
                double x = Math.min(a[0], b[0]), y = Math.min(a[1], b[1]);
                double w = Math.abs(b[0] - a[0]), h = Math.abs(b[1] - a[1]);
                if (d.isFilled()) gc.fillOval(x, y, w, h); else gc.strokeOval(x, y, w, h);
            }
        }
    }

    private void drawToken(GraphicsContext gc, MapObject obj, int cx, int cy, int slot) {
        double bx = cx * CELL_SIZE + slot * 6;
        double by = cy * CELL_SIZE + slot * 6;
        double size = CELL_SIZE - 8 - slot * 6;
        if (size < 8) return;

        Color tokenColor;
        String label;
        String imgPath = null;

        if (obj instanceof PlayerToken pt) {
            tokenColor = Color.web("#4466aa");
            label = pt.getCharacter() != null ? abbrev(pt.getCharacter().getName()) : "P";
            if (pt.getCharacter() != null) imgPath = pt.getCharacter().getImagePath();
        } else if (obj instanceof NpcToken nt) {
            tokenColor = Color.web("#66aa44");
            label = nt.getNpc() != null ? abbrev(nt.getNpc().getName()) : "N";
            if (nt.getNpc() != null) imgPath = nt.getNpc().getImagePath();
        } else if (obj instanceof MonsterToken mt) {
            tokenColor = Color.web("#aa4444");
            label = mt.getMonster() != null ? abbrev(mt.getMonster().getName()) : "M";
            if (mt.getMonster() != null) imgPath = mt.getMonster().getImagePath();
        } else if (obj instanceof BeastToken bt) {
            tokenColor = Color.web("#aa7744");
            label = "B";
        } else {
            tokenColor = Color.web("#aaaa44");
            label = obj.getSymbol();
        }

        boolean drewImage = false;
        if (imgPath != null && uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), imgPath);
            if (img != null) {
                gc.save();
                gc.beginPath();
                gc.arc(bx + size / 2, by + size / 2, size / 2, size / 2, 0, 360);
                gc.clip();
                gc.drawImage(img, bx, by, size, size);
                gc.restore();
                drewImage = true;
            }
        }

        if (!drewImage) {
            gc.setFill(tokenColor.deriveColor(0, 1, 0.5, 0.85));
            gc.fillOval(bx, by, size, size);
            gc.setStroke(tokenColor);
            gc.setLineWidth(1.5);
            gc.strokeOval(bx, by, size, size);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Georgia", javafx.scene.text.FontWeight.BOLD, size * 0.4));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            gc.fillText(label, bx + size / 2, by + size / 2);
        }

        gc.setStroke(tokenColor);
        gc.setLineWidth(1.5);
        gc.strokeOval(bx, by, size, size);
    }

    private String abbrev(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[1].charAt(0)).toUpperCase();
    }
}
