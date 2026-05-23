package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.config.SupabaseConfig;
import com.realestate.service.AuthService;
import com.realestate.service.BookingService;
import com.realestate.service.CardService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.*;
import javafx.scene.control.*;
import okhttp3.*;

public class ProfileController {

    @FXML
    private TextField nameField, phoneField, emailField;
    @FXML
    private Label msgLabel;
    @FXML
    private PasswordField newPasswordField, confirmPasswordField;
    @FXML
    private Label pwdMsgLabel;
    @FXML
    private TextField cardNumField, cardHolderField, cardExpiryField;
    @FXML
    private CheckBox cardDefaultBox;
    @FXML
    private Label cardMsgLabel;
    @FXML
    private ListView<String> cardListView;

    @FXML
    private TableView<JsonObject> bookingsTable;
    @FXML
    private TableColumn<JsonObject, String> bTitleCol, bDatesCol, bStatusCol, bCancelCol;

    private final java.util.List<String> cardIds = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        nameField.setText(SessionManager.getFullName() != null ? SessionManager.getFullName() : "");
        phoneField.setText(SessionManager.getPhone() != null ? SessionManager.getPhone() : "");
        loadEmail();
        loadCards();
        loadBookings();
        setupTable();
    }

    private void loadEmail() {
        new Thread(() -> {
            try {
                Request req = SupabaseConfig.req("/auth/v1/user").get().build();
                try (Response r = SupabaseConfig.http().newCall(req).execute()) {
                    JsonObject user = JsonParser.parseString(r.body().string()).getAsJsonObject();
                    String email = user.has("email") ? user.get("email").getAsString() : "";
                    Platform.runLater(() -> emailField.setText(email));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String body = String.format("{\"full_name\":\"%s\",\"phone\":\"%s\"}", name, phone);
        new Thread(() -> {
            try {
                Request req = SupabaseConfig.req(
                                "/rest/v1/profiles?id=eq." + SessionManager.getUserId())
                        .patch(RequestBody.create(body, SupabaseConfig.JSON)).build();
                try (Response r = SupabaseConfig.http().newCall(req).execute()) {
                    if (r.body() != null) r.body().close();
                }
                Platform.runLater(() -> showMsg(msgLabel, "✓ Профиль сохранён", false));
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(msgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    @FXML
    private void onChangeEmail() {
        String newEmail = emailField.getText().trim();
        if (newEmail.isBlank() || !newEmail.contains("@")) {
            showMsg(msgLabel, "Введите корректный email", true);
            return;
        }
        new Thread(() -> {
            try {
                AuthService.updateEmail(newEmail);
                Platform.runLater(() -> showMsg(msgLabel, "✓ Email обновлён (проверьте почту)", false));
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(msgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    @FXML
    private void onChangePassword() {
        String pwd = newPasswordField.getText();
        String conf = confirmPasswordField.getText();
        if (pwd.isBlank()) {
            showMsg(pwdMsgLabel, "Введите новый пароль", true);
            return;
        }
        if (pwd.length() < 6) {
            showMsg(pwdMsgLabel, "Минимум 6 символов", true);
            return;
        }
        if (!pwd.equals(conf)) {
            showMsg(pwdMsgLabel, "Пароли не совпадают", true);
            return;
        }
        new Thread(() -> {
            try {
                AuthService.updatePassword(pwd);
                Platform.runLater(() -> {
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    showMsg(pwdMsgLabel, "✓ Пароль изменён", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(pwdMsgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    private void loadCards() {
        new Thread(() -> {
            try {
                JsonArray cards = CardService.getSavedCards();
                Platform.runLater(() -> {
                    cardIds.clear();
                    cardListView.getItems().clear();
                    for (JsonElement el : cards) {
                        JsonObject c = el.getAsJsonObject();
                        cardIds.add(c.get("id").getAsString());
                        String def = c.get("is_default").getAsBoolean() ? " ★" : "";
                        String label = "**** **** **** " + c.get("card_last4").getAsString()
                                + "  " + c.get("card_holder").getAsString()
                                + "  " + c.get("card_expiry").getAsString() + def;
                        cardListView.getItems().add(label);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(cardMsgLabel, "Ошибка загрузки карт: " + e.getMessage(), true));
            }
        }).start();
    }

    @FXML
    private void onAddCard() {
        String num = cardNumField.getText().trim();
        String holder = cardHolderField.getText().trim().toUpperCase();
        String expiry = cardExpiryField.getText().trim();
        boolean def = cardDefaultBox.isSelected();

        if (num.isBlank() || num.length() < 4) {
            showMsg(cardMsgLabel, "Введите номер карты", true);
            return;
        }
        if (holder.isBlank()) {
            showMsg(cardMsgLabel, "Введите держателя карты", true);
            return;
        }
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            showMsg(cardMsgLabel, "Формат даты: ММ/ГГ", true);
            return;
        }

        String digits = num.replaceAll("\\D", "");
        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;

        new Thread(() -> {
            try {
                CardService.saveCard(last4, holder, expiry, def);
                Platform.runLater(() -> {
                    cardNumField.clear();
                    cardHolderField.clear();
                    cardExpiryField.clear();
                    cardDefaultBox.setSelected(false);
                    showMsg(cardMsgLabel, "✓ Карта добавлена", false);
                    loadCards();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(cardMsgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    @FXML
    private void onDeleteCard() {
        int idx = cardListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showMsg(cardMsgLabel, "Выберите карту для удаления", true);
            return;
        }
        String id = cardIds.get(idx);
        new Thread(() -> {
            try {
                CardService.deleteCard(id);
                Platform.runLater(() -> {
                    showMsg(cardMsgLabel, "✓ Карта удалена", false);
                    loadCards();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(cardMsgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    @FXML
    private void onSetDefault() {
        int idx = cardListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            showMsg(cardMsgLabel, "Выберите карту", true);
            return;
        }
        String id = cardIds.get(idx);
        new Thread(() -> {
            try {
                CardService.setDefault(id);
                Platform.runLater(() -> {
                    showMsg(cardMsgLabel, "✓ Карта по умолчанию установлена", false);
                    loadCards();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(cardMsgLabel, "Ошибка: " + e.getMessage(), true));
            }
        }).start();
    }

    private void setupTable() {
        bTitleCol.setCellValueFactory(c -> {
            JsonObject l = c.getValue().has("listings")
                    ? c.getValue().getAsJsonObject("listings") : null;
            return new SimpleStringProperty(l != null ? l.get("title").getAsString() : "—");
        });
        bDatesCol.setCellValueFactory(c -> {
            String s = c.getValue().get("start_date").getAsString();
            String e = !c.getValue().get("end_date").isJsonNull()
                    ? " — " + c.getValue().get("end_date").getAsString() : "";
            return new SimpleStringProperty(s + e);
        });
        bStatusCol.setCellValueFactory(c ->
                new SimpleStringProperty(translateStatus(c.getValue().get("status").getAsString())));
        bCancelCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Отменить");

            {
                btn.getStyleClass().add("btn-danger");
                btn.setOnAction(e -> {
                    JsonObject b = getTableView().getItems().get(getIndex());
                    if ("pending".equals(b.get("status").getAsString()))
                        new Thread(() -> {
                            try {
                                BookingService.cancelBooking(b.get("id").getAsString());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            Platform.runLater(ProfileController.this::loadBookings);
                        }).start();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                JsonObject b = getTableView().getItems().get(getIndex());
                setGraphic("pending".equals(b.get("status").getAsString()) ? btn : null);
            }
        });
    }

    private String translateStatus(String s) {
        return switch (s) {
            case "pending" -> "⏳ Ожидает";
            case "confirmed" -> "✅ Подтверждено";
            case "cancelled" -> "❌ Отменено";
            case "completed" -> "✔ Завершено";
            default -> s;
        };
    }

    private void loadBookings() {
        new Thread(() -> {
            try {
                JsonArray data = BookingService.getMyBookings();
                Platform.runLater(() -> {
                    bookingsTable.getItems().clear();
                    for (JsonElement el : data)
                        bookingsTable.getItems().add(el.getAsJsonObject());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showMsg(Label lbl, String text, boolean isError) {
        lbl.setText(text);
        lbl.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #2e7d32;");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/ListingsView.fxml");
    }

    @FXML
    private void onLogout() {
        new Thread(() -> {
            try {
                AuthService.logout();
            } catch (Exception e) {
                e.printStackTrace();
            }
            SessionManager.clear();
            Platform.runLater(() -> Main.navigateTo("fxml/LoginView.fxml"));
        }).start();
    }
}
