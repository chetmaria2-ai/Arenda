package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.service.BookingService;
import com.realestate.service.CardService;
import com.realestate.service.ListingService;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BookingController {

    private static String listingId;

    public static void setListingId(String id) {
        listingId = id;
    }

    @FXML
    private Label listingTitle, priceLabel, totalLabel, errorLabel;
    @FXML
    private DatePicker startPicker, endPicker;
    @FXML
    private ComboBox<String> savedCardBox;
    @FXML
    private TextField cardNumField, cardHolderField, cardExpiryField;
    @FXML
    private TextArea commentArea;
    @FXML
    private Button bookBtn;

    private double pricePerUnit = 0;
    private String priceType = "month";

    private final List<JsonObject> savedCards = new ArrayList<>();

    @FXML
    public void initialize() {
        loadListing();
        loadSavedCards();
        startPicker.valueProperty().addListener((o, ov, nv) -> recalc());
        endPicker.valueProperty().addListener((o, ov, nv) -> recalc());

        savedCardBox.valueProperty().addListener((o, ov, nv) -> {
            int idx = savedCardBox.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < savedCards.size()) {
                JsonObject c = savedCards.get(idx);
                cardNumField.setText(c.get("card_last4").getAsString());
                cardHolderField.setText(c.get("card_holder").getAsString());
                cardExpiryField.setText(c.get("card_expiry").getAsString());
            }
        });
    }

    private void loadSavedCards() {
        new Thread(() -> {
            try {
                JsonArray cards = CardService.getSavedCards();
                Platform.runLater(() -> {
                    savedCards.clear();
                    savedCardBox.getItems().clear();
                    int defaultIdx = -1;
                    for (int i = 0; i < cards.size(); i++) {
                        JsonObject c = cards.get(i).getAsJsonObject();
                        savedCards.add(c);
                        String def = c.get("is_default").getAsBoolean() ? " ★" : "";
                        savedCardBox.getItems().add("**** **** **** "
                                + c.get("card_last4").getAsString() + "  "
                                + c.get("card_holder").getAsString() + def);
                        if (c.get("is_default").getAsBoolean()) defaultIdx = i;
                    }

                    if (defaultIdx >= 0) savedCardBox.getSelectionModel().select(defaultIdx);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadListing() {
        new Thread(() -> {
            try {
                JsonObject l = ListingService.getListing(listingId, false);
                Platform.runLater(() -> {
                    listingTitle.setText(l.get("title").getAsString());
                    pricePerUnit = l.get("price").getAsDouble();
                    priceType = l.get("price_type").getAsString();
                    priceLabel.setText(String.format("%.0f ₽ / %s", pricePerUnit,
                            switch (priceType) {
                                case "day" -> "сутки";
                                case "year" -> "год";
                                default -> "мес.";
                            }));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void recalc() {
        LocalDate s = startPicker.getValue();
        LocalDate e = endPicker.getValue();
        if (s == null) {
            totalLabel.setText("");
            return;
        }
        if (e == null || !e.isAfter(s)) {
            totalLabel.setText("Итого: " + String.format("%.0f ₽", pricePerUnit));
            return;
        }
        long days = ChronoUnit.DAYS.between(s, e);
        double total = switch (priceType) {
            case "day" -> pricePerUnit * days;
            case "year" -> pricePerUnit * (days / 365.0);
            default -> pricePerUnit * (days / 30.0);
        };
        totalLabel.setText(String.format("Итого: %.0f ₽ (%d дней)", total, days));
    }

    @FXML
    private void onBook() {
        LocalDate start = startPicker.getValue();
        if (start == null) {
            showError("Укажите дату начала");
            return;
        }
        String endStr = endPicker.getValue() != null ? endPicker.getValue().toString() : null;
        String card4 = cardNumField.getText().isBlank() ? null : cardNumField.getText().trim();
        String holder = cardHolderField.getText().isBlank() ? null : cardHolderField.getText().trim();
        String expiry = cardExpiryField.getText().isBlank() ? null : cardExpiryField.getText().trim();
        String comment = commentArea.getText().isBlank() ? null : commentArea.getText().trim();
        bookBtn.setDisable(true);
        new Thread(() -> {
            try {
                BookingService.createBooking(listingId, start.toString(), endStr,
                        0, card4, holder, expiry, comment);
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION,
                            "Бронирование отправлено!\nОжидайте подтверждения арендодателя.", ButtonType.OK);
                    a.setHeaderText("✅ Готово");
                    a.showAndWait();
                    Main.navigateTo("fxml/ListingsView.fxml");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    bookBtn.setDisable(false);
                    showError(ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/DetailView.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
