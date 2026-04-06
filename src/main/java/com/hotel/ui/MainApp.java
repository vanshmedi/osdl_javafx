package com.hotel.ui;

import com.hotel.model.User;
import com.hotel.service.AuthService;
import com.hotel.service.BookingService;
import com.hotel.service.RoomService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private RoomService roomService;
    private BookingService bookingService;
    private AuthService authService;
    private Scene mainScene;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Services
        roomService = new RoomService();
        bookingService = new BookingService(roomService);
        authService = new AuthService();

        // Setup Login Screen
        showLoginScreen(primaryStage);

        // Load CSS
        loadCss();

        primaryStage.setTitle("Luxe Hotel Management System");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
    
    private void showLoginScreen(Stage primaryStage) {
        LoginScreen loginScreen = new LoginScreen(authService, user -> {
            showDashboard(user, primaryStage);
        });
        
        if (mainScene == null) {
            mainScene = new Scene(loginScreen.getView(), 1000, 700);
        } else {
            mainScene.setRoot(loginScreen.getView());
        }
    }

    private void showDashboard(User loggedInUser, Stage primaryStage) {
        Runnable onLogout = () -> showLoginScreen(primaryStage);
        Dashboard dashboard = new Dashboard(roomService, bookingService, authService, loggedInUser, onLogout);
        mainScene.setRoot(dashboard.getView());
    }
    
    private void loadCss() {
        String css = getClass().getResource("/styles.css") != null ? 
                     getClass().getResource("/styles.css").toExternalForm() : "";
        if (!css.isEmpty()) {
            mainScene.getStylesheets().add(css);
        } else {
            System.err.println("Warning: styles.css not found. Default styles will be used.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
