package com.realestate.view;

import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML
    private TextField nameField, phoneField, emailField;
    @FXML
    private PasswordField passField, pass2Field;
    @FXML
    private Label errorLabel;
    @FXML
    private Button regBtn;

    @FXML
    private void onRegister() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passField.getText();
        String pass2 = pass2Field.getText();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Заполните обязательные поля");
            return;
        }
        if (!pass.equals(pass2)) {
            showError("Пароли не совпадают");
            return;
        }
        if (pass.length() < 6) {
            showError("Пароль минимум 6 символов");
            return;
        }

        regBtn.setDisable(true);
        new Thread(() -> {
            try {
                AuthService.AuthResult res = AuthService.register(email, pass, name, phone);
                Platform.runLater(() -> {
                    regBtn.setDisable(false);
                    if (res.ok()) {
                        SessionManager.setUser(res.userId(), name, phone, "user", false);
                        Main.navigateTo("fxml/ListingsView.fxml");
                    } else {
                        showError(res.error());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    regBtn.setDisable(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/LoginView.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
