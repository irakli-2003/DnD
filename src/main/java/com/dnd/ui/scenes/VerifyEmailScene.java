package com.dnd.ui.scenes;

import com.dnd.auth.AuthService;
import com.dnd.ui.SceneType;
import com.dnd.ui.UiSession;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class VerifyEmailScene extends BaseScene {
    private final AuthService authService = new AuthService();

    public VerifyEmailScene(UiSession uiSession) { super(uiSession); }

    @Override
    public Scene build() {
        VBox root = centeredVBox(14);
        root.getStyleClass().add("root");

        String email = uiSession.getPendingAuthEmail();
        TextField emailField = textField("Email");
        if (email != null) emailField.setText(email);
        TextField codeField = textField("6-digit verification code");
        Label error = body("");
        error.getStyleClass().add("error-label");

        root.getChildren().addAll(
            title("⚔  DnD Campaign Manager  ⚔"),
            subtitle("Verify your email"),
            body("Enter the verification code sent to your email."),
            emailField,
            codeField,
            error,
            btn("Verify", () -> {
                String e = emailField.getText().trim();
                AuthService.SimpleResult result = authService.verifyEmail(e, codeField.getText().trim());
                if (result.success()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Verified");
                    alert.setHeaderText(null);
                    alert.setContentText(result.message());
                    styleDialog(alert);
                    alert.showAndWait();
                    uiSession.getRouter().goTo(SceneType.LOGIN);
                } else {
                    error.setText(result.message());
                }
            }),
            btn("Resend code", () -> {
                String e = emailField.getText().trim();
                AuthService.CodeResult result = authService.resendVerification(e);
                if (!result.success()) {
                    error.setText(result.message());
                    return;
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(result.emailSent() ? "Code sent" : "Email not configured");
                alert.setHeaderText(result.emailSent() ? null : "SMTP is not set up yet");
                alert.setContentText(result.emailSent()
                    ? "A new code was emailed to you."
                    : "Your new verification code is: " + result.code());
                styleDialog(alert);
                alert.showAndWait();
            }),
            btn("← Back to log in", () -> uiSession.getRouter().goTo(SceneType.LOGIN))
        );

        return wrapInScene(root);
    }
}
