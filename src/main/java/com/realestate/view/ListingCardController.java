package com.realestate.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.realestate.Main;
import com.realestate.config.SessionManager;
import com.realestate.service.FavoriteService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ListingCardController {

    @FXML
    private VBox cardRoot;
    @FXML
    private ImageView cardImage;
    @FXML
    private Label titleLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label metaLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private HBox tagsBox;
    @FXML
    private Button favBtn;

    private String listingId;
    private boolean isFav = false;


    public void setData(JsonObject l) {
        listingId = l.get("id").getAsString();

        JsonArray imgs = l.has("listing_images") ? l.getAsJsonArray("listing_images") : new JsonArray();
        if (imgs.size() > 0) {
            String url = imgs.get(0).getAsJsonObject().get("url").getAsString();
            new Thread(() -> {
                try {
                    Image img = new Image(url, 290, 190, false, true);
                    Platform.runLater(() -> {
                        if (img.isError()) {

                            cardImage.setStyle("-fx-background-color: #f0f0f0;");
                        } else {
                            cardImage.setImage(img);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }

        titleLabel.setText(l.get("title").getAsString());

        String priceType = l.has("price_type") ? l.get("price_type").getAsString() : "month";
        priceLabel.setText(String.format("%.0f ₽ / %s",
                l.get("price").getAsDouble(),
                switch (priceType) {
                    case "day" -> "сутки";
                    case "year" -> "год";
                    default -> "мес.";
                }));

        String meta = "";
        if (l.has("rooms") && !l.get("rooms").isJsonNull())
            meta += l.get("rooms").getAsInt() + " комн.  ";
        if (l.has("area_sqm") && !l.get("area_sqm").isJsonNull())
            meta += l.get("area_sqm").getAsDouble() + " м²";
        metaLabel.setText(meta.trim());

        addressLabel.setText(l.get("address").getAsString());

        tagsBox.getChildren().clear();
        if (l.has("has_ac") && l.get("has_ac").getAsBoolean())
            tagsBox.getChildren().add(makeTag("❄ Кондёр"));
        if (l.has("pets_allowed") && l.get("pets_allowed").getAsBoolean())
            tagsBox.getChildren().add(makeTag("🐾 Питомцы"));

        if (favBtn != null) {
            boolean show = SessionManager.isLoggedIn() && !SessionManager.isAdmin();
            favBtn.setVisible(show);
            favBtn.setManaged(show);
            if (show) {

                new Thread(() -> {
                    try {
                        boolean fav = FavoriteService.isFavorite(listingId);
                        Platform.runLater(() -> {
                            isFav = fav;
                            updateFavBtn();
                        });
                    } catch (Exception setDataException) {
                        setDataException.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private void updateFavBtn() {
        favBtn.setText(isFav ? "♥ В избранном" : "♡ В избранное");
        favBtn.setStyle(isFav ? "-fx-text-fill: #e53e3e;" : "");
    }

    @FXML
    private void onCardClick() {
        DetailController.setListingId(listingId);
        Main.navigateTo("fxml/DetailView.fxml");
    }

    @FXML
    private void onFavorite() {
        favBtn.setDisable(true);
        boolean wasF = isFav;
        new Thread(() -> {
            try {
                if (wasF) {
                    FavoriteService.removeFavorite(listingId);
                } else {
                    FavoriteService.addFavorite(listingId);
                }
                Platform.runLater(() -> {
                    isFav = !wasF;
                    updateFavBtn();
                    favBtn.setDisable(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> favBtn.setDisable(false));
            }
        }).start();
    }

    private Label makeTag(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("tag");
        return l;
    }
}
