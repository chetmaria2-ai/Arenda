package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.service.*;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

public class DetailController {

    private static String listingId;

    public static void setListingId(String id) {
        listingId = id;
    }

    @FXML
    private Label titleLabel, priceLabel, addressLabel, descLabel, ownerName, ownerPhone;
    @FXML
    private HBox imagesBox;
    @FXML
    private VBox ownerBox, content;
    @FXML
    private GridPane paramsGrid;
    @FXML
    private Button favBtn, bookBtn, editBtn, deleteBtn;

    private JsonObject listing;

    @FXML
    public void initialize() {
        boolean loggedIn = SessionManager.isLoggedIn();
        boolean isAdmin = SessionManager.isAdmin();

        favBtn.setVisible(loggedIn && !isAdmin);
        bookBtn.setVisible(loggedIn && !isAdmin);
        ownerBox.setVisible(loggedIn);
        ownerBox.setManaged(loggedIn);
        editBtn.setVisible(false);
        editBtn.setManaged(false);
        deleteBtn.setVisible(isAdmin);
        deleteBtn.setManaged(isAdmin);

        loadDetail();
    }

    private void loadDetail() {
        new Thread(() -> {
            try {
                boolean includeContacts = SessionManager.isLoggedIn();
                listing = ListingService.getListing(listingId, includeContacts);
                Platform.runLater(() -> populate(listing));
            } catch (Exception e) {
                Platform.runLater(() -> titleLabel.setText("Ошибка загрузки"));
            }
        }).start();
    }

    private void populate(JsonObject l) {
        titleLabel.setText(l.get("title").getAsString());
        String priceType = l.get("price_type").getAsString();
        priceLabel.setText(String.format("%.0f ₽ / %s",
                l.get("price").getAsDouble(),
                switch (priceType) {
                    case "day" -> "сутки";
                    case "year" -> "год";
                    default -> "мес.";
                }));
        addressLabel.setText(l.get("address").getAsString());
        if (l.has("description") && !l.get("description").isJsonNull())
            descLabel.setText(l.get("description").getAsString());

        boolean hasOwner = SessionManager.isLoggedIn() && l.has("profiles")
                && !l.get("profiles").isJsonNull();
        ownerBox.setVisible(hasOwner);
        ownerBox.setManaged(hasOwner);
        if (hasOwner) {
            JsonObject o = l.getAsJsonObject("profiles");
            String oName = o.has("full_name") && !o.get("full_name").isJsonNull()
                    ? o.get("full_name").getAsString() : "—";
            String oPhone = o.has("phone") && !o.get("phone").isJsonNull()
                    ? o.get("phone").getAsString() : "Не указан";
            ownerName.setText(oName);
            ownerPhone.setText("📞 " + oPhone);
        }

        if (SessionManager.isLoggedIn() &&
                l.get("owner_id").getAsString().equals(SessionManager.getUserId())) {
            editBtn.setVisible(true);
            editBtn.setManaged(true);
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
        }

        imagesBox.getChildren().clear();
        if (l.has("listing_images")) {
            for (JsonElement el : l.getAsJsonArray("listing_images")) {
                String url = el.getAsJsonObject().get("url").getAsString();
                ImageView iv = new ImageView();
                iv.setFitWidth(280);
                iv.setFitHeight(190);
                iv.setPreserveRatio(true);
                iv.getStyleClass().add("detail-img");

                iv.setStyle("-fx-background-color: #f0f0f0;");
                imagesBox.getChildren().add(iv);
                new Thread(() -> {
                    try {
                        Image img = new Image(url, 280, 190, true, true);
                        Platform.runLater(() -> {
                            if (!img.isError()) iv.setImage(img);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        }

        int row = 0;
        if (!l.get("rooms").isJsonNull()) addParam("Комнаты", l.get("rooms").getAsString(), row++);
        if (!l.get("area_sqm").isJsonNull()) addParam("Площадь", l.get("area_sqm").getAsString() + " м²", row++);
        if (!l.get("floor").isJsonNull())
            addParam("Этаж", l.get("floor").getAsString() + "/" + l.get("total_floors").getAsString(), row++);
        addParam("Кондиционер", l.get("has_ac").getAsBoolean() ? "Есть" : "Нет", row++);
        addParam("Парковка", l.get("has_parking").getAsBoolean() ? "Есть" : "Нет", row++);
        addParam("Питомцы", l.get("pets_allowed").getAsBoolean() ? "Можно" : "Нельзя", row++);
        addParam("Мебель", l.get("furnished").getAsBoolean() ? "Меблирована" : "Нет", row++);
    }

    private void addParam(String key, String val, int row) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("param-key");
        Label v = new Label(val);
        v.getStyleClass().add("param-val");
        paramsGrid.add(k, 0, row);
        paramsGrid.add(v, 1, row);
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/ListingsView.fxml");
    }

    @FXML
    private void onFavorite() {
        new Thread(() -> {
            try {
                FavoriteService.addFavorite(listingId);
            } catch (Exception e) {
            }
        }).start();
        favBtn.setText("♥ В избранном");
    }

    @FXML
    private void onBook() {
        BookingController.setListingId(listingId);
        Main.navigateTo("fxml/BookingView.fxml");
    }

    @FXML
    private void onEdit() {
        CreateListingController.setEditMode(listingId, listing);
        Main.navigateTo("fxml/CreateListingView.fxml");
    }

    @FXML
    private void onDelete() {
        new Thread(() -> {
            try {
                if (SessionManager.isAdmin()) ListingService.adminDeleteListing(listingId, "admin delete");
                else ListingService.deleteListing(listingId);
                Platform.runLater(() -> Main.navigateTo("fxml/ListingsView.fxml"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
