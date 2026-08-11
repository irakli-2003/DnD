package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.ui.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class EntityDetailScene extends BaseScene {

    public EntityDetailScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        EntityCategory cat = uiSession.getActiveEntityCategory();
        String entityId = uiSession.getActiveEntityId();
        boolean isNew = (entityId == null);

        VBox root = new VBox(0);
        root.getStyleClass().add("root");
        root.getChildren().add(backBar(SceneType.ENTITY_LIST));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1a1a2e;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 60, 40, 60));

        String catLabel = cat != null ? cat.name() : "";
        content.getChildren().add(title(isNew ? "New " + catLabel : "Edit " + catLabel));

        CampaignRepositories repos = new CampaignRepositories(uiSession.campaignRoot());
        Object entity = isNew ? null : getEntity(repos, cat, entityId);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #c9a84c; -fx-border-width: 1px;");
        String currentImgPath = entity != null ? getImagePath(entity) : null;
        if (currentImgPath != null && uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), currentImgPath);
            if (img != null) imageView.setImage(img);
        }

        final String[] newImagePath = {currentImgPath};
        Button uploadImageBtn = btn("Upload Image", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"));
            File chosen = fc.showOpenDialog(null);
            if (chosen != null && uiSession.campaignRoot() != null) {
                try {
                    String id = isNew ? "new_" + System.currentTimeMillis() : entityId;
                    String relative = ImageStore.copyImage(uiSession.campaignRoot(),
                        (cat != null ? cat.name().toLowerCase() : "misc") + "s", id, chosen);
                    newImagePath[0] = relative;
                    imageView.setImage(ImageStore.load(uiSession.campaignRoot(), relative));
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to upload: " + ex.getMessage()).showAndWait();
                }
            }
        });

        HBox imageSection = new HBox(16, imageView, uploadImageBtn);
        imageSection.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        content.getChildren().add(imageSection);

        Map<String, TextField> fieldEditors = new LinkedHashMap<>();
        List<Method> editableSetters = getEditableSetters(cat);

        for (Method setter : editableSetters) {
            String fieldName = setterToFieldName(setter);
            if (fieldName.equals("imagePath") || fieldName.equals("passwordHash") || fieldName.equals("passwordSalt")) continue;
            String currentValue = entity != null ? getFieldValue(entity, fieldName) : "";
            TextField tf = textField(fieldName);
            tf.setText(currentValue != null ? currentValue : "");
            tf.setMaxWidth(500);
            Label lbl = new Label(fieldName);
            lbl.setStyle("-fx-text-fill: #a89060; -fx-font-size: 12px;");
            VBox group = new VBox(4, lbl, tf);
            content.getChildren().add(group);
            fieldEditors.put(fieldName, tf);
        }

        Label errorLabel = body("");
        errorLabel.setStyle("-fx-text-fill: #e07070;");
        content.getChildren().add(errorLabel);

        if (uiSession.isDm()) {
            Button saveBtn = btn("Save", () -> {
                try {
                    Object target = isNew ? createNewEntity(cat) : entity;
                    for (Map.Entry<String, TextField> e : fieldEditors.entrySet()) {
                        setFieldValue(target, e.getKey(), e.getValue().getText());
                    }
                    setImagePath(target, newImagePath[0]);
                    saveEntity(repos, cat, target);
                    uiSession.getRouter().goTo(SceneType.ENTITY_LIST);
                } catch (Exception ex) {
                    errorLabel.setText("Save failed: " + ex.getMessage());
                }
            });
            content.getChildren().add(saveBtn);
        }

        scroll.setContent(content);
        root.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapInScene(root);
    }

    private Object getEntity(CampaignRepositories repos, EntityCategory cat, String id) {
        if (cat == null) return null;
        return switch (cat) {
            case PLAYER  -> repos.players().getById(id);
            case NPC     -> repos.npcs().getById(id);
            case MONSTER -> repos.monsters().getById(id);
            case BEAST   -> repos.beasts().getById(id);
            case ITEM    -> repos.items().getById(id);
            case SPELL   -> repos.spells().getById(id);
            case PLACE   -> repos.places().getById(id);
            case MAP     -> repos.maps().getById(id);
            default      -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private void saveEntity(CampaignRepositories repos, EntityCategory cat, Object entity) {
        if (cat == null) throw new IllegalArgumentException("No category");
        switch (cat) {
            case PLAYER  -> repos.players().save((com.dnd.model.character.PlayerCharacter) entity);
            case NPC     -> repos.npcs().save((com.dnd.model.creature.Npc) entity);
            case MONSTER -> repos.monsters().save((com.dnd.model.creature.Monster) entity);
            case BEAST   -> repos.beasts().save((com.dnd.model.creature.Beast) entity);
            case ITEM    -> repos.items().save((com.dnd.model.item.Item) entity);
            case SPELL   -> repos.spells().save((com.dnd.model.magic.Spell) entity);
            case PLACE   -> repos.places().save((com.dnd.model.world.Place) entity);
            case MAP     -> repos.maps().save((com.dnd.model.world.map.GameMap) entity);
            default      -> throw new IllegalArgumentException("Cannot save: " + cat);
        }
    }

    private Object createNewEntity(EntityCategory cat) throws Exception {
        if (cat == null) throw new IllegalArgumentException("No category");
        if (cat == EntityCategory.ITEM) {
            return com.dnd.model.item.books.Book.class.getDeclaredConstructor().newInstance();
        }
        Class<?> cls = switch (cat) {
            case PLAYER  -> com.dnd.model.character.PlayerCharacter.class;
            case NPC     -> com.dnd.model.creature.Npc.class;
            case MONSTER -> com.dnd.model.creature.Monster.class;
            case BEAST   -> com.dnd.model.creature.Beast.class;
            case SPELL   -> com.dnd.model.magic.Spell.class;
            case PLACE   -> com.dnd.model.world.Place.class;
            case MAP     -> com.dnd.model.world.map.GameMap.class;
            default      -> throw new IllegalArgumentException("Cannot create: " + cat);
        };
        return cls.getDeclaredConstructor().newInstance();
    }

    private List<Method> getEditableSetters(EntityCategory cat) {
        try {
            Class<?> cls = getEntityClass(cat);
            if (cls == null) return List.of();
            List<Method> setters = new ArrayList<>();
            for (Method m : cls.getMethods()) {
                if (m.getName().startsWith("set") && m.getParameterCount() == 1
                    && (m.getParameterTypes()[0] == String.class || m.getParameterTypes()[0] == int.class
                        || m.getParameterTypes()[0] == long.class || m.getParameterTypes()[0] == double.class)) {
                    setters.add(m);
                }
            }
            return setters;
        } catch (Exception e) { return List.of(); }
    }

    private Class<?> getEntityClass(EntityCategory cat) {
        if (cat == null) return null;
        return switch (cat) {
            case PLAYER  -> com.dnd.model.character.PlayerCharacter.class;
            case NPC     -> com.dnd.model.creature.Npc.class;
            case MONSTER -> com.dnd.model.creature.Monster.class;
            case BEAST   -> com.dnd.model.creature.Beast.class;
            case ITEM    -> com.dnd.model.item.Item.class;
            case SPELL   -> com.dnd.model.magic.Spell.class;
            case PLACE   -> com.dnd.model.world.Place.class;
            case MAP     -> com.dnd.model.world.map.GameMap.class;
            default      -> null;
        };
    }

    private String setterToFieldName(Method setter) {
        String n = setter.getName().substring(3);
        return Character.toLowerCase(n.charAt(0)) + n.substring(1);
    }

    private String getFieldValue(Object entity, String fieldName) {
        try {
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method m = entity.getClass().getMethod(getter);
            Object v = m.invoke(entity);
            return v != null ? v.toString() : "";
        } catch (Exception e) { return ""; }
    }

    private void setFieldValue(Object entity, String fieldName, String value) {
        try {
            String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method m = entity.getClass().getMethod(setter, String.class);
                m.invoke(entity, value); return;
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = entity.getClass().getMethod(setter, int.class);
                if (!value.isBlank()) m.invoke(entity, Integer.parseInt(value.trim())); return;
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = entity.getClass().getMethod(setter, double.class);
                if (!value.isBlank()) m.invoke(entity, Double.parseDouble(value.trim()));
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception ignored) {}
    }

    private String getImagePath(Object entity) {
        try { return (String) entity.getClass().getMethod("getImagePath").invoke(entity); }
        catch (Exception e) { return null; }
    }

    private void setImagePath(Object entity, String path) {
        try { entity.getClass().getMethod("setImagePath", String.class).invoke(entity, path); }
        catch (Exception ignored) {}
    }
}
