package com.dnd.ui.scenes;

import com.dnd.data.StorylineService;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DM-only screen: a file tree of the campaign's storyline (folders and session text files)
 * on the left, and a horizontally-scrollable nested-arc timeline visualization on the right.
 */
public class StorylineScene extends BaseScene {

    private StorylineService service;
    private TreeView<Path> tree;
    private ScrollPane arcScroll;

    public StorylineScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        service = new StorylineService(uiSession.campaignRoot());
        service.ensureRoot();

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.DM_MENU));

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 20, 20, 20));
        content.getChildren().add(title("Storyline"));

        SplitPane split = new SplitPane();
        split.getItems().addAll(buildTreePane(), buildArcPane());
        split.setDividerPositions(0.3);
        VBox.setVgrow(split, Priority.ALWAYS);

        content.getChildren().add(split);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return wrapInScene(root);
    }

    private VBox buildTreePane() {
        VBox pane = new VBox(8);
        pane.setPadding(new Insets(0, 10, 0, 0));
        pane.setPrefWidth(260);

        tree = new TreeView<>();
        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new TreeCell<Path>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });
        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Path selected = selectedPath();
                if (selected != null && service.isSessionFile(selected)) {
                    openEditor(selected);
                }
            }
        });
        VBox.setVgrow(tree, Priority.ALWAYS);

        Button newFolderBtn = btn("New Folder", this::onNewFolder);
        Button newFileBtn = btn("New File", this::onNewFile);
        Button moveBtn = btn("Move to Folder", this::onMove);
        Button deleteBtn = dangerBtn("Delete", this::onDelete);
        newFolderBtn.setDisable(true);
        newFileBtn.setDisable(true);
        moveBtn.setDisable(true);
        deleteBtn.setDisable(true);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSelection = newV != null;
            newFolderBtn.setDisable(!hasSelection);
            newFileBtn.setDisable(!hasSelection);
            moveBtn.setDisable(!hasSelection || (hasSelection && newV.getValue().equals(service.getRoot())));
            deleteBtn.setDisable(!hasSelection || (hasSelection && newV.getValue().equals(service.getRoot())));
        });

        FlowPane actions = new FlowPane(6, 6, newFolderBtn, newFileBtn, moveBtn, deleteBtn);

        pane.getChildren().addAll(tree, actions);
        refreshTree();
        return pane;
    }

    private ScrollPane buildArcPane() {
        arcScroll = new ScrollPane();
        arcScroll.setFitToHeight(true);
        arcScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        arcScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        refreshArc();
        return arcScroll;
    }

    private String displayName(Path item) {
        return item.equals(service.getRoot()) ? "Storyline" : item.getFileName().toString();
    }

    private void refreshTree() {
        TreeItem<Path> rootItem = buildTreeItem(service.getRoot());
        rootItem.setExpanded(true);
        tree.setRoot(rootItem);
    }

    private TreeItem<Path> buildTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        if (service.isFolder(path)) {
            for (Path child : service.listChildren(path)) {
                TreeItem<Path> childItem = buildTreeItem(child);
                childItem.setExpanded(true);
                item.getChildren().add(childItem);
            }
        }
        return item;
    }

    private void refreshArc() {
        arcScroll.setContent(StoryArcView.build(service, service.getRoot()));
    }

    private void refreshAll() {
        refreshTree();
        refreshArc();
    }

    private Path selectedPath() {
        TreeItem<Path> item = tree.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    private Path selectedFolder() {
        Path p = selectedPath();
        if (p == null) return null;
        return service.isFolder(p) ? p : p.getParent();
    }

    private void onNewFolder() {
        Path parent = selectedFolder();
        if (parent == null) return;
        promptName("New Folder", "Folder name:").ifPresent(name -> {
            try {
                service.createFolder(parent, name);
                refreshAll();
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });
    }

    private void onNewFile() {
        Path parent = selectedFolder();
        if (parent == null) return;
        promptName("New File", "File name:").ifPresent(name -> {
            try {
                service.createFile(parent, name);
                refreshAll();
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });
    }

    /** Repeatedly prompts until a non-blank name is entered, or the user cancels. */
    private Optional<String> promptName(String dialogTitle, String prompt) {
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(dialogTitle);
            dialog.setHeaderText(null);
            dialog.setContentText(prompt);
            styleDialog(dialog);
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) return Optional.empty();
            String name = result.get().trim();
            if (!name.isBlank()) return Optional.of(name);
            showError("Name is required.");
        }
    }

    private void onDelete() {
        Path target = selectedPath();
        if (target == null || target.equals(service.getRoot())) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete '" + target.getFileName() + "'?", ButtonType.OK, ButtonType.CANCEL);
        styleDialog(confirm);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                service.delete(target);
                refreshAll();
            }
        });
    }

    private void onMove() {
        Path source = selectedPath();
        if (source == null || source.equals(service.getRoot())) return;
        List<FolderOption> options = new ArrayList<>();
        collectFolderOptions(service.getRoot(), source, "Storyline", options);
        if (options.isEmpty()) {
            showError("No valid destination folder available.");
            return;
        }
        ChoiceDialog<FolderOption> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Move");
        dialog.setHeaderText("Select destination folder for '" + source.getFileName() + "'");
        dialog.setContentText("Destination:");
        styleDialog(dialog);
        dialog.showAndWait().ifPresent(opt -> {
            try {
                service.move(source, opt.path);
                refreshAll();
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });
    }

    /** Collects every folder under {@code folder} as a candidate move destination, excluding {@code excludeSubtree} itself and its descendants. */
    private void collectFolderOptions(Path folder, Path excludeSubtree, String label, List<FolderOption> out) {
        if (folder.equals(excludeSubtree) || folder.startsWith(excludeSubtree)) return;
        out.add(new FolderOption(folder, label));
        for (Path child : service.listChildren(folder)) {
            if (service.isFolder(child)) {
                collectFolderOptions(child, excludeSubtree, label + " / " + child.getFileName(), out);
            }
        }
    }

    private void openEditor(Path file) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(file.getFileName().toString());

        TextArea area = new TextArea(service.readText(file));
        area.setWrapText(true);
        area.setPrefSize(520, 420);

        Button save = new Button("Save");
        save.setOnAction(e -> {
            service.writeText(file, area.getText());
            dialogStage.close();
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> dialogStage.close());

        HBox buttons = new HBox(10, save, cancel);
        buttons.setPadding(new Insets(8, 0, 0, 0));
        VBox layout = new VBox(8, area, buttons);
        layout.setPadding(new Insets(12));
        layout.setStyle("-fx-background-color: #1a1a2e;");

        dialogStage.setScene(themedScene(layout, 560, 480));
        dialogStage.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        styleDialog(alert);
        alert.showAndWait();
    }

    private static final class FolderOption {
        final Path path;
        final String label;
        FolderOption(Path path, String label) { this.path = path; this.label = label; }
        @Override public String toString() { return label; }
    }
}
