package com.dnd.ui.scenes.player;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import com.dnd.ui.scenes.BaseScene;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PlayerCharacterSelectionScene extends BaseScene {
    public PlayerCharacterSelectionScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.PLAYER_CAMPAIGN_SELECTION));

        VBox content = new VBox(16);
        content.setPadding(new Insets(30, 60, 40, 60));

        Label error = body("");
        error.setStyle("-fx-text-fill: #e07070;");

        CampaignRepositories repos = new CampaignRepositories(
            uiSession.getSession().getCampaignContext().getPath());
        List<PlayerCharacter> players = repos.players().list();

        ListView<String> list = new ListView<>();
        list.getStyleClass().add("dnd-list-view");
        for (PlayerCharacter pc : players) {
            String label = pc.getName() != null && !pc.getName().isEmpty() ? pc.getName() : pc.getId();
            list.getItems().add(label);
        }
        list.setPrefHeight(200);

        PasswordField passField = passwordField("Password (if required)");
        Label passHint = body("Enter password if this character is password protected.");

        Button selectBtn = btn("Play This Character", () -> {
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx < 0) { error.setText("Please select a character."); return; }
            PlayerCharacter pc = players.get(idx);
            if (pc.hasPassword()) {
                if (!pc.checkPassword(passField.getText())) {
                    error.setText("Incorrect password.");
                    return;
                }
            }
            uiSession.getSession().setActivePlayerCharacterId(pc.getId());
            uiSession.getRouter().goTo(SceneType.PLAYER_HOME);
        });

        content.getChildren().addAll(
            title("Choose Your Character"),
            subtitle("Campaign: " + uiSession.getSession().getCampaignContext().getName()),
            list, passHint, passField, error, selectBtn
        );
        root.getChildren().add(content);
        return wrapInScene(root);
    }
}
