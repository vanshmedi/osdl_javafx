package com.hotel.ui;

import com.hotel.model.User;
import com.hotel.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginScreen {
    private BorderPane view;
    private AuthService authService;
    private Consumer<User> onLoginSuccess;

    public LoginScreen(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
        buildUI();
    }

    public BorderPane getView() {
        return view;
    }

    private void buildUI() {
        view = new BorderPane();
        view.getStyleClass().add("dashboard-pane");

        VBox loginBox = new VBox(18);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(50, 40, 50, 40));
        loginBox.getStyleClass().add("login-card");
        loginBox.setMaxWidth(420);

        // Logo
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitWidth(90);
            logoView.setFitHeight(90);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
        } catch (Exception e) {
            System.err.println("Logo not found, skipping.");
        }

        Label hotelTitle = new Label("THE LUXE HOTEL");
        hotelTitle.getStyleClass().add("header-label");

        Label subtitle = new Label("Staff Portal \u2014 Please sign in");
        subtitle.getStyleClass().add("header-sub-label");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field-custom");
        usernameField.setPrefHeight(44);
        usernameField.setMaxWidth(Double.MAX_VALUE);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-field-custom");
        passwordField.setPrefHeight(44);
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label("");
        errorLabel.getStyleClass().add("notification-error");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(46);

        Runnable doLogin = () -> {
            String u = usernameField.getText();
            String p = passwordField.getText();
            User user = authService.login(u, p);
            if (user != null) {
                onLoginSuccess.accept(user);
            } else {
                errorLabel.setText("Invalid username or password. Please try again.");
                errorLabel.setVisible(true);
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passwordField.setOnAction(e -> doLogin.run());

        if (logoView != null) {
            loginBox.getChildren().addAll(logoView, hotelTitle, subtitle, usernameField, passwordField, loginBtn, errorLabel);
        } else {
            loginBox.getChildren().addAll(hotelTitle, subtitle, usernameField, passwordField, loginBtn, errorLabel);
        }

        view.setCenter(loginBox);
    }
}
