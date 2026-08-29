package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.CharacterRace;
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
import java.util.function.Function;

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
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to upload: " + ex.getMessage());
                    styleDialog(alert);
                    alert.showAndWait();
                }
            }
        });

        HBox imageSection = new HBox(16, imageView, uploadImageBtn);
        imageSection.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        content.getChildren().add(imageSection);

        Map<String, Control> fieldEditors = new LinkedHashMap<>();
        List<Method> editableSetters = getEditableSetters(cat);

        List<CharacterClass> classOptions = cat == EntityCategory.PLAYER ? repos.classes().list() : List.of();
        List<CharacterRace> raceOptions = cat == EntityCategory.PLAYER ? repos.races().list() : List.of();

        for (Method setter : editableSetters) {
            String fieldName = setterToFieldName(setter);
            if (fieldName.equals("imagePath") || fieldName.equals("passwordHash") || fieldName.equals("passwordSalt")) continue;
            String currentValue = entity != null ? getFieldValue(entity, fieldName) : "";

            Label lbl = new Label(fieldName);
            lbl.setStyle("-fx-text-fill: #a89060; -fx-font-size: 12px;");

            Control editor;
            if (fieldName.equals("classId") && !classOptions.isEmpty()) {
                editor = idComboBox(classOptions, CharacterClass::getId, CharacterClass::getName, currentValue);
            } else if (fieldName.equals("raceId") && !raceOptions.isEmpty()) {
                editor = idComboBox(raceOptions, CharacterRace::getId, CharacterRace::getName, currentValue);
            } else if (setter.getParameterTypes()[0] == boolean.class) {
                CheckBox cb = checkBox(fieldName);
                cb.setSelected(Boolean.parseBoolean(currentValue));
                editor = cb;
            } else {
                TextField tf = textField(fieldName);
                tf.setText(currentValue != null ? currentValue : "");
                tf.setMaxWidth(500);
                editor = tf;
            }

            VBox group = new VBox(4, lbl, editor);
            content.getChildren().add(group);
            fieldEditors.put(fieldName, editor);
        }

        Label errorLabel = body("");
        errorLabel.setStyle("-fx-text-fill: #e07070;");
        content.getChildren().add(errorLabel);

        if (uiSession.isDm()) {
            Button saveBtn = btn("Save", () -> {
                try {
                    Object target = isNew ? createNewEntity(cat) : entity;
                    for (Map.Entry<String, Control> e : fieldEditors.entrySet()) {
                        setFieldValue(target, e.getKey(), controlValue(e.getValue()));
                    }
                    setImagePath(target, newImagePath[0]);
                    // Width/height are edited here as plain fields, but only GameMap's own
                    // constructor builds a matching grid - keep grid dimensions in sync so the
                    // map can actually be opened afterwards in the Map Editor/View.
                    if (target instanceof com.dnd.model.world.map.GameMap gm) {
                        gm.ensureGridSize();
                    }
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
            case PLAYER             -> repos.players().getById(id);
            case NPC                -> repos.npcs().getById(id);
            case MONSTER            -> repos.monsters().getById(id);
            case BEAST              -> repos.beasts().getById(id);
            case ITEM               -> repos.items().getById(id);
            case SPELL              -> repos.spells().getById(id);
            case PLACE              -> repos.places().getById(id);
            case MAP                -> repos.maps().getById(id);
            case CLASS              -> repos.classes().getById(id);
            case RACE               -> repos.races().getById(id);
            case DAMAGE_TYPE        -> repos.damageTypes().getById(id);
            case EFFECT             -> repos.effects().getById(id);
            case LANGUAGE           -> repos.languages().getById(id);
            case ALCHEMY_INGREDIENT -> repos.alchemyIngredients().getById(id);
            case BOOK               -> repos.books().getById(id);
            case DICE               -> repos.dice().getById(id);
        };
    }

    @SuppressWarnings("unchecked")
    private void saveEntity(CampaignRepositories repos, EntityCategory cat, Object entity) {
        if (cat == null) throw new IllegalArgumentException("No category");
        switch (cat) {
            case PLAYER             -> repos.players().save((com.dnd.model.character.PlayerCharacter) entity);
            case NPC                -> repos.npcs().save((com.dnd.model.creature.Npc) entity);
            case MONSTER            -> repos.monsters().save((com.dnd.model.creature.Monster) entity);
            case BEAST              -> repos.beasts().save((com.dnd.model.creature.Beast) entity);
            case ITEM               -> repos.items().save((com.dnd.model.item.Item) entity);
            case SPELL              -> repos.spells().save((com.dnd.model.magic.Spell) entity);
            case PLACE              -> repos.places().save((com.dnd.model.world.Place) entity);
            case MAP                -> repos.maps().save((com.dnd.model.world.map.GameMap) entity);
            case CLASS              -> repos.classes().save((CharacterClass) entity);
            case RACE               -> repos.races().save((CharacterRace) entity);
            case DAMAGE_TYPE        -> repos.damageTypes().save((com.dnd.model.combat.DamageType) entity);
            case EFFECT             -> repos.effects().save((com.dnd.model.combat.Effect) entity);
            case LANGUAGE           -> repos.languages().save((com.dnd.model.world.Language) entity);
            case ALCHEMY_INGREDIENT -> repos.alchemyIngredients().save((com.dnd.model.alchemy.AlchemyIngredient) entity);
            case BOOK               -> repos.books().save((com.dnd.model.item.books.Book) entity);
            case DICE               -> repos.dice().save((com.dnd.model.world.Dice) entity);
        }
    }

    private Object createNewEntity(EntityCategory cat) throws Exception {
        if (cat == null) throw new IllegalArgumentException("No category");
        if (cat == EntityCategory.ITEM) {
            return com.dnd.model.item.books.Book.class.getDeclaredConstructor().newInstance();
        }
        Class<?> cls = switch (cat) {
            case PLAYER             -> com.dnd.model.character.PlayerCharacter.class;
            case NPC                -> com.dnd.model.creature.Npc.class;
            case MONSTER            -> com.dnd.model.creature.Monster.class;
            case BEAST              -> com.dnd.model.creature.Beast.class;
            case SPELL              -> com.dnd.model.magic.Spell.class;
            case PLACE              -> com.dnd.model.world.Place.class;
            case MAP                -> com.dnd.model.world.map.GameMap.class;
            case CLASS              -> CharacterClass.class;
            case RACE               -> CharacterRace.class;
            case DAMAGE_TYPE        -> com.dnd.model.combat.DamageType.class;
            case EFFECT             -> com.dnd.model.combat.Effect.class;
            case LANGUAGE           -> com.dnd.model.world.Language.class;
            case ALCHEMY_INGREDIENT -> com.dnd.model.alchemy.AlchemyIngredient.class;
            case BOOK               -> com.dnd.model.item.books.Book.class;
            case DICE               -> com.dnd.model.world.Dice.class;
            default                 -> throw new IllegalArgumentException("Cannot create: " + cat);
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
                        || m.getParameterTypes()[0] == long.class || m.getParameterTypes()[0] == double.class
                        || m.getParameterTypes()[0] == boolean.class)) {
                    setters.add(m);
                }
            }
            return setters;
        } catch (Exception e) { return List.of(); }
    }

    private Class<?> getEntityClass(EntityCategory cat) {
        if (cat == null) return null;
        return switch (cat) {
            case PLAYER             -> com.dnd.model.character.PlayerCharacter.class;
            case NPC                -> com.dnd.model.creature.Npc.class;
            case MONSTER            -> com.dnd.model.creature.Monster.class;
            case BEAST              -> com.dnd.model.creature.Beast.class;
            case ITEM               -> com.dnd.model.item.Item.class;
            case SPELL              -> com.dnd.model.magic.Spell.class;
            case PLACE              -> com.dnd.model.world.Place.class;
            case MAP                -> com.dnd.model.world.map.GameMap.class;
            case CLASS              -> CharacterClass.class;
            case RACE               -> CharacterRace.class;
            case DAMAGE_TYPE        -> com.dnd.model.combat.DamageType.class;
            case EFFECT             -> com.dnd.model.combat.Effect.class;
            case LANGUAGE           -> com.dnd.model.world.Language.class;
            case ALCHEMY_INGREDIENT -> com.dnd.model.alchemy.AlchemyIngredient.class;
            case BOOK               -> com.dnd.model.item.books.Book.class;
            case DICE               -> com.dnd.model.world.Dice.class;
        };
    }

    private String setterToFieldName(Method setter) {
        String n = setter.getName().substring(3);
        return Character.toLowerCase(n.charAt(0)) + n.substring(1);
    }

    private String getFieldValue(Object entity, String fieldName) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method m = entity.getClass().getMethod("get" + capitalized);
            Object v = m.invoke(entity);
            return v != null ? v.toString() : "";
        } catch (Exception ignored) {}
        try {
            // boolean getters conventionally use "is" rather than "get" (e.g. isDamaging()).
            Method m = entity.getClass().getMethod("is" + capitalized);
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
                if (!value.isBlank()) m.invoke(entity, Double.parseDouble(value.trim())); return;
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = entity.getClass().getMethod(setter, boolean.class);
                m.invoke(entity, Boolean.parseBoolean(value));
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

    /**
     * Builds a searchable {@link ComboBox} whose items are drawn from {@code options} but whose
     * selection resolves to the option's id (via {@code idFn}) when saved. The display text uses
     * {@code labelFn} (falling back to the id if the name is blank) so the user picks a
     * human-readable name instead of typing a raw id.
     */
    private <T> ComboBox<T> idComboBox(List<T> options, Function<T, String> idFn, Function<T, String> labelFn, String currentId) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().addAll(options);
        combo.setMaxWidth(500);
        combo.setEditable(false);
        combo.setConverter(new javafx.util.StringConverter<T>() {
            @Override
            public String toString(T option) {
                if (option == null) return "";
                String label = labelFn.apply(option);
                String id = idFn.apply(option);
                return (label != null && !label.isBlank()) ? label + "  [" + id + "]" : id;
            }

            @Override
            public T fromString(String string) {
                return combo.getItems().stream()
                    .filter(o -> toString(o).equals(string))
                    .findFirst().orElse(null);
            }
        });
        if (currentId != null && !currentId.isBlank()) {
            options.stream()
                .filter(o -> currentId.equals(idFn.apply(o)))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
        }
        return combo;
    }

    /** Extracts the value to persist from a {@link TextField}, id-backed {@link ComboBox}, or {@link CheckBox}. */
    private String controlValue(Control control) {
        if (control instanceof TextField tf) {
            return tf.getText();
        }
        if (control instanceof CheckBox cb) {
            return Boolean.toString(cb.isSelected());
        }
        if (control instanceof ComboBox<?> combo) {
            Object selected = combo.getSelectionModel().getSelectedItem();
            if (selected == null) return "";
            if (selected instanceof CharacterClass cc) return cc.getId();
            if (selected instanceof CharacterRace cr) return cr.getId();
            return selected.toString();
        }
        return "";
    }
}
