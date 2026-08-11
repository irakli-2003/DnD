package com.dnd.ui.scenes.player;

import com.dnd.cli.core.CampaignContext;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import com.dnd.ui.scenes.BaseScene;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.util.List;

public class PlayerCampaignSelectionScene extends BaseScene {
    public PlayerCampaignSelectionScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.LANDING));

        VBox content = new VBox(16);
        content.setPadding(new Insets(30, 60, 40, 60));

        List<String> campaigns = uiSession.getStorage().listCustomCampaigns();
        ListView<String> list = new ListView<>();
        list.getStyleClass().add("dnd-list-view");
        list.getItems().addAll(campaigns);
        list.setPrefHeight(200);

        Button selectBtn = btn("Select Campaign", () -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Path path = uiSession.getStorage().resolveCustomCampaignPath(selected);
            uiSession.getSession().setCampaignContext(new CampaignContext(selected, path));
            uiSession.getRouter().goTo(SceneType.PLAYER_CHARACTER_SELECTION);
        });

        content.getChildren().addAll(
            title("Player Mode"),
            subtitle("Select the campaign you're playing in"),
            list,
            selectBtn
        );
        root.getChildren().add(content);
        return wrapInScene(root);
    }
}
