package com.dnd.ui.scenes;

import com.dnd.ui.UiSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public abstract class BaseScene {
    protected final UiSession uiSession;
    private static final String CSS_PATH = "/com/dnd/ui/styles/dnd-theme.css";

    protected BaseScene(UiSession uiSession) {
        this.uiSession = uiSession;
    }

    public abstract Scene build();

    protected Scene wrapInScene(javafx.scene.Parent root) {
        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
        return scene;
    }

    protected Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("title-label");
        return l;
    }

    protected Label subtitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtitle-label");
        return l;
    }

    protected Label body(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("body-label");
        l.setMaxWidth(600);
        return l;
    }

    protected Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    protected Button btn(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("dnd-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    protected Button dangerBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("danger-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    protected TextField textField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("dnd-text-field");
        tf.setMaxWidth(400);
        return tf;
    }

    protected PasswordField passwordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("dnd-text-field");
        pf.setMaxWidth(400);
        return pf;
    }

    protected Region spacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    protected Region hSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    protected VBox centeredVBox(int spacing) {
        VBox box = new VBox(spacing);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }

    protected HBox backBar(com.dnd.ui.SceneType backTo) {
        Button back = new Button("← Back");
        back.getStyleClass().add("dnd-button");
        back.setOnAction(e -> uiSession.getRouter().goTo(backTo));
        HBox bar = new HBox(back);
        bar.setPadding(new Insets(12, 20, 0, 20));
        return bar;
    }
}
