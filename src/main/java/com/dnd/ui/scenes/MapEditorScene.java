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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class MapEditorScene extends BaseScene {

    private static final double CELL_SIZE = 48.0;

    /** Active canvas interaction mode, selected via the "Draw Tools" toggle buttons. */
    private enum Tool { SELECT, PEN, LINE, RECT, OVAL, ERASER }

    private GameMap map;
    private CampaignRepositories repos;
    private Canvas canvas;

    // Layer select/drag state
    private MapLayer selectedLayer = null;
    private double dragStartX, dragStartY;
    private double layerDragOrigX, layerDragOrigY;
    private boolean dragging = false;

    // Drawing tool state
    private Tool currentTool = Tool.SELECT;
    private Color drawColor = Color.web("#c9a84c");
    private boolean drawFilled = false;
    private double drawLineWidth = 2.0;
    private final List<double[]> penPoints = new ArrayList<>();
    private boolean shapeDrawing = false;
    private double shapeStartX, shapeStartY, shapeCurX, shapeCurY;
    private int drawIdCounter = 0;

    // Place-tokens panel controls, wired up so canvas clicks/drops can read the current selection
    private ComboBox<String> tokenTypeBox;
    private ListView<String> tokenEntityListView;

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
        setupCanvasDragAndDrop();
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
        layerList.setPrefHeight(150);
        refreshLayerList(layerList);

        layerList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedLayer = sel;
            renderCanvas();
        });

        Button addLayerBtn = btn("+ Add Layer", () -> openAddLayerDialog(layerList));

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

        panel.getChildren().add(separator());
        panel.getChildren().add(buildDrawToolsSection());
        panel.getChildren().add(separator());
        panel.getChildren().add(buildPlaceTokensSection());

        return panel;
    }

    private Separator separator() {
        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));
        return sep;
    }

    /** Draw tools: shape selector, color picker, filled toggle, line width, and clear-all. */
    private VBox buildDrawToolsSection() {
        VBox box = new VBox(6);
        box.getChildren().add(sectionLabel("Draw Tools"));

        ToggleGroup toolGroup = new ToggleGroup();
        ToggleButton selectBtn = toolToggle("Select/Move", toolGroup);
        ToggleButton penBtn = toolToggle("Pen", toolGroup);
        ToggleButton lineBtn = toolToggle("Line", toolGroup);
        ToggleButton rectBtn = toolToggle("Rect", toolGroup);
        ToggleButton ovalBtn = toolToggle("Oval", toolGroup);
        ToggleButton eraserBtn = toolToggle("Eraser", toolGroup);
        selectBtn.setSelected(true);

        toolGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null) { selectBtn.setSelected(true); return; }
            if (sel == selectBtn) currentTool = Tool.SELECT;
            else if (sel == penBtn) currentTool = Tool.PEN;
            else if (sel == lineBtn) currentTool = Tool.LINE;
            else if (sel == rectBtn) currentTool = Tool.RECT;
            else if (sel == ovalBtn) currentTool = Tool.OVAL;
            else if (sel == eraserBtn) currentTool = Tool.ERASER;
            penPoints.clear();
            shapeDrawing = false;
            renderCanvas();
        });

        FlowPane toolRow = new FlowPane(4, 4, selectBtn, penBtn, lineBtn, rectBtn, ovalBtn, eraserBtn);

        ColorPicker colorPicker = new ColorPicker(drawColor);
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setOnAction(e -> drawColor = colorPicker.getValue());

        CheckBox filledCheck = checkBox("Filled shape (Rect/Oval)");
        filledCheck.selectedProperty().addListener((obs, old, val) -> drawFilled = val);

        Spinner<Integer> widthSpinner = new Spinner<>(1, 12, (int) drawLineWidth);
        widthSpinner.setEditable(true);
        widthSpinner.setMaxWidth(80);
        widthSpinner.valueProperty().addListener((obs, old, val) -> drawLineWidth = val);
        HBox widthRow = new HBox(6, body("Line width:"), widthSpinner);
        widthRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button clearBtn = dangerBtn("Clear All Drawings", () -> {
            if (map.getDrawings().isEmpty()) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove all hand-drawn shapes on this map?", ButtonType.OK, ButtonType.CANCEL);
            styleDialog(confirm);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    map.getDrawings().clear();
                    renderCanvas();
                }
            });
        });

        Label hint = body("Pen/Line/Rect/Oval draw on the map. Eraser removes the shape under your click.");
        hint.setStyle("-fx-text-fill: #6a5a3a; -fx-font-size: 11px; -fx-wrap-text: true;");

        box.getChildren().addAll(toolRow, colorPicker, filledCheck, widthRow, clearBtn, hint);
        return box;
    }

    private ToggleButton toolToggle(String label, ToggleGroup group) {
        ToggleButton b = new ToggleButton(label);
        b.getStyleClass().add("tool-toggle-button");
        b.setToggleGroup(group);
        return b;
    }

    /** Place-tokens panel: pick an entity type + specific entity, then click or drag onto the map. */
    private VBox buildPlaceTokensSection() {
        VBox box = new VBox(6);
        box.getChildren().add(sectionLabel("Place Tokens"));

        tokenTypeBox = new ComboBox<>();
        tokenTypeBox.getItems().addAll("Player", "NPC", "Monster", "Beast");
        tokenTypeBox.setValue("NPC");
        tokenTypeBox.setMaxWidth(Double.MAX_VALUE);
        tokenTypeBox.setStyle("-fx-background-color: #0f0f1e; -fx-text-fill: #d0c5a8;");

        tokenEntityListView = new ListView<>();
        tokenEntityListView.getStyleClass().add("dnd-list-view");
        tokenEntityListView.setPrefHeight(120);

        Runnable refreshEntities = () -> {
            tokenEntityListView.getItems().clear();
            switch (tokenTypeBox.getValue()) {
                case "Player"  -> repos.players().list().forEach(p -> tokenEntityListView.getItems().add(p.getId() + " | " + p.getName()));
                case "NPC"     -> repos.npcs().list().forEach(n -> tokenEntityListView.getItems().add(n.getId() + " | " + n.getName()));
                case "Monster" -> repos.monsters().list().forEach(m -> tokenEntityListView.getItems().add(m.getId() + " | " + m.getName()));
                case "Beast"   -> repos.beasts().list().forEach(b -> tokenEntityListView.getItems().add(b.getId() + " | " + b.getName()));
            }
        };
        tokenTypeBox.setOnAction(e -> refreshEntities.run());
        refreshEntities.run();

        // Drag support: drag a row onto the canvas to drop a token there.
        tokenEntityListView.setOnDragDetected(e -> {
            String sel = tokenEntityListView.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Dragboard db = tokenEntityListView.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(tokenTypeBox.getValue() + "::" + sel);
            db.setContent(content);
            e.consume();
        });

        Label clickHint = body("Select an entity, then click a cell on the map (or drag it there) to place it.");
        clickHint.setStyle("-fx-text-fill: #6a5a3a; -fx-font-size: 11px; -fx-wrap-text: true;");

        box.getChildren().addAll(tokenTypeBox, tokenEntityListView, clickHint);
        return box;
    }

    /** Dialog for adding a background layer: name, image, and placement/size within the map. */
    private void openAddLayerDialog(ListView<MapLayer> layerList) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Add Map Layer");

        TextField labelField = textField("Layer name");
        File[] chosenFile = new File[1];
        Label fileLabel = body("No image selected.");
        fileLabel.setStyle("-fx-text-fill: #6a5a3a; -fx-font-size: 11px;");
        Button chooseBtn = btn("Choose Image...", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Layer Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File chosen = fc.showOpenDialog(dialogStage);
            if (chosen != null) {
                chosenFile[0] = chosen;
                fileLabel.setText(chosen.getName());
                if (labelField.getText() == null || labelField.getText().isBlank()) {
                    labelField.setText(chosen.getName());
                }
            }
        });

        Spinner<Integer> xSpinner = new Spinner<>(0, GameMap.MAX_DIMENSION, 0);
        Spinner<Integer> ySpinner = new Spinner<>(0, GameMap.MAX_DIMENSION, 0);
        Spinner<Integer> wSpinner = new Spinner<>(1, GameMap.MAX_DIMENSION, map.getWidth());
        Spinner<Integer> hSpinner = new Spinner<>(1, GameMap.MAX_DIMENSION, map.getHeight());
        for (Spinner<Integer> s : List.of(xSpinner, ySpinner, wSpinner, hSpinner)) {
            s.setEditable(true);
            s.setMaxWidth(90);
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, body("Name:"), labelField);
        grid.addRow(1, body("Image:"), new HBox(6, chooseBtn, fileLabel));
        grid.addRow(2, body("X (cells):"), xSpinner, body("Y (cells):"), ySpinner);
        grid.addRow(3, body("Width (cells):"), wSpinner, body("Height (cells):"), hSpinner);

        Label errorLabel = body("");
        errorLabel.setStyle("-fx-text-fill: #cc4444; -fx-font-size: 11px;");

        Button addBtn = btn("Add Layer", () -> {
            if (chosenFile[0] == null) {
                errorLabel.setText("Please choose an image for the layer.");
                return;
            }
            if (labelField.getText() == null || labelField.getText().isBlank()) {
                errorLabel.setText("Please give the layer a name.");
                return;
            }
            try {
                String layerId = "layer_" + System.currentTimeMillis();
                String rel = ImageStore.copyImage(uiSession.campaignRoot(),
                    "maps/" + map.getId(), layerId, chosenFile[0]);
                int nextZ = map.getLayers().stream().mapToInt(MapLayer::getZOrder).max().orElse(-1) + 1;
                MapLayer newLayer = new MapLayer(layerId, labelField.getText().trim(), rel,
                    xSpinner.getValue(), ySpinner.getValue(), wSpinner.getValue(), hSpinner.getValue(), nextZ);
                map.getLayers().add(newLayer);
                refreshLayerList(layerList);
                renderCanvas();
                dialogStage.close();
            } catch (Exception ex) {
                errorLabel.setText("Upload failed: " + ex.getMessage());
            }
        });
        Button cancelBtn = btn("Cancel", dialogStage::close);

        HBox buttons = new HBox(10, addBtn, cancelBtn);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        VBox layout = new VBox(10, grid, errorLabel, buttons);
        layout.setPadding(new Insets(14));
        layout.setStyle("-fx-background-color: #1a1a2e;");

        dialogStage.setScene(themedScene(layout, 480, 320));
        dialogStage.showAndWait();
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

            switch (currentTool) {
                case PEN -> { penPoints.clear(); penPoints.add(new double[]{mx, my}); }
                case LINE, RECT, OVAL -> {
                    shapeStartX = mx; shapeStartY = my;
                    shapeCurX = mx; shapeCurY = my;
                    shapeDrawing = true;
                }
                case ERASER -> eraseAt(mx, my);
                case SELECT -> {
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
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            switch (currentTool) {
                case PEN -> { penPoints.add(new double[]{e.getX(), e.getY()}); renderCanvas(); }
                case LINE, RECT, OVAL -> { shapeCurX = e.getX(); shapeCurY = e.getY(); renderCanvas(); }
                case SELECT -> {
                    if (dragging && selectedLayer != null) {
                        double dx = (e.getX() - dragStartX) / CELL_SIZE;
                        double dy = (e.getY() - dragStartY) / CELL_SIZE;
                        selectedLayer.setX(Math.max(0, layerDragOrigX + dx));
                        selectedLayer.setY(Math.max(0, layerDragOrigY + dy));
                        renderCanvas();
                    }
                }
                default -> { }
            }
        });

        canvas.setOnMouseReleased(e -> {
            switch (currentTool) {
                case PEN -> {
                    if (penPoints.size() > 1) commitDrawing(Drawing.Type.FREEHAND, new ArrayList<>(penPoints));
                    penPoints.clear();
                    renderCanvas();
                }
                case LINE -> { commitShapeIfDrawing(Drawing.Type.LINE); renderCanvas(); }
                case RECT -> { commitShapeIfDrawing(Drawing.Type.RECTANGLE); renderCanvas(); }
                case OVAL -> { commitShapeIfDrawing(Drawing.Type.OVAL); renderCanvas(); }
                case SELECT -> {
                    boolean wasDragging = dragging;
                    dragging = false;
                    if (selectedLayer != null) {
                        selectedLayer.setX(Math.round(selectedLayer.getX() * 2) / 2.0);
                        selectedLayer.setY(Math.round(selectedLayer.getY() * 2) / 2.0);
                    }
                    if (!wasDragging) {
                        // Not a layer drag: treat as a click-to-place for the selected token.
                        placeSelectedTokenAt(e.getX(), e.getY());
                    }
                    renderCanvas();
                }
                default -> { }
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

    private void setupCanvasDragAndDrop() {
        canvas.setOnDragOver(e -> {
            if (e.getGestureSource() != canvas && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        canvas.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String data = db.getString();
                int sep = data.indexOf("::");
                if (sep > 0) {
                    String type = data.substring(0, sep);
                    String entry = data.substring(sep + 2);
                    success = placeTokenAt(type, idFromListEntry(entry), e.getX(), e.getY());
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void commitShapeIfDrawing(Drawing.Type type) {
        if (!shapeDrawing) return;
        shapeDrawing = false;
        commitDrawing(type, List.of(new double[]{shapeStartX, shapeStartY}, new double[]{shapeCurX, shapeCurY}));
    }

    private void commitDrawing(Drawing.Type type, List<double[]> pixelPoints) {
        List<Double> gridPoints = new ArrayList<>();
        for (double[] p : pixelPoints) {
            gridPoints.add(p[0] / CELL_SIZE);
            gridPoints.add(p[1] / CELL_SIZE);
        }
        Drawing d = new Drawing("drawing_" + System.currentTimeMillis() + "_" + (drawIdCounter++),
            type, toHex(drawColor), drawLineWidth, drawFilled, gridPoints);
        map.getDrawings().add(d);
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    /** Removes the top-most drawing whose bounding box (with a small hit-test margin) contains the click. */
    private void eraseAt(double mx, double my) {
        List<Drawing> list = map.getDrawings();
        for (int i = list.size() - 1; i >= 0; i--) {
            Drawing d = list.get(i);
            List<Double> gp = d.getPoints();
            if (gp.size() < 2) continue;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (int j = 0; j + 1 < gp.size(); j += 2) {
                double px = gp.get(j) * CELL_SIZE, py = gp.get(j + 1) * CELL_SIZE;
                minX = Math.min(minX, px); maxX = Math.max(maxX, px);
                minY = Math.min(minY, py); maxY = Math.max(maxY, py);
            }
            double pad = 6;
            if (mx >= minX - pad && mx <= maxX + pad && my >= minY - pad && my <= maxY + pad) {
                list.remove(i);
                renderCanvas();
                return;
            }
        }
    }

    private String idFromListEntry(String entry) {
        int idx = entry.indexOf(" | ");
        return idx >= 0 ? entry.substring(0, idx) : entry;
    }

    private void placeSelectedTokenAt(double px, double py) {
        if (tokenEntityListView == null) return;
        String sel = tokenEntityListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        placeTokenAt(tokenTypeBox.getValue(), idFromListEntry(sel), px, py);
    }

    private boolean placeTokenAt(String type, String id, double px, double py) {
        int cx = (int) (px / CELL_SIZE);
        int cy = (int) (py / CELL_SIZE);
        if (cx < 0 || cy < 0 || cx >= map.getWidth() || cy >= map.getHeight()) return false;
        try {
            MapObject token = switch (type) {
                case "Player" -> {
                    var pc = repos.players().getById(id);
                    yield pc != null ? new PlayerToken(pc) : null;
                }
                case "NPC" -> {
                    var n = repos.npcs().getById(id);
                    yield n != null ? new NpcToken(n) : null;
                }
                case "Monster" -> {
                    var m = repos.monsters().getById(id);
                    yield m != null ? new MonsterToken(m) : null;
                }
                case "Beast" -> {
                    var b = repos.beasts().getById(id);
                    yield b != null ? new BeastToken(b) : null;
                }
                default -> null;
            };
            if (token == null) return false;
            map.placeObject(token, cx, cy);
            renderCanvas();
            return true;
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not place token: " + ex.getMessage());
            styleDialog(alert);
            alert.showAndWait();
            return false;
        }
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

        // Hand-drawn shapes: persisted ones first, then a live preview of whatever is in progress.
        if (map.getDrawings() != null) {
            for (Drawing d : map.getDrawings()) {
                renderDrawing(gc, d);
            }
        }
        if (currentTool == Tool.PEN && penPoints.size() > 1) {
            paintShape(gc, Drawing.Type.FREEHAND, penPoints, drawColor, drawLineWidth, false);
        } else if (shapeDrawing) {
            Drawing.Type previewType = currentTool == Tool.LINE ? Drawing.Type.LINE
                : currentTool == Tool.OVAL ? Drawing.Type.OVAL : Drawing.Type.RECTANGLE;
            paintShape(gc, previewType,
                List.of(new double[]{shapeStartX, shapeStartY}, new double[]{shapeCurX, shapeCurY}),
                drawColor, drawLineWidth, drawFilled);
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
        List<double[]> pixelPts = new ArrayList<>();
        for (int i = 0; i + 1 < gp.size(); i += 2) {
            pixelPts.add(new double[]{gp.get(i) * CELL_SIZE, gp.get(i + 1) * CELL_SIZE});
        }
        Color c;
        try { c = Color.web(d.getColor()); } catch (Exception ex) { c = Color.web("#c9a84c"); }
        paintShape(gc, d.getType(), pixelPts, c, d.getLineWidth(), d.isFilled());
    }

    private void paintShape(GraphicsContext gc, Drawing.Type type, List<double[]> pts, Color color, double lineWidth, boolean filled) {
        if (pts.isEmpty()) return;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        switch (type) {
            case FREEHAND -> {
                if (pts.size() < 2) return;
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
                if (filled) gc.fillRect(x, y, w, h); else gc.strokeRect(x, y, w, h);
            }
            case OVAL -> {
                double[] a = pts.get(0), b = pts.get(pts.size() - 1);
                double x = Math.min(a[0], b[0]), y = Math.min(a[1], b[1]);
                double w = Math.abs(b[0] - a[0]), h = Math.abs(b[1] - a[1]);
                if (filled) gc.fillOval(x, y, w, h); else gc.strokeOval(x, y, w, h);
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
