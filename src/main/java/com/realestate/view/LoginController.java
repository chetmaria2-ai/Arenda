package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.config.SupabaseConfig;
import com.realestate.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import okhttp3.*;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginBtn;

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String pass = passField.getText();
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Входим...");

        new Thread(() -> {
            try {
                AuthService.AuthResult res = AuthService.login(email, pass);
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Войти");
                    if (!res.ok()) {
                        showError(res.error());
                        return;
                    }
                    if (res.isBlocked()) {
                        showError("Аккаунт заблокирован");
                        return;
                    }
                    loadProfileAndNavigate(res, email);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Войти");
                    showError("Ошибка сети: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadProfileAndNavigate(AuthService.AuthResult res, String email) {
        new Thread(() -> {
            try {
                Request req = SupabaseConfig.req(
                        "/rest/v1/profiles?id=eq." + res.userId()
                                + "&select=full_name,phone").get().build();
                try (Response r = SupabaseConfig.http().newCall(req).execute()) {
                    String body = r.body().string();
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    String fullName = email;
                    String phone = null;
                    if (arr.size() > 0) {
                        JsonObject p = arr.get(0).getAsJsonObject();
                        fullName = p.has("full_name") && !p.get("full_name").isJsonNull()
                                ? p.get("full_name").getAsString() : email;
                        phone = p.has("phone") && !p.get("phone").isJsonNull()
                                ? p.get("phone").getAsString() : null;
                    }
                    final String fn = fullName, ph = phone;
                    Platform.runLater(() -> {
                        SessionManager.setUser(res.userId(), fn, ph, res.role(), res.isBlocked());
                        if ("admin".equals(res.role())) Main.navigateTo("fxml/admin/AdminDashboard.fxml");
                        else Main.navigateTo("fxml/ListingsView.fxml");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SessionManager.setUser(res.userId(), email, null, res.role(), res.isBlocked());
                    if ("admin".equals(res.role())) Main.navigateTo("fxml/admin/AdminDashboard.fxml");
                    else Main.navigateTo("fxml/ListingsView.fxml");
                });
            }
        }).start();
    }

    @FXML
    private void onGuest() {
        SessionManager.clear();
        Main.navigateTo("fxml/ListingsView.fxml");
    }

    @FXML
    private void onRegister() {
        Main.navigateTo("fxml/RegisterView.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
