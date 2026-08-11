package com.dnd.ui.scenes.player;

import com.dnd.security.FirebaseSessionSync;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import com.dnd.ui.scenes.BaseScene;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class PlayerOnlineSessionScene extends BaseScene {
    public PlayerOnlineSessionScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.PLAYER_HOME));

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 60, 40, 60));

        if (uiSession.getSession().isOnline()) {
            buildConnectedView(root, content);
        } else {
            buildJoinView(content);
        }

        root.getChildren().add(content);
        return wrapInScene(root);
    }

    private void buildJoinView(VBox content) {
        TextField tokenField = textField("Paste session token from DM");
        tokenField.setMaxWidth(600);
        Label error = body("");
        error.setStyle("-fx-text-fill: #e07070;");

        Button joinBtn = btn("Join Session", () -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) { error.setText("Token cannot be empty."); return; }

            FirebaseSessionSync sync;
            try {
                sync = FirebaseSessionSync.fromToken(token);
            } catch (IllegalArgumentException e) {
                error.setText("Invalid token: " + e.getMessage());
                return;
            }

            String json;
            try {
                json = sync.pull();
            } catch (Exception e) {
                error.setText("Connection failed: " + e.getMessage());
                return;
            }

            if (json == null) {
                error.setText("Session not started yet. Ask your DM to start the session.");
                return;
            }

            sync.startListening(update -> Platform.runLater(() ->
                uiSession.getRouter().goTo(SceneType.PLAYER_ONLINE_SESSION)));

            uiSession.getSession().setFirebaseSync(sync);
            uiSession.getRouter().goTo(SceneType.PLAYER_ONLINE_SESSION);
        });

        content.getChildren().addAll(
            title("Join Online Session"),
            body("Enter the token your DM shared with you to connect to the live game."),
            tokenField, error, joinBtn
        );
    }

    private void buildConnectedView(VBox root, VBox content) {
        HBox notifBar = new HBox();
        notifBar.getStyleClass().add("notification-bar");
        Label notifLabel = new Label("● Connected — updates arrive automatically from the DM");
        notifLabel.getStyleClass().add("notification-label");
        notifBar.getChildren().add(notifLabel);
        root.getChildren().add(1, notifBar);

        String json = uiSession.getSession().getFirebaseSync().getLatestSnapshot();

        content.getChildren().addAll(
            title("Live Session"),
            buildSnapshotView(json),
            dangerBtn("Disconnect", () -> {
                uiSession.getSession().setFirebaseSync(null);
                uiSession.getRouter().goTo(SceneType.PLAYER_ONLINE_SESSION);
            })
        );
    }

    private javafx.scene.Node buildSnapshotView(String json) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #0f0f1e; -fx-border-color: #3a3a5a; -fx-border-width: 1px;");

        if (json == null) {
            box.getChildren().add(body("Waiting for DM to push session state..."));
            return box;
        }

        try {
            ObjectMapper mapper = com.dnd.data.JsonMappers.create();
            JsonNode rootNode = mapper.readTree(json);

            if (rootNode.has("campaign")) {
                box.getChildren().add(sectionLabel("Campaign: " + rootNode.get("campaign").asText()));
            }
            if (rootNode.has("players")) {
                box.getChildren().add(body("Players: " + rootNode.get("players").size()));
                for (JsonNode p : rootNode.get("players")) {
                    String n = p.has("name") ? p.get("name").asText() : p.path("id").asText("?");
                    box.getChildren().add(body("  • " + n + " (Lv " + p.path("level").asInt(1) + ")"));
                }
            }
            if (rootNode.has("maps")) {
                box.getChildren().add(body("Maps: " + rootNode.get("maps").size()));
                for (JsonNode m : rootNode.get("maps")) {
                    box.getChildren().add(body("  • " + m.path("name").asText("?")));
                }
            }
            if (rootNode.has("npcs"))     box.getChildren().add(body("NPCs: "     + rootNode.get("npcs").size()));
            if (rootNode.has("monsters")) box.getChildren().add(body("Monsters: " + rootNode.get("monsters").size()));
        } catch (Exception e) {
            box.getChildren().add(body("(Could not parse session data)"));
        }
        return box;
    }
}
