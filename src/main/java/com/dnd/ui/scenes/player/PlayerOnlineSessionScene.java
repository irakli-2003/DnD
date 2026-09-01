package com.dnd.ui.scenes.player;

import com.dnd.cli.core.CampaignContext;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.SessionSnapshot;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.security.FirebaseSessionSync;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import com.dnd.ui.scenes.BaseScene;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.util.List;

/**
 * The player's way in. There is no campaign list here on purpose: the DM decides what is
 * live, and a player's token is what grants them access to it.
 */
public class PlayerOnlineSessionScene extends BaseScene {

    public PlayerOnlineSessionScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.LANDING));

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

        Button joinBtn = btn("Join Session", () -> join(tokenField.getText().trim(), error));
        tokenField.setOnAction(e -> join(tokenField.getText().trim(), error));

        content.getChildren().addAll(
            title("Join Your DM's Session"),
            body("Enter the token your DM shared with you. You'll be taken straight into the "
                + "campaign they're running, as your own character."),
            tokenField, error, joinBtn
        );
    }

    private void join(String token, Label error) {
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

        Path campaignRoot;
        try {
            campaignRoot = SessionSnapshot.hydrate(json, sync.getSessionId());
        } catch (RuntimeException e) {
            error.setText("Could not open the session data: " + e.getMessage());
            return;
        }

        uiSession.getSession().setCampaignContext(
            new CampaignContext(SessionSnapshot.campaignNameOf(json), campaignRoot));
        uiSession.getSession().setFirebaseSync(sync);
        uiSession.setSessionToken(token);

        // A later push from the DM refreshes the local copy in place, so the player sees
        // the updated campaign without having to rejoin.
        sync.startListening(update -> Platform.runLater(() -> {
            try {
                SessionSnapshot.hydrate(update, sync.getSessionId());
            } catch (RuntimeException ignored) {
                // A malformed update just leaves the last good copy in place.
            }
        }));

        uiSession.getRouter().goTo(destinationFor(campaignRoot));
    }

    /**
     * Sends the player straight to their own character when the DM has assigned them one,
     * and only falls back to the picker when the roster can't say who they are.
     */
    private SceneType destinationFor(Path campaignRoot) {
        String username = uiSession.isLoggedIn() ? uiSession.getCurrentUser().getUsername() : null;
        if (username != null) {
            List<PlayerCharacter> roster = new CampaignRepositories(campaignRoot).players().list();
            for (PlayerCharacter pc : roster) {
                if (pc.isOwnedBy(username)) {
                    uiSession.getSession().setActivePlayerCharacterId(pc.getId());
                    return SceneType.PLAYER_HOME;
                }
            }
        }
        return SceneType.PLAYER_CHARACTER_SELECTION;
    }

    private void buildConnectedView(VBox root, VBox content) {
        HBox notifBar = new HBox();
        notifBar.getStyleClass().add("notification-bar");
        Label notifLabel = new Label("● Connected — updates arrive automatically from the DM");
        notifLabel.getStyleClass().add("notification-label");
        notifBar.getChildren().add(notifLabel);
        root.getChildren().add(1, notifBar);

        var context = uiSession.getSession().getCampaignContext();

        content.getChildren().addAll(
            title("Live Session"),
            body("Campaign: " + (context != null ? context.getName() : "unknown")),
            body("Your DM is hosting this session. Your character sheet follows what they push; "
                + "your portrait, name and description are yours and stay on this machine."),
            btn("Go to My Character", () -> uiSession.getRouter().goTo(
                uiSession.getSession().getActivePlayerCharacterId() != null
                    ? SceneType.PLAYER_HOME
                    : SceneType.PLAYER_CHARACTER_SELECTION)),
            dangerBtn("Leave Session", () -> {
                try { uiSession.getSession().getFirebaseSync().stopListening(); } catch (Exception ignored) {}
                uiSession.getSession().setFirebaseSync(null);
                uiSession.getSession().setActivePlayerCharacterId(null);
                uiSession.setSessionToken(null);
                uiSession.getRouter().goTo(SceneType.PLAYER_ONLINE_SESSION);
            })
        );
    }
}
