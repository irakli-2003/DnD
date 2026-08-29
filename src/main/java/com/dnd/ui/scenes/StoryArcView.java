package com.dnd.ui.scenes;

import com.dnd.data.StorylineService;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Storyline file tree as a horizontally-scrollable "story arc" timeline:
 * a thick colored line at the bottom where each segment is one session (leaf text file),
 * and nested half-circle arcs above the line for every folder, stretching over the
 * segments of all sessions nested under it. A parent folder's arc naturally encloses its
 * child folders' arcs because its session range is a superset of theirs, so the storyline
 * root's arc always spans the entire timeline.
 */
final class StoryArcView {

    private static final double SLOT_WIDTH = 130;
    private static final double BAR_HEIGHT = 26;
    private static final double TOP_PADDING = 34;
    private static final double BOTTOM_PADDING = 20;
    private static final Color[] PALETTE = {
        Color.web("#c0392b"), Color.web("#2980b9"), Color.web("#27ae60"), Color.web("#8e44ad"),
        Color.web("#d35400"), Color.web("#16a085"), Color.web("#c9a84c"), Color.web("#7f8c8d")
    };
    private static final Color ARC_COLOR = Color.web("#e8d9a0");

    private StoryArcView() {}

    static Pane build(StorylineService service, Path root) {
        List<Path> sessions = new ArrayList<>();
        collectSessions(service, root, sessions);

        double contentWidth = Math.max(SLOT_WIDTH, sessions.size() * SLOT_WIDTH);
        double maxRadius = contentWidth / 2.0;
        double baselineY = maxRadius + TOP_PADDING;
        double paneHeight = baselineY + BAR_HEIGHT + BOTTOM_PADDING;

        Pane pane = new Pane();
        pane.setPrefSize(contentWidth, paneHeight);
        pane.setMinSize(contentWidth, paneHeight);
        pane.setStyle("-fx-background-color: #14141f;");

        if (sessions.isEmpty()) {
            Label empty = new Label("No sessions yet. Add folders and files on the left to build the story arc.");
            empty.setStyle("-fx-text-fill: #a89060; -fx-font-size: 12px;");
            empty.setLayoutX(16);
            empty.setLayoutY(20);
            pane.getChildren().add(empty);
            return pane;
        }

        for (int i = 0; i < sessions.size(); i++) {
            double x = i * SLOT_WIDTH;
            Rectangle bar = new Rectangle(x + 1, baselineY, SLOT_WIDTH - 2, BAR_HEIGHT);
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bar.setFill(PALETTE[i % PALETTE.length]);
            pane.getChildren().add(bar);

            Text label = new Text(stripExtension(sessions.get(i).getFileName().toString()));
            label.setFill(Color.WHITE);
            label.setStyle("-fx-font-size: 10px;");
            label.setX(x + 5);
            label.setY(baselineY + BAR_HEIGHT / 2.0 + 4);
            label.setWrappingWidth(SLOT_WIDTH - 10);
            pane.getChildren().add(label);
        }

        addFolderArcs(pane, service, root, root, sessions, baselineY);
        return pane;
    }

    /** Recursively draws an arc for {@code folder} (and all its subfolders) spanning the range of its descendant sessions. */
    private static int[] addFolderArcs(Pane pane, StorylineService service, Path root, Path folder, List<Path> sessions, double baselineY) {
        int minIdx = Integer.MAX_VALUE;
        int maxIdx = -1;
        for (Path child : service.listChildren(folder)) {
            if (service.isFolder(child)) {
                int[] childRange = addFolderArcs(pane, service, root, child, sessions, baselineY);
                if (childRange != null) {
                    minIdx = Math.min(minIdx, childRange[0]);
                    maxIdx = Math.max(maxIdx, childRange[1]);
                }
            } else {
                int idx = sessions.indexOf(child);
                if (idx >= 0) {
                    minIdx = Math.min(minIdx, idx);
                    maxIdx = Math.max(maxIdx, idx);
                }
            }
        }
        if (maxIdx < 0) return null; // empty folder: nothing to draw, nothing to report upward

        double x1 = minIdx * SLOT_WIDTH;
        double x2 = (maxIdx + 1) * SLOT_WIDTH;
        double centerX = (x1 + x2) / 2.0;
        double radius = (x2 - x1) / 2.0;

        Arc arc = new Arc(centerX, baselineY, radius, radius, 0, 180);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(ARC_COLOR);
        arc.setStrokeWidth(2);
        pane.getChildren().add(arc);

        String name = folder.equals(root) ? "Storyline" : folder.getFileName().toString();
        Text label = new Text(name);
        label.setFill(ARC_COLOR);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        label.setX(centerX - Math.min(radius, name.length() * 3.2));
        label.setY(baselineY - radius - 8);
        pane.getChildren().add(label);

        return new int[]{minIdx, maxIdx};
    }

    private static void collectSessions(StorylineService service, Path folder, List<Path> out) {
        for (Path child : service.listChildren(folder)) {
            if (service.isFolder(child)) {
                collectSessions(service, child, out);
            } else {
                out.add(child);
            }
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
