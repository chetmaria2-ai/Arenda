package com.realestate;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("RealEstate — Аренда недвижимости");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        navigateTo("fxml/LoginView.fxml");

        stage.centerOnScreen();
        stage.show();
    }

    public static void navigateTo(String fxml) {
        try {

            boolean wasShowing = primaryStage.isShowing();
            double x = primaryStage.getX();
            double y = primaryStage.getY();
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();
            boolean maximized = primaryStage.isMaximized();

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("/com/realestate/" + fxml));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                                    Main.class.getResource("/com/realestate/css/main.css"))
                            .toExternalForm());

            primaryStage.setScene(scene);

            if (wasShowing && !Double.isNaN(width) && width > 0) {
                primaryStage.setX(x);
                primaryStage.setY(y);
                primaryStage.setWidth(width);
                primaryStage.setHeight(height);
                if (maximized) primaryStage.setMaximized(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
