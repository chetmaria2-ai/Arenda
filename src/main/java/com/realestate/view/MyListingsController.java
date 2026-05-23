package com.realestate.view;

import com.google.gson.*;
import com.realestate.Main;
import com.realestate.service.BookingService;
import com.realestate.service.ListingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class MyListingsController {

    @FXML
    private FlowPane activePane;
    @FXML
    private TableView<JsonObject> bookingsTable;
    @FXML
    private TableColumn<JsonObject, String> bTenantCol, bListingCol, bDatesCol, bPriceCol, bStatusCol, bActionsCol;

    @FXML
    public void initialize() {
        setupBookingsTable();
        loadListings();
        loadBookings();
    }

    private void loadListings() {
        new Thread(() -> {
            try {
                JsonArray data = ListingService.getMyListings();
                Platform.runLater(() -> {
                    activePane.getChildren().clear();
                    for (JsonElement el : data) {
                        JsonObject l = el.getAsJsonObject();
                        if (!"deleted".equals(l.get("status").getAsString()))
                            activePane.getChildren().add(buildMyCard(l));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadBookings() {
        new Thread(() -> {
            try {
                JsonArray data = BookingService.getBookingsForOwner();
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

    private void setupBookingsTable() {
        bTenantCol.setCellValueFactory(c -> {
            JsonObject p = c.getValue().has("profiles") ? c.getValue().getAsJsonObject("profiles") : null;
            return new SimpleStringProperty(p != null ? p.get("full_name").getAsString() : "—");
        });
        bListingCol.setCellValueFactory(c -> {
            JsonObject l = c.getValue().has("listings") ? c.getValue().getAsJsonObject("listings") : null;
            return new SimpleStringProperty(l != null ? l.get("title").getAsString() : "—");
        });
        bDatesCol.setCellValueFactory(c -> {
            String start = c.getValue().get("start_date").getAsString();
            String end = c.getValue().has("end_date") && !c.getValue().get("end_date").isJsonNull()
                    ? " — " + c.getValue().get("end_date").getAsString() : "";
            return new SimpleStringProperty(start + end);
        });
        bPriceCol.setCellValueFactory(c -> {
            double p = c.getValue().has("total_price") && !c.getValue().get("total_price").isJsonNull()
                    ? c.getValue().get("total_price").getAsDouble() : 0;
            return new SimpleStringProperty(String.format("%.0f ₽", p));
        });
        bStatusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().get("status").getAsString()));

        bActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button confirmBtn = new Button("Подтвердить");

            {
                confirmBtn.getStyleClass().add("btn-primary");
                confirmBtn.setOnAction(e -> {
                    JsonObject b = getTableView().getItems().get(getIndex());
                    new Thread(() -> {
                        try {
                            BookingService.confirmBooking(b.get("id").getAsString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Platform.runLater(() -> loadBookings());
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
                if ("pending".equals(b.get("status").getAsString())) setGraphic(confirmBtn);
                else setGraphic(null);
            }
        });
    }

    private VBox buildMyCard(JsonObject l) {
        VBox card = new VBox(8);
        card.getStyleClass().add("listing-card");
        card.setPrefWidth(240);
        card.setPadding(new Insets(12));

        Label title = new Label(l.get("title").getAsString());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label price = new Label(String.format("%.0f ₽", l.get("price").getAsDouble()));
        price.getStyleClass().add("card-price");
        Label status = new Label(l.get("status").getAsString());
        status.getStyleClass().add("tag");

        String id = l.get("id").getAsString();
        Button edit = new Button("Редактировать");
        edit.getStyleClass().add("btn-secondary");
        Button delete = new Button("Удалить");
        delete.getStyleClass().add("btn-danger");

        edit.setOnAction(e -> {
            CreateListingController.setEditMode(id, l);
            Main.navigateTo("fxml/CreateListingView.fxml");
        });
        delete.setOnAction(e -> new Thread(() -> {
            try {
                ListingService.deleteListing(id);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Platform.runLater(this::loadListings);
        }).start());

        HBox btns = new HBox(8, edit, delete);
        card.getChildren().addAll(title, price, status, btns);
        return card;
    }

    @FXML
    private void onAdd() {
        Main.navigateTo("fxml/CreateListingView.fxml");
    }

    @FXML
    private void onBack() {
        Main.navigateTo("fxml/ListingsView.fxml");
    }
}
