package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.service.FavoriteService;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class FavoritesController {

    @FXML
    private FlowPane favPane;

    @FXML
    public void initialize() {
        loadFavorites();
    }

    private void loadFavorites() {
        new Thread(() -> {
            try {
                JsonArray data = FavoriteService.getFavorites();
                Platform.runLater(() -> {
                    favPane.getChildren().clear();
                    if (data.isEmpty()) {
                        Label empty = new Label("Нет избранных объявлений");
                        empty.setStyle("-fx-text-fill: #888; -fx-font-size: 16px; -fx-padding: 40;");
                        favPane.getChildren().add(empty);
                        return;
                    }
                    for (JsonElement el : data) {
                        JsonObject fav = el.getAsJsonObject();
                        if (fav.has("listings") && !fav.get("listings").isJsonNull())
                            favPane.getChildren().add(buildCard(
                                    fav.getAsJsonObject("listings"),
                                    fav.get("listing_id").getAsString()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox buildCard(JsonObject l, String listingId) {
        VBox card = new VBox(8);
        card.getStyleClass().add("listing-card");
        card.setPrefWidth(260);
        card.setPadding(new Insets(0, 0, 12, 0));

        ImageView iv = new ImageView();
        iv.setFitWidth(260);
        iv.setFitHeight(160);
        iv.setPreserveRatio(false);
        if (l.has("listing_images") && l.getAsJsonArray("listing_images").size() > 0) {
            String url = l.getAsJsonArray("listing_images").get(0)
                    .getAsJsonObject().get("url").getAsString();
            new Thread(() -> {
                try {
                    Image img = new Image(url, 260, 160, false, true);
                    Platform.runLater(() -> {
                        if (!img.isError()) iv.setImage(img);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }

        VBox info = new VBox(4);
        info.setPadding(new Insets(0, 12, 0, 12));

        Label title = new Label(l.get("title").getAsString());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label price = new Label(String.format("%.0f ₽", l.get("price").getAsDouble()));
        price.getStyleClass().add("card-price");

        Button remove = new Button("✕ Удалить из избранного");
        remove.getStyleClass().add("btn-danger");
        remove.setOnAction(e -> new Thread(() -> {
            try {
                FavoriteService.removeFavorite(listingId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Platform.runLater(this::loadFavorites);
        }).start());

        info.getChildren().addAll(title, price, remove);
        card.getChildren().addAll(iv, info);
        card.setOnMouseClicked(e -> {
            if (e.getTarget() == remove || e.getTarget() == remove.getGraphic()) return;
            DetailController.setListingId(listingId);
            Main.navigateTo("fxml/DetailView.fxml");
        });
        return card;
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/ListingsView.fxml");
    }
}
