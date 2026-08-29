package com.dnd.ui.scenes;

import com.dnd.auth.AuthService;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginScene extends BaseScene {
    private final AuthService authService = new AuthService();

    public LoginScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = centeredVBox(14);
        root.getStyleClass().add("root");

        TextField identifierField = textField("Username or email");
        PasswordField passwordField = passwordField("Password");
        Label error = body("");
        error.getStyleClass().add("error-label");

        root.getChildren().addAll(
            title("⚔  DnD Campaign Manager  ⚔"),
            subtitle("Log in to continue"),
            identifierField,
            passwordField,
            error,
            btn("Log In", () -> {
                AuthService.LoginResult result = authService.login(identifierField.getText(), passwordField.getText());
                if (result.success()) {
                    uiSession.setCurrentUser(result.user());
                    uiSession.getRouter().goTo(SceneType.LANDING);
                } else {
                    error.setText(result.message());
                }
            }),
            btn("Create an account", () -> uiSession.getRouter().goTo(SceneType.REGISTER)),
            btn("Forgot password?", () -> uiSession.getRouter().goTo(SceneType.FORGOT_PASSWORD))
        );

        return wrapInScene(root);
    }
}
