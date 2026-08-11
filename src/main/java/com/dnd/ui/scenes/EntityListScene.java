package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.ui.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class EntityListScene extends BaseScene {

    public EntityListScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        EntityCategory cat = uiSession.getActiveEntityCategory();

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        SceneType backTarget = uiSession.isDm() ? SceneType.DM_MENU : SceneType.PLAYER_HOME;
        root.getChildren().add(backBar(backTarget));

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 40, 30, 40));

        String catName = cat != null ? cat.name() : "Entities";
        content.getChildren().addAll(title(catName), subtitle("Campaign: " + campaignName()));

        ListView<HBox> listView = new ListView<>();
        listView.getStyleClass().add("dnd-list-view");
        listView.setPrefHeight(350);

        CampaignRepositories repos = new CampaignRepositories(uiSession.campaignRoot());
        List<?> entities = getEntities(repos, cat);

        for (Object entity : entities) {
            listView.getItems().add(buildRow(entity, cat));
        }

        content.getChildren().add(listView);

        HBox actions = new HBox(10);
        Button viewBtn = btn("View / Edit", () -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= entities.size()) return;
            Object selected = entities.get(idx);
            uiSession.setActiveEntityId(getEntityId(selected));
            if (cat == EntityCategory.MAP) {
                uiSession.setActiveMapId(getEntityId(selected));
                uiSession.getRouter().goTo(uiSession.isDm() ? SceneType.MAP_EDITOR : SceneType.MAP_VIEW);
            } else {
                uiSession.getRouter().goTo(SceneType.ENTITY_DETAIL);
            }
        });
        actions.getChildren().add(viewBtn);

        if (uiSession.isDm()) {
            Button newBtn = btn("New", () -> {
                uiSession.setActiveEntityId(null);
                uiSession.getRouter().goTo(SceneType.ENTITY_DETAIL);
            });
            Button deleteBtn = dangerBtn("Delete", () -> {
                int idx = listView.getSelectionModel().getSelectedIndex();
                if (idx < 0 || idx >= entities.size()) return;
                Object selected = entities.get(idx);
                String id = getEntityId(selected);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete " + catName + " '" + id + "'?", ButtonType.OK, ButtonType.CANCEL);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        deleteEntity(repos, cat, id);
                        uiSession.getRouter().goTo(SceneType.ENTITY_LIST);
                    }
                });
            });
            actions.getChildren().addAll(newBtn, deleteBtn);
        }

        content.getChildren().add(actions);
        root.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return wrapInScene(root);
    }

    private HBox buildRow(Object entity, EntityCategory cat) {
        HBox row = new HBox(12);
        row.setStyle("-fx-padding: 4 8 4 8;");

        String imgPath = getImagePath(entity);
        ImageView thumb = new ImageView();
        thumb.setFitWidth(40);
        thumb.setFitHeight(40);
        thumb.setPreserveRatio(true);
        if (imgPath != null && uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), imgPath);
            if (img != null) thumb.setImage(img);
        }
        if (thumb.getImage() == null) {
            thumb.setImage(createColorPlaceholder(cat));
        }

        Label name = new Label(getEntityName(entity));
        name.setStyle("-fx-text-fill: #d0c5a8; -fx-font-size: 13px;");
        Label id = new Label("  [" + getEntityId(entity) + "]");
        id.setStyle("-fx-text-fill: #6a5a3a; -fx-font-size: 11px;");

        row.getChildren().addAll(thumb, name, id);
        return row;
    }

    private Image createColorPlaceholder(EntityCategory cat) {
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(40, 40);
        javafx.scene.canvas.GraphicsContext gc = c.getGraphicsContext2D();
        Color color = switch (cat) {
            case PLAYER   -> Color.web("#4466aa");
            case NPC      -> Color.web("#66aa44");
            case MONSTER  -> Color.web("#aa4444");
            case BEAST    -> Color.web("#aa7744");
            case ITEM     -> Color.web("#aaaa44");
            case MAP      -> Color.web("#44aaaa");
            case SPELL    -> Color.web("#aa44aa");
            default       -> Color.web("#666666");
        };
        gc.setFill(color);
        gc.fillRoundRect(2, 2, 36, 36, 8, 8);
        gc.setStroke(Color.web("#ffffff44"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(2, 2, 36, 36, 8, 8);
        return c.snapshot(null, null);
    }

    private List<?> getEntities(CampaignRepositories repos, EntityCategory cat) {
        if (cat == null) return List.of();
        return switch (cat) {
            case PLAYER            -> repos.players().list();
            case NPC               -> repos.npcs().list();
            case MONSTER           -> repos.monsters().list();
            case BEAST             -> repos.beasts().list();
            case ITEM              -> repos.items().list();
            case SPELL             -> repos.spells().list();
            case PLACE             -> repos.places().list();
            case MAP               -> repos.maps().list();
            case CLASS             -> repos.classes().list();
            case RACE              -> repos.races().list();
            case DAMAGE_TYPE       -> repos.damageTypes().list();
            case EFFECT            -> repos.effects().list();
            case LANGUAGE          -> repos.languages().list();
            case ALCHEMY_INGREDIENT-> repos.alchemyIngredients().list();
            case BOOK              -> repos.books().list();
            case DICE              -> repos.dice().list();
        };
    }

    private String getEntityId(Object e) {
        try { return (String) e.getClass().getMethod("getId").invoke(e); }
        catch (Exception ex) { return "?"; }
    }
    private String getEntityName(Object e) {
        try { Object n = e.getClass().getMethod("getName").invoke(e); return n != null ? n.toString() : getEntityId(e); }
        catch (Exception ex) { return getEntityId(e); }
    }
    private String getImagePath(Object e) {
        try { return (String) e.getClass().getMethod("getImagePath").invoke(e); }
        catch (Exception ex) { return null; }
    }
    private void deleteEntity(CampaignRepositories repos, EntityCategory cat, String id) {
        switch (cat) {
            case PLAYER  -> repos.players().delete(id);
            case NPC     -> repos.npcs().delete(id);
            case MONSTER -> repos.monsters().delete(id);
            case BEAST   -> repos.beasts().delete(id);
            case ITEM    -> repos.items().delete(id);
            case SPELL   -> repos.spells().delete(id);
            case PLACE   -> repos.places().delete(id);
            case MAP     -> repos.maps().delete(id);
            default      -> {}
        }
    }

    private String campaignName() {
        return uiSession.getSession().getCampaignContext() != null
            ? uiSession.getSession().getCampaignContext().getName() : "(none)";
    }
}
