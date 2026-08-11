package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.JsonMappers;
import com.dnd.security.FirebaseSessionSync;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class OnlineSessionScene extends BaseScene {

    private static final String FIREBASE_URL =
        "https://dnd-campaign-f8c23-default-rtdb.europe-west1.firebasedatabase.app";

    public OnlineSessionScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.DM_MENU));

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 60, 40, 60));

        if (uiSession.getSession().isOnline()) {
            buildActiveView(content);
        } else {
            buildStartView(content);
        }

        root.getChildren().add(content);
        return wrapInScene(root);
    }

    private void buildStartView(VBox content) {
        PasswordField passphraseField = passwordField("Session passphrase (share with players)");
        Label error = body("");
        error.setStyle("-fx-text-fill: #e07070;");

        Button startBtn = btn("Start Online Session", () -> {
            String passphrase = passphraseField.getText().trim();
            if (passphrase.isEmpty()) { error.setText("Passphrase cannot be empty."); return; }
            if (uiSession.getSession().getCampaignContext() == null) {
                error.setText("No campaign selected.");
                return;
            }

            String sessionId = FirebaseSessionSync.generateSessionId();
            FirebaseSessionSync sync = new FirebaseSessionSync(FIREBASE_URL, sessionId, passphrase);

            try {
                sync.push(buildSnapshot());
            } catch (FirebaseSessionSync.SyncException e) {
                error.setText("Firebase error: " + e.getMessage());
                return;
            }

            String token = FirebaseSessionSync.generateToken(FIREBASE_URL, sessionId, passphrase);
            uiSession.getSession().setFirebaseSync(sync);
            uiSession.setSessionToken(token);
            uiSession.getRouter().goTo(SceneType.DM_ONLINE_SESSION);
        });

        content.getChildren().addAll(
            title("Start Online Session"),
            body("Players will connect using the generated token. All data is AES-256 encrypted."),
            sectionLabel("Passphrase"),
            passphraseField,
            error,
            startBtn
        );
    }

    private void buildActiveView(VBox content) {
        Label statusLabel = body("Session is live. Share the token below with your players.");
        statusLabel.setStyle("-fx-text-fill: #80ff80;");

        String token = uiSession.getSessionToken();
        TextArea tokenArea = new TextArea(token != null ? token : "(token unavailable)");
        tokenArea.setEditable(false);
        tokenArea.setWrapText(true);
        tokenArea.setPrefRowCount(3);
        tokenArea.getStyleClass().add("dnd-text-field");
        tokenArea.setMaxWidth(700);

        Button stopBtn = dangerBtn("Stop Session", () -> {
            try { uiSession.getSession().getFirebaseSync().delete(); } catch (Exception ignored) {}
            uiSession.getSession().setFirebaseSync(null);
            uiSession.setSessionToken(null);
            uiSession.getRouter().goTo(SceneType.DM_ONLINE_SESSION);
        });

        Button pushBtn = btn("Push State Now", () -> {
            try {
                uiSession.getSession().getFirebaseSync().push(buildSnapshot());
            } catch (Exception ignored) {}
        });

        content.getChildren().addAll(
            title("Online Session  ● LIVE"),
            body("Campaign state is pushed automatically when you navigate back to the DM Menu."),
            statusLabel,
            sectionLabel("Player Token"),
            tokenArea,
            new HBox(12, pushBtn, stopBtn)
        );
    }

    private String buildSnapshot() {
        try {
            ObjectMapper mapper = JsonMappers.create();
            CampaignRepositories repos = new CampaignRepositories(
                uiSession.getSession().getCampaignContext().getPath());
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("campaign", uiSession.getSession().getCampaignContext().getName());
            snapshot.put("players", repos.players().list());
            snapshot.put("maps", repos.maps().list());
            snapshot.put("npcs", repos.npcs().list());
            snapshot.put("monsters", repos.monsters().list());
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{\"campaign\":\"" + uiSession.getSession().getCampaignContext().getName() + "\"}";
        }
    }
}
