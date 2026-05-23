package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.service.ImageService;
import com.realestate.service.ListingService;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class CreateListingController {

    private static String editId = null;
    private static JsonObject editData = null;

    public static void setEditMode(String id, JsonObject data) {
        editId = id;
        editData = data;
    }

    public static void clearEdit() {
        editId = null;
        editData = null;
    }

    @FXML
    private TextField titleField, addressField, cityField, priceField,
            roomsField, areaField, floorField, floorsField;
    @FXML
    private ComboBox<String> priceTypeBox, propertyTypeBox;
    @FXML
    private CheckBox acBox, parkingBox, petsBox, furnishedBox;
    @FXML
    private TextArea descArea;
    @FXML
    private HBox imagesBox;
    @FXML
    private Button addPhotoBtn;
    @FXML
    private Label errorLabel, pageTitle;
    @FXML
    private Button saveBtn;

    private final List<File> newImages = new ArrayList<>();
    private final List<StackPane> newPreviews = new ArrayList<>();
    private final List<String> existingImageIds = new ArrayList<>();
    private final List<String> existingImagePaths = new ArrayList<>();
    private String currentListingId = null;

    @FXML
    public void initialize() {
        priceTypeBox.getItems().addAll("day", "month", "year");
        propertyTypeBox.getItems().addAll("apartment", "house", "studio", "room", "commercial");

        if (editId != null) {
            pageTitle.setText("Редактировать объявление");
            saveBtn.setText("Сохранить");
            currentListingId = editId;
            populateEdit();
        }
    }

    private void populateEdit() {
        JsonObject l = editData;
        titleField.setText(l.get("title").getAsString());
        addressField.setText(l.get("address").getAsString());
        cityField.setText(l.get("city").getAsString());
        priceField.setText(String.valueOf(l.get("price").getAsDouble()));
        priceTypeBox.setValue(l.get("price_type").getAsString());
        propertyTypeBox.setValue(l.get("property_type").getAsString());
        if (!l.get("rooms").isJsonNull()) roomsField.setText(l.get("rooms").getAsString());
        if (!l.get("area_sqm").isJsonNull()) areaField.setText(l.get("area_sqm").getAsString());
        if (!l.get("floor").isJsonNull()) floorField.setText(l.get("floor").getAsString());
        if (!l.get("total_floors").isJsonNull()) floorsField.setText(l.get("total_floors").getAsString());
        acBox.setSelected(l.get("has_ac").getAsBoolean());
        parkingBox.setSelected(l.get("has_parking").getAsBoolean());
        petsBox.setSelected(l.get("pets_allowed").getAsBoolean());
        furnishedBox.setSelected(l.get("furnished").getAsBoolean());
        if (!l.get("description").isJsonNull()) descArea.setText(l.get("description").getAsString());

        if (l.has("listing_images")) {
            JsonArray imgs = l.getAsJsonArray("listing_images");
            for (JsonElement el : imgs) {
                JsonObject img = el.getAsJsonObject();
                String imgId = img.get("id").getAsString();
                String imgPath = img.get("storage_path").getAsString();
                String imgUrl = img.get("url").getAsString();
                existingImageIds.add(imgId);
                existingImagePaths.add(imgPath);
                addExistingPreview(imgUrl, imgId, imgPath);
            }
        }
        refreshAddBtn();
    }

    private void addExistingPreview(String url, String imgId, String storagePath) {
        ImageView iv = new ImageView();
        iv.setFitWidth(110);
        iv.setFitHeight(90);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");
        new Thread(() -> {
            try {
                Image img = new Image(url, 110, 90, true, true);
                Platform.runLater(() -> {
                    if (!img.isError()) iv.setImage(img);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        Button delBtn = makeDeleteBtn();
        StackPane sp = new StackPane(iv, delBtn);
        StackPane.setAlignment(delBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(delBtn, new Insets(4, 4, 0, 0));
        sp.setPrefSize(110, 90);

        delBtn.setOnAction(e -> removeExistingImage(sp, imgId, storagePath));
        imagesBox.getChildren().add(sp);
    }

    private void removeExistingImage(StackPane sp, String imgId, String storagePath) {
        int idx = existingImageIds.indexOf(imgId);
        if (idx >= 0) {
            existingImageIds.remove(idx);
            existingImagePaths.remove(idx);
        }
        imagesBox.getChildren().remove(sp);
        new Thread(() -> {
            try {
                ImageService.deleteImage(imgId, storagePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
        refreshAddBtn();
    }

    @FXML
    private void onAddImage() {
        int total = existingImageIds.size() + newImages.size();
        if (total >= 3) {
            showError("Максимум 3 фотографии");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Выберите фото");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File f = fc.showOpenDialog(Main.getStage());
        if (f == null) return;

        newImages.add(f);
        ImageView iv = new ImageView(new Image(f.toURI().toString(), 110, 90, true, true));
        iv.setFitWidth(110);
        iv.setFitHeight(90);
        iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");

        Button delBtn = makeDeleteBtn();
        StackPane sp = new StackPane(iv, delBtn);
        StackPane.setAlignment(delBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(delBtn, new Insets(4, 4, 0, 0));
        sp.setPrefSize(110, 90);

        delBtn.setOnAction(e -> removeNewImage(sp, f));
        newPreviews.add(sp);
        imagesBox.getChildren().add(sp);
        refreshAddBtn();
    }

    private void removeNewImage(StackPane sp, File f) {
        int idx = newPreviews.indexOf(sp);
        if (idx >= 0) {
            newPreviews.remove(idx);
            newImages.remove(idx);
        }
        imagesBox.getChildren().remove(sp);
        refreshAddBtn();
    }

    private void refreshAddBtn() {
        int total = existingImageIds.size() + newImages.size();
        addPhotoBtn.setDisable(total >= 3);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private Button makeDeleteBtn() {
        Button b = new Button("✕");
        b.setStyle("-fx-background-color: rgba(229,62,62,0.9); -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 5;" +
                "-fx-background-radius: 4; -fx-min-width: 22; -fx-min-height: 22;");
        return b;
    }

    @FXML
    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isBlank() || addressField.getText().isBlank()
                || cityField.getText().isBlank() || priceField.getText().isBlank()) {
            showError("Заполните обязательные поля (*)");
            return;
        }
        if (title.length() < 5) {
            showError("Заголовок минимум 5 символов");
            return;
        }
        if (title.length() > 150) {
            showError("Заголовок не более 150 символов");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Некорректная цена");
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", title);
        data.put("address", addressField.getText().trim());
        data.put("city", cityField.getText().trim());
        data.put("price", price);
        data.put("price_type", priceTypeBox.getValue() != null ? priceTypeBox.getValue() : "month");
        data.put("property_type", propertyTypeBox.getValue() != null ? propertyTypeBox.getValue() : "apartment");
        try {
            if (!roomsField.getText().isBlank()) data.put("rooms", Integer.parseInt(roomsField.getText().trim()));
            if (!areaField.getText().isBlank()) data.put("area_sqm", Double.parseDouble(areaField.getText().trim()));
            if (!floorField.getText().isBlank()) data.put("floor", Integer.parseInt(floorField.getText().trim()));
            if (!floorsField.getText().isBlank())
                data.put("total_floors", Integer.parseInt(floorsField.getText().trim()));
        } catch (NumberFormatException ex) {
            showError("Проверьте числовые поля");
            return;
        }
        data.put("has_ac", acBox.isSelected());
        data.put("has_parking", parkingBox.isSelected());
        data.put("pets_allowed", petsBox.isSelected());
        data.put("furnished", furnishedBox.isSelected());
        if (!descArea.getText().isBlank()) data.put("description", descArea.getText().trim());

        saveBtn.setDisable(true);
        final boolean isEdit = editId != null;
        final List<File> toUpload = new ArrayList<>(newImages);

        new Thread(() -> {
            try {
                if (isEdit) {
                    ListingService.updateListing(editId, data);
                    currentListingId = editId;
                } else {
                    JsonObject created = ListingService.createListing(data);
                    if (created == null) throw new Exception("Не удалось создать объявление");
                    currentListingId = created.get("id").getAsString();
                }
                for (File img : toUpload)
                    ImageService.uploadImage(img, currentListingId);
                clearEdit();
                Platform.runLater(() -> Main.navigateTo("fxml/MyListingsView.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onBack() {
        clearEdit();
        Main.navigateTo("fxml/ListingsView.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
