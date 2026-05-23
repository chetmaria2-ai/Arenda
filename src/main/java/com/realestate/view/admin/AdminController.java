package com.realestate.view.admin;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.service.AdminService;
import com.realestate.service.AuthService;
import com.realestate.service.ListingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class AdminController {

    @FXML
    private TableView<JsonObject> usersTable;
    @FXML
    private TableColumn<JsonObject, String> uNameCol, uPhoneCol, uRoleCol, uBlockedCol, uDateCol, uActionsCol;

    @FXML
    private TableView<JsonObject> listingsTable;
    @FXML
    private TableColumn<JsonObject, String> lTitleCol, lOwnerCol, lStatusCol, lDateCol, lActionsCol;

    @FXML
    private TableView<JsonObject> logsTable;
    @FXML
    private TableColumn<JsonObject, String> lgAdminCol, lgActionCol, lgTypeCol, lgTargetCol, lgDetailsCol, lgDateCol;

    @FXML
    private TextField userSearchField;

    @FXML
    public void initialize() {
        setupUsersTable();
        setupListingsTable();
        setupLogsTable();
        loadUsers();
        loadListings();
        loadLogs();
    }

    private void setupUsersTable() {
        uNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().get("full_name").getAsString()));
        uPhoneCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().has("phone") && !c.getValue().get("phone").isJsonNull()
                        ? c.getValue().get("phone").getAsString() : "—"));
        uRoleCol.setCellValueFactory(c -> new SimpleStringProperty(
                "admin".equals(c.getValue().get("role").getAsString()) ? "Администратор" : "Пользователь"));
        uBlockedCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("is_blocked").getAsBoolean() ? "🔴 Да" : "🟢 Нет"));
        uDateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("created_at").getAsString().substring(0, 10)));

        uActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button blockBtn = new Button();

            {
                blockBtn.setOnAction(e -> toggleBlock(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                JsonObject u = getTableView().getItems().get(getIndex());
                boolean blocked = u.get("is_blocked").getAsBoolean();
                blockBtn.setText(blocked ? "Разблокировать" : "Заблокировать");
                blockBtn.getStyleClass().setAll(blocked ? "btn-secondary" : "btn-danger");
                setGraphic(blockBtn);
            }
        });
    }

    private void toggleBlock(JsonObject user) {
        String uid = user.get("id").getAsString();
        boolean block = !user.get("is_blocked").getAsBoolean();
        new Thread(() -> {
            try {
                AdminService.blockUser(uid, block);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(this::loadUsers);
        }).start();
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                JsonArray data = AdminService.getAllUsers();
                Platform.runLater(() -> {
                    usersTable.getItems().clear();
                    String q = userSearchField.getText().toLowerCase();
                    for (JsonElement el : data) {
                        JsonObject u = el.getAsJsonObject();
                        if (q.isBlank() || u.get("full_name").getAsString().toLowerCase().contains(q))
                            usersTable.getItems().add(u);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupListingsTable() {
        lTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().get("title").getAsString()));
        lOwnerCol.setCellValueFactory(c -> {
            JsonObject p = c.getValue().has("profiles") ? c.getValue().getAsJsonObject("profiles") : null;
            return new SimpleStringProperty(p != null ? p.get("full_name").getAsString() : "—");
        });
        lStatusCol.setCellValueFactory(c -> new SimpleStringProperty(translateStatus(c.getValue().get("status").getAsString())));
        lDateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("created_at").getAsString().substring(0, 10)));

        lActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Удалить");

            {
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> {
                    JsonObject l = getTableView().getItems().get(getIndex());
                    new Thread(() -> {
                        try {
                            ListingService.adminDeleteListing(l.get("id").getAsString(), "admin");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Platform.runLater(this::reloadListings);
                    }).start();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }

            private void reloadListings() {
                AdminController.this.loadListings();
            }
        });
    }

    private String translateStatus(String s) {
        return switch (s) {
            case "active" -> "✅ Активно";
            case "rented" -> "🔑 Арендовано";
            case "paused" -> "⏸ Приостановлено";
            case "deleted" -> "🗑 Удалено";
            default -> s;
        };
    }

    private void loadListings() {
        new Thread(() -> {
            try {
                JsonArray data = AdminService.getAllListings();
                Platform.runLater(() -> {
                    listingsTable.getItems().clear();
                    for (JsonElement el : data) listingsTable.getItems().add(el.getAsJsonObject());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupLogsTable() {
        lgAdminCol.setCellValueFactory(c -> {
            JsonObject p = c.getValue().has("profiles") ? c.getValue().getAsJsonObject("profiles") : null;
            return new SimpleStringProperty(p != null && p.has("full_name")
                    ? p.get("full_name").getAsString() : "—");
        });
        lgActionCol.setCellValueFactory(c -> new SimpleStringProperty(
                translateAction(c.getValue().get("action").getAsString())));
        lgTypeCol.setCellValueFactory(c -> new SimpleStringProperty(
                translateTargetType(c.getValue().get("target_type").getAsString())));
        lgTargetCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("target_id").getAsString().substring(0, 8) + "..."));
        lgDetailsCol.setCellValueFactory(c -> {
            if (!c.getValue().has("details") || c.getValue().get("details").isJsonNull())
                return new SimpleStringProperty("—");
            JsonElement det = c.getValue().get("details");
            if (det.isJsonObject()) {
                JsonObject obj = det.getAsJsonObject();
                if (obj.has("reason") && !obj.get("reason").isJsonNull())
                    return new SimpleStringProperty(obj.get("reason").getAsString());
            }
            return new SimpleStringProperty(det.toString());
        });
        lgDateCol.setCellValueFactory(c -> {
            String raw = c.getValue().get("created_at").getAsString();
            return new SimpleStringProperty(raw.length() >= 16 ? raw.substring(0, 16).replace("T", " ") : raw);
        });

        logsTable.setRowFactory(tv -> {
            TableRow<JsonObject> row = new TableRow<>();
            row.itemProperty().addListener((obs, ov, nv) -> {
                if (nv == null) {
                    row.setStyle("");
                    return;
                }
                String action = nv.get("action").getAsString();
                if (action.contains("block") || action.contains("delete"))
                    row.setStyle("-fx-background-color: #fff0f0;");
                else if (action.contains("unblock"))
                    row.setStyle("-fx-background-color: #f0fff0;");
                else
                    row.setStyle("");
            });
            return row;
        });
    }

    private String translateAction(String a) {
        return switch (a) {
            case "block_user" -> "🔴 Блокировка";
            case "unblock_user" -> "🟢 Разблокировка";
            case "delete_listing" -> "🗑 Удаление объявления";
            default -> a;
        };
    }

    private String translateTargetType(String t) {
        return switch (t) {
            case "user" -> "Пользователь";
            case "listing" -> "Объявление";
            case "booking" -> "Бронирование";
            default -> t;
        };
    }

    private void loadLogs() {
        new Thread(() -> {
            try {
                JsonArray data = AdminService.getAdminLogs();
                Platform.runLater(() -> {
                    logsTable.getItems().clear();
                    for (JsonElement el : data)
                        logsTable.getItems().add(el.getAsJsonObject());
                });
            } catch (Exception e) {
                Platform.runLater(() -> logsTable.setPlaceholder(
                        new Label("Ошибка загрузки логов: " + e.getMessage())));
            }
        }).start();
    }

    @FXML
    private void onRefreshUsers() {
        loadUsers();
    }

    @FXML
    private void onRefreshListings() {
        loadListings();
    }

    @FXML
    private void onRefreshLogs() {
        loadLogs();
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
