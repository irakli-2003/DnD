package com.dnd.ui.scenes;

import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

public class LandingScene extends BaseScene {
    public LandingScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = centeredVBox(20);
        root.getStyleClass().add("root");

        root.getChildren().addAll(
            title("⚔  DnD Campaign Manager  ⚔"),
            subtitle(uiSession.isLoggedIn()
                ? "Logged in as " + uiSession.getCurrentUser().getUsername() + " - choose your role to begin"
                : "Choose your role to begin"),
            spacer(),
            btn("Dungeon Master", () -> { uiSession.setDm(true); uiSession.getRouter().goTo(SceneType.CAMPAIGN_SELECTION); }),
            // Players don't pick a campaign: the DM hosts one and their token grants entry to it.
            btn("Player", () -> { uiSession.setDm(false); uiSession.getRouter().goTo(SceneType.PLAYER_ONLINE_SESSION); }),
            spacer(),
            btn("Log Out", () -> { uiSession.setCurrentUser(null); uiSession.getRouter().goTo(SceneType.LOGIN); })
        );

        return wrapInScene(root);
    }
}
