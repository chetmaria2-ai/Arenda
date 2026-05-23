package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.service.ListingService;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class ListingsController {

    @FXML
    private FlowPane listingsPane;
    @FXML
    private Label countLabel;
    @FXML
    private TextField cityField, priceMinField, priceMaxField;
    @FXML
    private ComboBox<String> roomsBox, typeBox;
    @FXML
    private CheckBox petsBox, acBox;
    @FXML
    private Button favBtn, myBtn, addBtn, profileBtn, authBtn;

    @FXML
    public void initialize() {
        roomsBox.getItems().addAll("1", "2", "3", "4", "5+");
        typeBox.getItems().addAll("apartment", "house", "studio", "room", "commercial");
        updateNavVisibility();
        loadListings(null);
    }

    private void updateNavVisibility() {
        boolean loggedIn = SessionManager.isLoggedIn();
        favBtn.setVisible(loggedIn);
        favBtn.setManaged(loggedIn);
        myBtn.setVisible(loggedIn);
        myBtn.setManaged(loggedIn);
        addBtn.setVisible(loggedIn);
        addBtn.setManaged(loggedIn);
        profileBtn.setVisible(loggedIn);
        profileBtn.setManaged(loggedIn);
        authBtn.setVisible(!loggedIn);
        authBtn.setManaged(!loggedIn);
    }

    private void loadListings(Map<String, String> filters) {
        listingsPane.getChildren().clear();
        countLabel.setText("Загрузка...");

        new Thread(() -> {
            try {
                JsonArray data;
                if (SessionManager.isLoggedIn()) {

                    data = ListingService.getListings(filters);
                } else {

                    data = ListingService.getListingsGuest(filters);
                }

                Platform.runLater(() -> {
                    listingsPane.getChildren().clear();
                    countLabel.setText("Объявлений: " + data.size());
                    for (JsonElement el : data) {
                        Node card = buildCard(el.getAsJsonObject());
                        if (card != null)
                            listingsPane.getChildren().add(card);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> countLabel.setText("Ошибка загрузки: " + e.getMessage()));
            }
        }).start();
    }

    private Node buildCard(JsonObject listing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("/com/realestate/fxml/ListingCard.fxml"));
            Node card = loader.load();
            ListingCardController ctrl = loader.getController();
            ctrl.setData(listing);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void onSearch() {
        Map<String, String> f = new HashMap<>();
        if (!cityField.getText().isBlank())
            f.put("city", cityField.getText().trim());
        if (!priceMinField.getText().isBlank())
            f.put("price_min", priceMinField.getText().trim());
        if (!priceMaxField.getText().isBlank())
            f.put("price_max", priceMaxField.getText().trim());
        if (roomsBox.getValue() != null)
            f.put("rooms", roomsBox.getValue().replace("+", ""));
        if (typeBox.getValue() != null)
            f.put("property_type", typeBox.getValue());
        if (petsBox.isSelected())
            f.put("pets_allowed", "true");
        if (acBox.isSelected())
            f.put("has_ac", "true");
        loadListings(f.isEmpty() ? null : f);
    }

    @FXML
    private void onReset() {
        cityField.clear();
        priceMinField.clear();
        priceMaxField.clear();
        roomsBox.setValue(null);
        typeBox.setValue(null);
        petsBox.setSelected(false);
        acBox.setSelected(false);
        loadListings(null);
    }

    @FXML
    private void onFavorites() {
        Main.navigateTo("fxml/FavoritesView.fxml");
    }

    @FXML
    private void onMyListings() {
        Main.navigateTo("fxml/MyListingsView.fxml");
    }

    @FXML
    private void onAddListing() {
        Main.navigateTo("fxml/CreateListingView.fxml");
    }

    @FXML
    private void onProfile() {
        Main.navigateTo("fxml/ProfileView.fxml");
    }

    @FXML
    private void onAuth() {
        Main.navigateTo("fxml/LoginView.fxml");
    }
}
