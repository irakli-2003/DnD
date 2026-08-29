package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.world.map.*;
import com.dnd.ui.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class MapEditorScene extends BaseScene {

    private static final double CELL_SIZE = 48.0;

    private GameMap map;
    private CampaignRepositories repos;
    private Canvas canvas;
    private MapLayer selectedLayer = null;
    private double dragStartX, dragStartY;
    private double layerDragOrigX, layerDragOrigY;
    private boolean dragging = false;

    public MapEditorScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        repos = new CampaignRepositories(uiSession.campaignRoot());
        map = repos.maps().getById(uiSession.getActiveMapId());

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.ENTITY_LIST));

        if (map == null) {
            root.getChildren().add(body("Map not found."));
            return wrapInScene(root);
        }

        // Defensive: repair maps whose stored grid doesn't match their width/height (e.g. maps
        // created before GameMap.ensureGridSize() existed) so they can still be opened here.
        map.ensureGridSize();

        HBox header = new HBox(12);
        header.setPadding(new Insets(8, 20, 4, 20));
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getChildren().addAll(
            title("Map Editor: " + map.getName()),
            btn("Save Map", this::saveMap),
            btn("View Map", () -> uiSession.getRouter().goTo(SceneType.MAP_VIEW))
        );
        root.getChildren().add(header);

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox leftPanel = buildLeftPanel();
        leftPanel.setMinWidth(220);

        double canvasW = map.getWidth() * CELL_SIZE;
        double canvasH = map.getHeight() * CELL_SIZE;
        canvas = new Canvas(canvasW, canvasH);
        setupCanvasInteraction();
        renderCanvas();

        ScrollPane canvasScroll = new ScrollPane(new Group(canvas));
        canvasScroll.setStyle("-fx-background-color: #1a1a2e;");

        split.getItems().addAll(leftPanel, canvasScroll);
        root.getChildren().add(split);
        return wrapInScene(root);
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #12122a;");

        panel.getChildren().add(sectionLabel("Background Layers"));

        ListView<MapLayer> layerList = new ListView<>();
        layerList.getStyleClass().add("dnd-list-view");
        layerList.setPrefHeight(200);
        refreshLayerList(layerList);

        layerList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedLayer = sel;
            renderCanvas();
        });

        Button addLayerBtn = btn("+ Add Layer", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Layer Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg"));
            File chosen = fc.showOpenDialog(null);
            if (chosen != null && uiSession.campaignRoot() != null) {
                try {
                    String layerId = "layer_" + System.currentTimeMillis();
                    String rel = ImageStore.copyImage(uiSession.campaignRoot(),
                        "maps/" + map.getId(), layerId, chosen);
                    int nextZ = map.getLayers().stream().mapToInt(MapLayer::getZOrder).max().orElse(-1) + 1;
                    MapLayer newLayer = new MapLayer(layerId, chosen.getName(), rel,
                        0, 0, map.getWidth(), map.getHeight(), nextZ);
                    map.getLayers().add(newLayer);
                    refreshLayerList(layerList);
                    renderCanvas();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Upload failed: " + ex.getMessage());
                    styleDialog(alert);
                    alert.showAndWait();
                }
            }
        });

        Button removeLayerBtn = dangerBtn("Remove Layer", () -> {
            MapLayer sel = layerList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                map.getLayers().remove(sel);
                selectedLayer = null;
                refreshLayerList(layerList);
                renderCanvas();
            }
        });

        Button upBtn = btn("↑ Forward", () -> {
            MapLayer sel = layerList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            sel.setZOrder(sel.getZOrder() + 1);
            refreshLayerList(layerList);
            renderCanvas();
        });
        Button downBtn = btn("↓ Back", () -> {
            MapLayer sel = layerList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            sel.setZOrder(Math.max(0, sel.getZOrder() - 1));
            refreshLayerList(layerList);
            renderCanvas();
        });

        panel.getChildren().addAll(layerList,
            new HBox(6, upBtn, downBtn),
            new HBox(6, addLayerBtn, removeLayerBtn));

        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));
        panel.getChildren().add(sep);

        panel.getChildren().add(sectionLabel("Place Tokens"));

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Player", "NPC", "Monster", "Beast");
        typeBox.setValue("NPC");
        typeBox.setStyle("-fx-background-color: #0f0f1e; -fx-text-fill: #d0c5a8;");

        ListView<String> entityListView = new ListView<>();
        entityListView.getStyleClass().add("dnd-list-view");
        entityListView.setPrefHeight(120);

        Runnable refreshEntities = () -> {
            entityListView.getItems().clear();
            switch (typeBox.getValue()) {
                case "Player"  -> repos.players().list().forEach(p -> entityListView.getItems().add(p.getId() + " | " + p.getName()));
                case "NPC"     -> repos.npcs().list().forEach(n -> entityListView.getItems().add(n.getId() + " | " + n.getName()));
                case "Monster" -> repos.monsters().list().forEach(m -> entityListView.getItems().add(m.getId() + " | " + m.getName()));
                case "Beast"   -> repos.beasts().list().forEach(b -> entityListView.getItems().add(b.getId() + " | " + b.getName()));
            }
        };
        typeBox.setOnAction(e -> refreshEntities.run());
        refreshEntities.run();

        Label clickHint = body("Select entity then click a cell to place");
        clickHint.setStyle("-fx-text-fill: #6a5a3a; -fx-font-size: 11px; -fx-wrap-text: true;");

        panel.getChildren().addAll(typeBox, entityListView, clickHint);
        panel.setUserData(new Object[]{typeBox, entityListView});

        return panel;
    }

    private void refreshLayerList(ListView<MapLayer> list) {
        list.getItems().clear();
        if (map.getLayers() != null) {
            List<MapLayer> sorted = new ArrayList<>(map.getLayers());
            sorted.sort(Comparator.comparingInt(MapLayer::getZOrder));
            list.getItems().addAll(sorted);
        }
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(MapLayer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText("z=" + item.getZOrder() + " | " + item.getLabel()); setStyle("-fx-text-fill: #d0c5a8;"); }
            }
        });
    }

    private void setupCanvasInteraction() {
        canvas.setOnMousePressed(e -> {
            double mx = e.getX(), my = e.getY();
            dragStartX = mx; dragStartY = my;
            dragging = false;

            if (selectedLayer != null) {
                double lx = selectedLayer.getX() * CELL_SIZE;
                double ly = selectedLayer.getY() * CELL_SIZE;
                double lw = selectedLayer.getWidth() * CELL_SIZE;
                double lh = selectedLayer.getHeight() * CELL_SIZE;
                if (mx >= lx && mx <= lx + lw && my >= ly && my <= ly + lh) {
                    layerDragOrigX = selectedLayer.getX();
                    layerDragOrigY = selectedLayer.getY();
                    dragging = true;
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (dragging && selectedLayer != null) {
                double dx = (e.getX() - dragStartX) / CELL_SIZE;
                double dy = (e.getY() - dragStartY) / CELL_SIZE;
                selectedLayer.setX(Math.max(0, layerDragOrigX + dx));
                selectedLayer.setY(Math.max(0, layerDragOrigY + dy));
                renderCanvas();
            }
        });

        canvas.setOnMouseReleased(e -> {
            dragging = false;
            if (selectedLayer != null) {
                selectedLayer.setX(Math.round(selectedLayer.getX() * 2) / 2.0);
                selectedLayer.setY(Math.round(selectedLayer.getY() * 2) / 2.0);
                renderCanvas();
            }
        });

        canvas.setOnScroll(e -> {
            if (selectedLayer != null && e.isControlDown()) {
                double delta = e.getDeltaY() > 0 ? 0.5 : -0.5;
                selectedLayer.setWidth(Math.max(1, selectedLayer.getWidth() + delta));
                selectedLayer.setHeight(Math.max(1, selectedLayer.getHeight() + delta));
                renderCanvas();
                e.consume();
            }
        });
    }

    private void renderCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (map.getLayers() != null) {
            List<MapLayer> sorted = map.getLayers().stream()
                .sorted(Comparator.comparingInt(MapLayer::getZOrder)).toList();
            for (MapLayer layer : sorted) {
                if (layer.getImagePath() != null && uiSession.campaignRoot() != null) {
                    Image img = ImageStore.load(uiSession.campaignRoot(), layer.getImagePath());
                    if (img != null) {
                        gc.drawImage(img,
                            layer.getX() * CELL_SIZE, layer.getY() * CELL_SIZE,
                            layer.getWidth() * CELL_SIZE, layer.getHeight() * CELL_SIZE);
                    } else {
                        gc.setFill(Color.web("#44444488"));
                        gc.fillRect(layer.getX() * CELL_SIZE, layer.getY() * CELL_SIZE,
                            layer.getWidth() * CELL_SIZE, layer.getHeight() * CELL_SIZE);
                    }
                }
                if (layer == selectedLayer) {
                    gc.setStroke(Color.web("#c9a84c"));
                    gc.setLineWidth(2);
                    gc.setLineDashes(6, 4);
                    gc.strokeRect(layer.getX() * CELL_SIZE, layer.getY() * CELL_SIZE,
                        layer.getWidth() * CELL_SIZE, layer.getHeight() * CELL_SIZE);
                    gc.setLineDashes();
                    double hx = (layer.getX() + layer.getWidth()) * CELL_SIZE - 6;
                    double hy = (layer.getY() + layer.getHeight()) * CELL_SIZE - 6;
                    gc.setFill(Color.web("#c9a84c"));
                    gc.fillRect(hx, hy, 12, 12);
                }
            }
        }

        gc.setStroke(Color.web("#ffffff22"));
        gc.setLineWidth(0.5);
        gc.setLineDashes();
        for (int x = 0; x <= map.getWidth(); x++)
            gc.strokeLine(x * CELL_SIZE, 0, x * CELL_SIZE, map.getHeight() * CELL_SIZE);
        for (int y = 0; y <= map.getHeight(); y++)
            gc.strokeLine(0, y * CELL_SIZE, map.getWidth() * CELL_SIZE, y * CELL_SIZE);

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

    private void drawToken(GraphicsContext gc, MapObject obj, int cx, int cy, int slot) {
        double bx = cx * CELL_SIZE + 4 + slot * 4;
        double by = cy * CELL_SIZE + 4 + slot * 4;
        double size = CELL_SIZE - 8 - slot * 4;
        if (size < 8) return;

        Color tokenColor;
        String label;
        String imgPath = null;

        if (obj instanceof PlayerToken pt) {
            tokenColor = Color.web("#4466aa"); label = pt.getCharacter() != null ? abbrev(pt.getCharacter().getName()) : "P";
            if (pt.getCharacter() != null) imgPath = pt.getCharacter().getImagePath();
        } else if (obj instanceof NpcToken nt) {
            tokenColor = Color.web("#66aa44"); label = nt.getNpc() != null ? abbrev(nt.getNpc().getName()) : "N";
            if (nt.getNpc() != null) imgPath = nt.getNpc().getImagePath();
        } else if (obj instanceof MonsterToken mt) {
            tokenColor = Color.web("#aa4444"); label = mt.getMonster() != null ? abbrev(mt.getMonster().getName()) : "M";
            if (mt.getMonster() != null) imgPath = mt.getMonster().getImagePath();
        } else {
            tokenColor = Color.web("#aaaa44"); label = obj.getSymbol();
        }

        boolean drewImage = false;
        if (imgPath != null && uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), imgPath);
            if (img != null) {
                gc.save();
                gc.beginPath();
                gc.arc(bx + size/2, by + size/2, size/2, size/2, 0, 360);
                gc.clip();
                gc.drawImage(img, bx, by, size, size);
                gc.restore();
                drewImage = true;
            }
        }
        if (!drewImage) {
            gc.setFill(tokenColor.deriveColor(0,1,0.5,0.85));
            gc.fillOval(bx, by, size, size);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Georgia", javafx.scene.text.FontWeight.BOLD, size * 0.4));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            gc.fillText(label, bx + size/2, by + size/2);
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

    private void saveMap() {
        try {
            repos.maps().save(map);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Map saved.", ButtonType.OK);
            styleDialog(alert);
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage());
            styleDialog(alert);
            alert.showAndWait();
        }
    }
}
