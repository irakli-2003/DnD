package com.dnd.ui.scenes;

import com.dnd.ui.EntityCategory;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DmMenuScene extends BaseScene {
    public DmMenuScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.CAMPAIGN_SELECTION));

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 60, 40, 60));
        content.setAlignment(Pos.TOP_LEFT);

        String campaignName = uiSession.getSession().getCampaignContext() != null
            ? uiSession.getSession().getCampaignContext().getName() : "(none)";

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(title("Dungeon Master"));
        if (uiSession.getSession().isOnline()) {
            Label badge = new Label("● LIVE");
            badge.getStyleClass().add("success-badge");
            header.getChildren().add(badge);
        }

        content.getChildren().addAll(
            header,
            subtitle("Campaign: " + campaignName),
            spacer()
        );

        content.getChildren().add(sectionLabel("Manage Content"));
        HBox row1 = new HBox(12);
        row1.getChildren().addAll(
            categoryBtn("Players",  EntityCategory.PLAYER),
            categoryBtn("NPCs",     EntityCategory.NPC),
            categoryBtn("Monsters", EntityCategory.MONSTER),
            categoryBtn("Items",    EntityCategory.ITEM)
        );
        HBox row2 = new HBox(12);
        row2.getChildren().addAll(
            categoryBtn("Spells",   EntityCategory.SPELL),
            categoryBtn("Places",   EntityCategory.PLACE),
            categoryBtn("Maps",     EntityCategory.MAP),
            categoryBtn("More...",  null)
        );
        content.getChildren().addAll(row1, row2);
        content.getChildren().add(
            btn("Online Session" + (uiSession.getSession().isOnline() ? "  ●" : ""),
                () -> uiSession.getRouter().goTo(SceneType.DM_ONLINE_SESSION))
        );

        root.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return wrapInScene(root);
    }

    private Button categoryBtn(String label, EntityCategory cat) {
        return btn(label, () -> {
            if (cat == null) {
                uiSession.setActiveEntityCategory(EntityCategory.LANGUAGE);
            } else {
                uiSession.setActiveEntityCategory(cat);
            }
            uiSession.getRouter().goTo(SceneType.ENTITY_LIST);
        });
    }
}

