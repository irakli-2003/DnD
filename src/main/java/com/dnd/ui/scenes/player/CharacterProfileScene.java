package com.dnd.ui.scenes.player;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.ui.*;
import com.dnd.ui.scenes.BaseScene;
import com.dnd.ui.SceneType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class CharacterProfileScene extends BaseScene {

    public CharacterProfileScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        CampaignRepositories repos = new CampaignRepositories(uiSession.campaignRoot());
        PlayerCharacter pc = repos.players().getById(uiSession.getSession().getActivePlayerCharacterId());

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.PLAYER_HOME));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1a1a2e;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 60, 40, 60));

        if (pc == null) {
            content.getChildren().add(body("Character not found."));
            scroll.setContent(content);
            root.getChildren().add(scroll);
            return wrapInScene(root);
        }

        String name = pc.getName() != null ? pc.getName() : pc.getId();
        content.getChildren().add(title("Profile: " + name));

        ImageView portrait = new ImageView();
        portrait.setFitWidth(200);
        portrait.setFitHeight(200);
        portrait.setPreserveRatio(true);
        portrait.setStyle("-fx-border-color: #c9a84c; -fx-border-width: 2px;");

        if (uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), pc.getImagePath());
            if (img != null) portrait.setImage(img);
        }

        Button uploadBtn = btn("Upload Portrait", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Portrait");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.webp"));
            File chosen = fc.showOpenDialog(null);
            if (chosen != null && uiSession.campaignRoot() != null) {
                try {
                    String rel = ImageStore.copyImage(uiSession.campaignRoot(), "characters", pc.getId(), chosen);
                    pc.setImagePath(rel);
                    repos.players().save(pc);
                    portrait.setImage(ImageStore.load(uiSession.campaignRoot(), rel));
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Upload failed: " + ex.getMessage()).showAndWait();
                }
            }
        });

        VBox portraitBox = new VBox(10, portrait, uploadBtn);
        portraitBox.setAlignment(Pos.TOP_CENTER);

        VBox statsBox = buildStatsBox(pc, repos);

        HBox topSection = new HBox(30, portraitBox, statsBox);
        topSection.setAlignment(Pos.TOP_LEFT);
        content.getChildren().add(topSection);

        content.getChildren().add(sectionLabel("Inventory"));
        if (pc.getItems() != null && !pc.getItems().isEmpty()) {
            for (var pi : pc.getItems()) {
                var item = repos.items().getById(pi.getItemId());
                String itemName = item != null ? item.getName() : pi.getItemId();
                HBox row = new HBox(10);
                ImageView itemThumb = new ImageView();
                itemThumb.setFitWidth(32); itemThumb.setFitHeight(32);
                if (item != null && item.getImagePath() != null && uiSession.campaignRoot() != null) {
                    Image itemImg = ImageStore.load(uiSession.campaignRoot(), item.getImagePath());
                    if (itemImg != null) itemThumb.setImage(itemImg);
                }
                Label itemLabel = body(itemName + (pi.isEquipped() ? " ✓" : ""));
                row.getChildren().addAll(itemThumb, itemLabel);
                content.getChildren().add(row);
            }
        } else {
            content.getChildren().add(body("No items."));
        }

        content.getChildren().add(sectionLabel("Spells / Abilities"));
        if (pc.getSpells() != null && !pc.getSpells().isEmpty()) {
            for (var ps : pc.getSpells()) {
                var spell = repos.spells().getById(ps.getSpellId());
                String spellName = spell != null ? spell.getName() : ps.getSpellId();
                content.getChildren().add(body("• " + spellName + " (Rank " + ps.getRank() + ")"));
            }
        } else {
            content.getChildren().add(body("No spells."));
        }

        scroll.setContent(content);
        root.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapInScene(root);
    }

    private VBox buildStatsBox(PlayerCharacter pc, CampaignRepositories repos) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #0f0f1e; -fx-border-color: #3a3a5a; -fx-border-width: 1px;");
        box.setPrefWidth(380);

        String className = resolve(repos.classes().getById(pc.getClassId()));
        String raceName  = resolve(repos.races().getById(pc.getRaceId()));

        box.getChildren().addAll(
            sectionLabel(pc.getName() != null ? pc.getName() : "Unknown"),
            body("Class: " + className),
            body("Race: " + raceName),
            body("Level: " + pc.getLevel())
        );

        CoreStats s = pc.getStats();
        if (s != null) {
            box.getChildren().addAll(
                new Separator(),
                statRow("Strength",     s.getStrength()),
                statRow("Dexterity",    s.getDexterity()),
                statRow("Constitution", s.getConstitution()),
                statRow("Intelligence", s.getIntelligence()),
                statRow("Wisdom",       s.getWisdom()),
                statRow("Charisma",     s.getCharisma())
            );
        }
        return box;
    }

    private HBox statRow(String name, int value) {
        Label n = body(name + ":");
        n.setMinWidth(110);
        Label v = body(String.valueOf(value));
        v.setStyle("-fx-text-fill: #c9a84c; -fx-font-weight: bold;");
        return new HBox(8, n, v);
    }

    private String resolve(Object entity) {
        if (entity == null) return "Unknown";
        try { return entity.getClass().getMethod("getName").invoke(entity).toString(); }
        catch (Exception e) { return "Unknown"; }
    }
}
