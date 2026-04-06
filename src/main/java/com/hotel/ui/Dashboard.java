package com.hotel.ui;

import com.hotel.model.*;
import com.hotel.service.*;
import com.hotel.threads.CleaningThread;
import com.hotel.threads.RoomServiceThread;
import com.hotel.util.FileHandler;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.util.Map;

public class Dashboard {
    private BorderPane mainLayout;
    private RoomService roomService;
    private BookingService bookingService;
    private AuthService authService;
    private ReportService reportService;
    private User loggedInUser;
    private Runnable onLogout;

    private ObservableList<Room> masterRoomData = FXCollections.observableArrayList();
    private FilteredList<Room> filteredRoomsFrontDesk;
    private FilteredList<Room> filteredRoomsAdmin;
    private ObservableList<Customer> masterCustomerData = FXCollections.observableArrayList();
    private FilteredList<Customer> filteredCustomers;
    private ObservableList<User> masterUserData = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private ObservableList<CheckoutRecord> masterCheckoutData = FXCollections.observableArrayList();
    private FilteredList<CheckoutRecord> filteredCheckouts;

    private TableView<Room> roomTable;
    private TableView<Room> adminRoomTable;
    private TableView<Customer> customerTable;
    private TableView<User> userTable;
    private TableView<CheckoutRecord> checkoutTable;
    private Label notificationLabel;
    private Label statTotalRooms, statOccupied, statAvailable, statRevenue;

    private TextArea logsArea;
    private TextArea roomServiceLogsArea;
    private TextArea cleaningLogsArea;
    private TextArea reportTextArea;

    public Dashboard(RoomService roomService, BookingService bookingService, AuthService authService,
                     User loggedInUser, Runnable onLogout) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.authService = authService;
        this.reportService = new ReportService(roomService, bookingService);
        this.loggedInUser = loggedInUser;
        this.onLogout = onLogout;

        filteredRoomsFrontDesk = new FilteredList<>(masterRoomData, p -> true);
        filteredRoomsAdmin = new FilteredList<>(masterRoomData, p -> true);
        filteredCustomers = new FilteredList<>(masterCustomerData, p -> true);
        filteredUsers = new FilteredList<>(masterUserData, p -> true);
        filteredCheckouts = new FilteredList<>(masterCheckoutData, p -> true);

        initializeUI();
    }

    public BorderPane getView() { return mainLayout; }

    private void initializeUI() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("dashboard-pane");
        mainLayout.setTop(buildHeader());

        notificationLabel = new Label("");
        notificationLabel.getStyleClass().add("notification-label");
        notificationLabel.setVisible(false);
        notificationLabel.setMaxWidth(Double.MAX_VALUE);
        notificationLabel.setWrapText(true);

        setupRoomTable();
        setupCustomerTable();

        if (loggedInUser.getRole() == Role.ADMIN) {
            mainLayout.setCenter(buildAdminCenter());
        } else {
            VBox centerWrap = new VBox(16, notificationLabel, createFrontDeskView());
            centerWrap.setPadding(new Insets(24));
            mainLayout.setCenter(centerWrap);
        }
        refreshTables();
    }

    private HBox buildHeader() {
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitWidth(38);
            logoView.setFitHeight(38);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
        } catch (Exception ignored) {}

        Label hotelName = new Label("THE LUXE HOTEL");
        hotelName.getStyleClass().add("header-label");
        Label welcomeLabel = new Label("Welcome back, " + loggedInUser.getUsername()
                + "  \u00b7  " + loggedInUser.getRole().name());
        welcomeLabel.getStyleClass().add("header-sub-label");
        VBox titleStack = new VBox(2, hotelName, welcomeLabel);
        titleStack.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow;
        if (logoView != null) {
            titleRow = new HBox(12, logoView, titleStack);
            titleRow.setAlignment(Pos.CENTER_LEFT);
        } else {
            titleRow = new HBox(titleStack);
            titleRow.setAlignment(Pos.CENTER_LEFT);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnLogout = new Button("Sign Out");
        btnLogout.getStyleClass().add("btn-secondary");
        btnLogout.setOnAction(e -> { if (onLogout != null) onLogout.run(); });

        HBox header = new HBox(16, titleRow, spacer, btnLogout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 28, 18, 28));
        header.getStyleClass().add("header-box");
        return header;
    }

    private VBox buildAdminCenter() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("card");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab frontDeskTab = new Tab("  Front Desk  ", createFrontDeskView());
        Tab roomsTab     = new Tab("  Rooms  ",      createRoomsManagementView());
        Tab staffTab     = new Tab("  Staff  ",      createStaffManagementView());
        Tab reportsTab   = new Tab("  Reports  ",    createReportsView());
        Tab historyTab   = new Tab("  Checkout History  ", createCheckoutHistoryView());
        Tab logsTab      = new Tab("  Audit Logs  ", createLogsView());
        Tab serviceTab   = new Tab("  Room Service  ", createRoomServiceView());

        tabPane.getTabs().addAll(frontDeskTab, roomsTab, staffTab, reportsTab, historyTab, logsTab, serviceTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now == logsTab)    refreshLogs();
            if (now == serviceTab) refreshRoomServiceLogs();
            if (now == reportsTab) refreshReportView();
            if (now == historyTab) refreshTables();
        });

        VBox wrap = new VBox(14, notificationLabel, tabPane);
        wrap.setPadding(new Insets(24));
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        return wrap;
    }

    // =========================================================================
    // STATS CARDS
    // =========================================================================

    private HBox buildStatsCards() {
        statTotalRooms = new Label("0");
        statOccupied = new Label("0");
        statAvailable = new Label("0");
        statRevenue = new Label("₹0.00");

        VBox c1 = buildStatCard("TOTAL ROOMS", statTotalRooms, "#2D2B55");
        VBox c2 = buildStatCard("OCCUPIED", statOccupied, "#C0392B");
        VBox c3 = buildStatCard("AVAILABLE", statAvailable, "#27AE60");
        VBox c4 = buildStatCard("DAILY REVENUE", statRevenue, "#C9A84C");

        HBox cards = new HBox(16, c1, c2, c3, c4);
        cards.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(c1, Priority.ALWAYS); HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS); HBox.setHgrow(c4, Priority.ALWAYS);
        return cards;
    }

    private VBox buildStatCard(String title, Label valueLabel, String color) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");
        valueLabel.getStyleClass().add("stat-card-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        VBox card = new VBox(8, titleLabel, valueLabel);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        return card;
    }

    private void refreshStats() {
        if (statTotalRooms == null) return;
        statTotalRooms.setText(String.valueOf(reportService.getTotalRooms()));
        statOccupied.setText(String.valueOf(reportService.getOccupiedRooms()));
        statAvailable.setText(String.valueOf(reportService.getAvailableRooms()));
        statRevenue.setText("₹" + String.format("%.2f", reportService.getEstimatedDailyRevenue()));
    }

    // =========================================================================
    // FRONT DESK
    // =========================================================================

    private VBox createFrontDeskView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(12));

        HBox statsCards = buildStatsCards();

        Button btnBookRoom    = makeButton("Book Room",           "btn-primary");
        Button btnCheckout    = makeButton("Checkout Guest",      "btn-secondary");
        Button btnSort        = makeButton("Sort by Price",       "btn-outline");
        Button btnRoomService = makeButton("Call Room Service",   "btn-outline");
        btnBookRoom.setOnAction(e -> showBookRoomDialog());
        btnCheckout.setOnAction(e -> processCheckout());
        btnSort.setOnAction(e -> { roomService.sortRoomsByPrice(); refreshTables(); showNotification("\u2713  Rooms sorted by nightly price."); });
        btnRoomService.setOnAction(e -> callRoomService());
        HBox controls = new HBox(12, btnBookRoom, btnCheckout, btnSort, btnRoomService);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("control-panel");

        Label hintLabel = new Label("\ud83d\udca1  Tip: Double-click any guest to preview their Luxe bill.");
        hintLabel.getStyleClass().add("sub-label");
        hintLabel.setStyle("-fx-text-fill: #9090A0; -fx-font-size: 12px; -fx-padding: 0 0 0 4px;");

        // Room search/filter bar
        TextField searchRoomField = new TextField();
        searchRoomField.setPromptText("Search room #\u2026");
        searchRoomField.getStyleClass().add("text-field-custom");
        searchRoomField.setMaxWidth(160);
        ComboBox<String> fdTypeFilter = new ComboBox<>(FXCollections.observableArrayList("All Types", "Standard", "Deluxe", "Suite"));
        fdTypeFilter.setValue("All Types");
        ComboBox<String> fdAvailFilter = new ComboBox<>(FXCollections.observableArrayList("All Status", "Available", "Occupied"));
        fdAvailFilter.setValue("All Status");
        ComboBox<String> fdAmenityFilter = new ComboBox<>(FXCollections.observableArrayList("All Amenities", "AC", "WiFi", "AC + WiFi"));
        fdAmenityFilter.setValue("All Amenities");

        Runnable applyFdFilter = () -> filteredRoomsFrontDesk.setPredicate(r -> {
            boolean n = searchRoomField.getText().isEmpty() || String.valueOf(r.getRoomNumber()).contains(searchRoomField.getText());
            boolean t = fdTypeFilter.getValue().equals("All Types") || r.getType().name().equalsIgnoreCase(fdTypeFilter.getValue());
            boolean a = fdAvailFilter.getValue().equals("All Status")
                    || (fdAvailFilter.getValue().equals("Available") && r.isAvailable())
                    || (fdAvailFilter.getValue().equals("Occupied") && !r.isAvailable());
            boolean am = fdAmenityFilter.getValue().equals("All Amenities")
                    || (fdAmenityFilter.getValue().equals("AC") && r.isHasAC())
                    || (fdAmenityFilter.getValue().equals("WiFi") && r.isHasWifi())
                    || (fdAmenityFilter.getValue().equals("AC + WiFi") && r.isHasAC() && r.isHasWifi());
            return n && t && a && am;
        });
        searchRoomField.textProperty().addListener(e -> applyFdFilter.run());
        fdTypeFilter.valueProperty().addListener(e -> applyFdFilter.run());
        fdAvailFilter.valueProperty().addListener(e -> applyFdFilter.run());
        fdAmenityFilter.valueProperty().addListener(e -> applyFdFilter.run());

        HBox roomFilterBar = new HBox(10, new Label("Search:"), searchRoomField, fdTypeFilter, fdAvailFilter, fdAmenityFilter);
        roomFilterBar.setAlignment(Pos.CENTER_LEFT);

        // Guest search
        TextField searchGuestField = new TextField();
        searchGuestField.setPromptText("Search guests by name, ID, contact or room\u2026");
        searchGuestField.getStyleClass().add("text-field-custom");
        searchGuestField.setMaxWidth(320);
        searchGuestField.textProperty().addListener((obs, oldV, newV) ->
            filteredCustomers.setPredicate(c -> {
                if (newV == null || newV.isEmpty()) return true;
                String q = newV.toLowerCase();
                return c.getName().toLowerCase().contains(q)
                    || c.getCustomerId().toLowerCase().contains(q)
                    || c.getContact().toLowerCase().contains(q)
                    || String.valueOf(c.getAllocatedRoomNumber()).contains(q);
            })
        );

        Label roomLabel = new Label("ROOM AVAILABILITY");
        roomLabel.getStyleClass().add("bill-section-header");
        VBox roomSection = new VBox(12, roomLabel, roomFilterBar, roomTable);
        roomSection.getStyleClass().add("card");
        roomSection.setPadding(new Insets(18));

        Label guestLabel = new Label("CURRENT GUESTS");
        guestLabel.getStyleClass().add("bill-section-header");
        HBox guestHeader = new HBox(16, guestLabel, searchGuestField);
        guestHeader.setAlignment(Pos.CENTER_LEFT);
        VBox customerSection = new VBox(12, guestHeader, customerTable);
        customerSection.getStyleClass().add("card");
        customerSection.setPadding(new Insets(18));

        HBox tablesRow = new HBox(24, roomSection, customerSection);
        HBox.setHgrow(roomSection, Priority.ALWAYS);
        HBox.setHgrow(customerSection, Priority.ALWAYS);

        view.getChildren().addAll(statsCards, controls, hintLabel, tablesRow);
        return view;
    }

    // =========================================================================
    // ROOM MGMT (ADMIN)
    // =========================================================================

    private VBox createRoomsManagementView() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(16));

        Button btnAddRoom    = makeButton("Add Room",    "btn-primary");
        Button btnRemoveRoom = makeButton("Remove Room", "btn-danger");
        Button btnEditRate   = makeButton("Edit Rate",   "btn-outline");

        btnAddRoom.setOnAction(e -> showAddRoomDialog());
        btnRemoveRoom.setOnAction(e -> {
            Room sel = adminRoomTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showNotification("Select a room first."); return; }
            try {
                roomService.removeRoom(loggedInUser, sel.getRoomNumber());
                showNotification("\u2713  Room " + sel.getRoomNumber() + " removed.");
                refreshTables();
            } catch (Exception ex) { showNotification("Error: " + ex.getMessage()); }
        });
        btnEditRate.setOnAction(e -> showEditRateDialog());

        HBox controls = new HBox(12, btnAddRoom, btnRemoveRoom, btnEditRate);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("control-panel");

        TextField searchField = new TextField();
        searchField.setPromptText("Search room #\u2026");
        searchField.getStyleClass().add("text-field-custom");
        searchField.setMaxWidth(200);
        ComboBox<String> typeFilter = new ComboBox<>(FXCollections.observableArrayList("All", "Standard", "Deluxe", "Suite"));
        typeFilter.setValue("All");

        Runnable applyFilter = () -> filteredRoomsAdmin.setPredicate(r -> {
            boolean numCheck = searchField.getText().isEmpty() || String.valueOf(r.getRoomNumber()).contains(searchField.getText());
            boolean typeCheck = typeFilter.getValue().equals("All") || r.getType().name().equalsIgnoreCase(typeFilter.getValue());
            return numCheck && typeCheck;
        });
        searchField.textProperty().addListener(e -> applyFilter.run());
        typeFilter.valueProperty().addListener(e -> applyFilter.run());

        adminRoomTable = new TableView<>();
        setupTableColumnsForRooms(adminRoomTable);
        SortedList<Room> sorted = new SortedList<>(filteredRoomsAdmin);
        sorted.comparatorProperty().bind(adminRoomTable.comparatorProperty());
        adminRoomTable.setItems(sorted);

        HBox filterBar = new HBox(12, new Label("Search:"), searchField, new Label("Type:"), typeFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("ROOMS INVENTORY");
        heading.getStyleClass().add("bill-section-header");
        VBox card = new VBox(12, heading, filterBar, adminRoomTable);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));

        view.getChildren().addAll(controls, card);
        return view;
    }

    private void showEditRateDialog() {
        Room sel = adminRoomTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showNotification("Select a room to edit its rate."); return; }

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Edit Room Rate");
        dialog.setHeaderText("Update nightly rate for Room " + sel.getRoomNumber() + " [" + sel.getType() + "]");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField priceField = new TextField(String.valueOf(sel.getPrice()));
        priceField.getStyleClass().add("text-field-custom");
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20));
        g.add(new Label("Current Rate:"), 0, 0); g.add(new Label("₹" + String.format("%.2f", sel.getPrice())), 1, 0);
        g.add(new Label("New Rate (₹):"), 0, 1); g.add(priceField, 1, 1);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double nv = Double.parseDouble(priceField.getText().trim());
                    if (nv <= 0) { showNotification("Price must be positive."); return null; }
                    return nv;
                } catch (NumberFormatException ex) { showNotification("Invalid price."); }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(newRate -> {
            try {
                roomService.updateRoomRate(loggedInUser, sel.getRoomNumber(), newRate);
                refreshTables();
                showNotification("\u2713  Room " + sel.getRoomNumber() + " rate updated to ₹" + String.format("%.2f", newRate));
            } catch (Exception ex) { showNotification("Error: " + ex.getMessage()); }
        });
    }

    // =========================================================================
    // STAFF MGMT
    // =========================================================================

    private VBox createStaffManagementView() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(16));

        userTable = new TableView<>();
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<User, Role> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        userTable.getColumns().addAll(userCol, roleCol);
        SortedList<User> sortedU = new SortedList<>(filteredUsers);
        sortedU.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedU);

        TextField searchUserField = new TextField();
        searchUserField.setPromptText("Search staff accounts\u2026");
        searchUserField.getStyleClass().add("text-field-custom");
        searchUserField.setMaxWidth(280);
        searchUserField.textProperty().addListener((obs, oldV, newV) ->
            filteredUsers.setPredicate(u ->
                newV == null || newV.isEmpty() || u.getUsername().toLowerCase().contains(newV.toLowerCase()))
        );

        TextField usernameField = new TextField(); usernameField.setPromptText("New username");
        usernameField.getStyleClass().add("text-field-custom"); usernameField.setPrefWidth(180);
        PasswordField passField = new PasswordField(); passField.setPromptText("Password");
        passField.getStyleClass().add("text-field-custom"); passField.setPrefWidth(180);
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleBox.setValue(Role.RECEPTIONIST);
        Button btnCreate = makeButton("Create Account", "btn-primary");
        btnCreate.setOnAction(e -> {
            String uName = usernameField.getText().trim();
            String pwd   = passField.getText().trim();
            if (uName.isEmpty() || pwd.isEmpty()) { showNotification("All fields are required."); return; }
            if (uName.length() < 3) { showNotification("Username must be \u2265 3 characters."); return; }
            if (!uName.matches("^[a-zA-Z0-9]+₹")) { showNotification("Username must be alphanumeric only."); return; }
            try {
                authService.addUser(new User(uName, pwd, roleBox.getValue()));
                showNotification("\u2713  Account created for " + uName);
                refreshTables(); usernameField.clear(); passField.clear();
            } catch (Exception ex) { showNotification(ex.getMessage()); }
        });

        HBox createForm = new HBox(12, new Label("Username:"), usernameField, new Label("Password:"), passField, new Label("Role:"), roleBox, btnCreate);
        createForm.setAlignment(Pos.CENTER_LEFT);
        createForm.getStyleClass().add("control-panel");

        Label staffHeading = new Label("SYSTEM STAFF");
        staffHeading.getStyleClass().add("bill-section-header");
        HBox staffHeader = new HBox(16, staffHeading, searchUserField);
        staffHeader.setAlignment(Pos.CENTER_LEFT);
        VBox listCard = new VBox(12, staffHeader, userTable);
        listCard.getStyleClass().add("card");
        listCard.setPadding(new Insets(18));

        view.getChildren().addAll(createForm, listCard);
        return view;
    }

    // =========================================================================
    // REPORTS TAB
    // =========================================================================

    private VBox createReportsView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(16));

        Button btnRefresh = makeButton("Refresh Report", "btn-primary");
        Button btnExport  = makeButton("Export to File",  "btn-outline");
        btnRefresh.setOnAction(e -> refreshReportView());
        btnExport.setOnAction(e -> {
            try {
                String filename = "luxe_report_" + System.currentTimeMillis() + ".txt";
                reportService.exportReport(filename);
                showNotification("\u2713  Report exported to " + filename);
            } catch (IOException ex) { showNotification("Export failed: " + ex.getMessage()); }
        });

        HBox controls = new HBox(12, btnRefresh, btnExport);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("control-panel");

        reportTextArea = new TextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setPrefHeight(500);

        Label heading = new Label("SYSTEM REPORTS");
        heading.getStyleClass().add("bill-section-header");
        Label hint = new Label("Comprehensive overview of hotel operations. Click Refresh for latest data.");
        hint.getStyleClass().add("sub-label");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #9090A0;");
        hint.setWrapText(true);

        VBox card = new VBox(10, heading, hint, reportTextArea);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));

        view.getChildren().addAll(controls, card);
        return view;
    }

    private void refreshReportView() {
        if (reportTextArea != null) {
            reportTextArea.setText(reportService.generateFullReport());
            reportTextArea.setScrollTop(0);
        }
    }

    // =========================================================================
    // CHECKOUT HISTORY TAB
    // =========================================================================

    private VBox createCheckoutHistoryView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(16));

        checkoutTable = new TableView<>();
        checkoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CheckoutRecord, String> idCol = new TableColumn<>("Guest ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        TableColumn<CheckoutRecord, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        TableColumn<CheckoutRecord, Integer> roomCol = new TableColumn<>("Room #");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<CheckoutRecord, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        TableColumn<CheckoutRecord, Integer> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(new PropertyValueFactory<>("nightsStayed"));
        TableColumn<CheckoutRecord, String> coTimeCol = new TableColumn<>("Checkout Time");
        coTimeCol.setCellValueFactory(new PropertyValueFactory<>("checkOutTime"));
        TableColumn<CheckoutRecord, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : "₹" + String.format("%.2f", val));
            }
        });
        TableColumn<CheckoutRecord, String> byCol = new TableColumn<>("Processed By");
        byCol.setCellValueFactory(new PropertyValueFactory<>("processedBy"));

        checkoutTable.getColumns().addAll(idCol, nameCol, roomCol, typeCol, nightsCol, coTimeCol, totalCol, byCol);

        SortedList<CheckoutRecord> sortedCO = new SortedList<>(filteredCheckouts);
        sortedCO.comparatorProperty().bind(checkoutTable.comparatorProperty());
        checkoutTable.setItems(sortedCO);

        TextField searchCO = new TextField();
        searchCO.setPromptText("Search checkout history\u2026");
        searchCO.getStyleClass().add("text-field-custom");
        searchCO.setMaxWidth(300);
        searchCO.textProperty().addListener((obs, o, n) ->
            filteredCheckouts.setPredicate(r -> {
                if (n == null || n.isEmpty()) return true;
                String q = n.toLowerCase();
                return r.getCustomerName().toLowerCase().contains(q)
                    || r.getCustomerId().toLowerCase().contains(q)
                    || String.valueOf(r.getRoomNumber()).contains(q);
            })
        );

        Label heading = new Label("CHECKOUT HISTORY");
        heading.getStyleClass().add("bill-section-header");
        HBox header = new HBox(16, heading, searchCO);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, header, checkoutTable);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        view.getChildren().add(card);
        return view;
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    private VBox createLogsView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(16));
        logsArea = new TextArea();
        logsArea.setEditable(false);
        logsArea.setPrefHeight(500);
        Label heading = new Label("SYSTEM AUDIT LOGS");
        heading.getStyleClass().add("bill-section-header");
        Label hint = new Label("All booking, checkout, and admin actions are recorded here. Checkout entries include full amount charged.");
        hint.getStyleClass().add("sub-label");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #9090A0;");
        hint.setWrapText(true);
        VBox card = new VBox(10, heading, hint, logsArea);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        view.getChildren().add(card);
        return view;
    }

    private void refreshLogs() {
        if (logsArea != null) { logsArea.setText(FileHandler.readLogs()); logsArea.setScrollTop(Double.MAX_VALUE); }
    }

    // =========================================================================
    // ROOM SERVICE TAB
    // =========================================================================

    private VBox createRoomServiceView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(16));
        roomServiceLogsArea = new TextArea(); roomServiceLogsArea.setEditable(false); roomServiceLogsArea.setPrefHeight(320);
        cleaningLogsArea = new TextArea(); cleaningLogsArea.setEditable(false); cleaningLogsArea.setPrefHeight(320);

        Label rsHeading = new Label("ROOM SERVICE REQUESTS");
        rsHeading.getStyleClass().add("bill-section-header");
        VBox rsCard = new VBox(10, rsHeading, roomServiceLogsArea);
        rsCard.getStyleClass().add("card"); rsCard.setPadding(new Insets(18));

        Label cleanHeading = new Label("CLEANING & MAINTENANCE HISTORY");
        cleanHeading.getStyleClass().add("bill-section-header");
        VBox cleanCard = new VBox(10, cleanHeading, cleaningLogsArea);
        cleanCard.getStyleClass().add("card"); cleanCard.setPadding(new Insets(18));

        HBox split = new HBox(20, rsCard, cleanCard);
        HBox.setHgrow(rsCard, Priority.ALWAYS); HBox.setHgrow(cleanCard, Priority.ALWAYS);
        view.getChildren().add(split);
        return view;
    }

    private void refreshRoomServiceLogs() {
        if (roomServiceLogsArea != null) { roomServiceLogsArea.setText(FileHandler.readRoomServiceLogs()); roomServiceLogsArea.setScrollTop(Double.MAX_VALUE); }
        if (cleaningLogsArea != null) { cleaningLogsArea.setText(FileHandler.readCleaningLogs()); cleaningLogsArea.setScrollTop(Double.MAX_VALUE); }
    }

    // =========================================================================
    // TABLE SETUP
    // =========================================================================

    private void setupRoomTable() {
        roomTable = new TableView<>();
        setupTableColumnsForRooms(roomTable);
        SortedList<Room> sorted = new SortedList<>(filteredRoomsFrontDesk);
        sorted.comparatorProperty().bind(roomTable.comparatorProperty());
        roomTable.setItems(sorted);
    }

    private void setupTableColumnsForRooms(TableView<Room> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Room, Integer> numCol = new TableColumn<>("Room #");
        numCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<Room, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Room, Double> priceCol = new TableColumn<>("Base Rate");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty); setText((empty || val == null) ? null : String.format("₹%.2f", val));
            }
        });

        TableColumn<Room, String> amenCol = new TableColumn<>("Amenities");
        amenCol.setCellValueFactory(new PropertyValueFactory<>("amenitiesDisplay"));

        TableColumn<Room, Double> effCol = new TableColumn<>("Eff. Rate");
        effCol.setCellValueFactory(new PropertyValueFactory<>("effectiveRate"));
        effCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText((empty || val == null) ? null : String.format("₹%.2f", val));
                if (!empty && val != null) setStyle("-fx-font-weight: 700; -fx-text-fill: #2D2B55;");
            }
        });

        TableColumn<Room, Boolean> availCol = new TableColumn<>("Status");
        availCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        availCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val ? "AVAILABLE" : "OCCUPIED");
                setStyle(val ? "-fx-text-fill: #27AE60; -fx-font-weight: 700;" : "-fx-text-fill: #C0392B; -fx-font-weight: 700;");
            }
        });
        table.getColumns().addAll(numCol, typeCol, priceCol, amenCol, effCol, availCol);
    }

    private void setupCustomerTable() {
        customerTable = new TableView<>();
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Customer, String> idCol   = new TableColumn<>("Guest ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        TableColumn<Customer, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Customer, String> contactCol = new TableColumn<>("Contact");
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contact"));
        TableColumn<Customer, Integer> roomCol = new TableColumn<>("Room #");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("allocatedRoomNumber"));
        TableColumn<Customer, String> ciCol = new TableColumn<>("Check-In");
        ciCol.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));
        TableColumn<Customer, String> byCol = new TableColumn<>("Processed By");
        byCol.setCellValueFactory(new PropertyValueFactory<>("checkedInBy"));
        customerTable.getColumns().addAll(idCol, nameCol, contactCol, roomCol, ciCol, byCol);

        customerTable.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && !row.isEmpty()) showBillPreviewDialog(row.getItem()); });
            return row;
        });
        SortedList<Customer> sorted = new SortedList<>(filteredCustomers);
        sorted.comparatorProperty().bind(customerTable.comparatorProperty());
        customerTable.setItems(sorted);
    }

    private void refreshTables() {
        masterRoomData.setAll(roomService.getAllRooms());
        masterCustomerData.setAll(bookingService.getAllCustomers());
        masterUserData.setAll(authService.getAllUsers());
        masterCheckoutData.setAll(bookingService.getCheckoutHistory());
        refreshStats();
    }

    // =========================================================================
    // DIALOGS - ADD ROOM
    // =========================================================================

    private void showAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter Room Details");
        ButtonType saveBtn = new ButtonType("Add Room", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField roomNumField = new TextField(); roomNumField.getStyleClass().add("text-field-custom");
        ComboBox<RoomType> typeBox = new ComboBox<>(FXCollections.observableArrayList(RoomType.values()));
        typeBox.setValue(RoomType.STANDARD);
        TextField priceField = new TextField(String.valueOf(RoomType.STANDARD.getDefaultPrice()));
        priceField.getStyleClass().add("text-field-custom");
        typeBox.setOnAction(e -> priceField.setText(String.valueOf(typeBox.getValue().getDefaultPrice())));

        CheckBox acCheck = new CheckBox("Air Conditioning (+₹15/night)");
        CheckBox wifiCheck = new CheckBox("WiFi (+₹10/night)");

        grid.add(new Label("Room Number:"), 0, 0); grid.add(roomNumField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);        grid.add(typeBox, 1, 1);
        grid.add(new Label("Price / Night (₹):"), 0, 2); grid.add(priceField, 1, 2);
        grid.add(new Label("Amenities:"), 0, 3);
        VBox amenBox = new VBox(8, acCheck, wifiCheck);
        grid.add(amenBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    int num = Integer.parseInt(roomNumField.getText().trim());
                    double price = Double.parseDouble(priceField.getText().trim());
                    if (num <= 0 || num > 9999) { showNotification("Room # must be 1\u20139999."); return null; }
                    if (price <= 0) { showNotification("Price must be positive."); return null; }
                    return new Room(num, typeBox.getValue(), price, acCheck.isSelected(), wifiCheck.isSelected());
                } catch (NumberFormatException e) { showNotification("Invalid room number or price format."); }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(room -> {
            try {
                roomService.addRoom(loggedInUser, room);
                refreshTables();
                showNotification("\u2713  Room " + room.getRoomNumber() + " added successfully.");
            } catch (Exception e) { showNotification("Error: " + e.getMessage()); }
        });
    }

    // =========================================================================
    // DIALOGS - BOOK ROOM
    // =========================================================================

    private void showBookRoomDialog() {
        Room sel = roomTable.getSelectionModel().getSelectedItem();
        if (sel == null || !sel.isAvailable()) { showNotification("Select an available room from the table first."); return; }

        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Book Room " + sel.getRoomNumber());
        dialog.setHeaderText("Register Guest \u2014 Room " + sel.getRoomNumber()
                + " [" + sel.getType() + "]  \u00b7  ₹" + String.format("%.2f", sel.getEffectiveRate()) + "/night"
                + (sel.isHasAC() ? "  [AC]" : "") + (sel.isHasWifi() ? "  [WiFi]" : ""));
        ButtonType bookBtn = new ButtonType("Confirm Booking", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bookBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        TextField idField      = styled(new TextField(), "text-field-custom");
        TextField nameField    = styled(new TextField(), "text-field-custom");
        TextField contactField = styled(new TextField(), "text-field-custom");
        grid.add(new Label("Guest ID (SSN/Passport):"), 0, 0); grid.add(idField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);               grid.add(nameField, 1, 1);
        grid.add(new Label("Contact Number:"), 0, 2);          grid.add(contactField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != bookBtn) return null;
            String reqId = idField.getText().trim(), reqName = nameField.getText().trim(), reqCon = contactField.getText().trim();
            if (reqId.isEmpty() || reqName.isEmpty() || reqCon.isEmpty()) { showNotification("All guest fields are required."); return null; }
            try {
                bookingService.bookRoom(loggedInUser, reqId, reqName, reqCon, sel.getRoomNumber());
                return bookingService.getAllCustomers().stream().filter(c -> c.getCustomerId().equals(reqId)).findFirst().orElse(null);
            } catch (Exception e) { showNotification("Booking Failed: " + e.getMessage()); return null; }
        });
        dialog.showAndWait().ifPresent(result -> {
            if (result != null) { refreshTables(); showNotification("\u2713  Room " + sel.getRoomNumber() + " booked for " + result.getName() + "."); }
        });
    }

    // =========================================================================
    // CHECKOUT
    // =========================================================================

    private void processCheckout() {
        Customer sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showNotification("Select a guest from the table to check out."); return; }
        int roomNumber = sel.getAllocatedRoomNumber();
        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) { showNotification("Error: Room not found."); return; }

        Dialog<Integer> daysDialog = new Dialog<>();
        daysDialog.setTitle("Checkout \u2014 " + sel.getName());
        daysDialog.setHeaderText("Confirm Number of Nights Stayed");
        daysDialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Checkout & Generate Bill", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);

        TextField daysField = styled(new TextField("1"), "text-field-custom");
        daysField.setPrefWidth(80);
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));
        g.add(new Label("Guest:"),      0, 0); g.add(new Label(sel.getName()), 1, 0);
        g.add(new Label("Room:"),       0, 1); g.add(new Label("#" + roomNumber + " [" + room.getType() + "]"), 1, 1);
        g.add(new Label("Check-In:"),   0, 2); g.add(new Label(sel.getCheckInTime() != null ? sel.getCheckInTime() : "N/A"), 1, 2);
        g.add(new Label("Base Rate:"),  0, 3); g.add(new Label("₹" + String.format("%.2f", room.getPrice())), 1, 3);
        if (room.isHasAC()) { g.add(new Label("AC:"), 0, 4); g.add(new Label("+₹15.00/night"), 1, 4); }
        if (room.isHasWifi()) { int r = room.isHasAC() ? 5 : 4; g.add(new Label("WiFi:"), 0, r); g.add(new Label("+₹10.00/night"), 1, r); }
        int nightsRow = 4 + (room.isHasAC() ? 1 : 0) + (room.isHasWifi() ? 1 : 0);
        g.add(new Label("Eff. Rate:"),  0, nightsRow); g.add(new Label("₹" + String.format("%.2f", room.getEffectiveRate()) + "/night"), 1, nightsRow);
        g.add(new Label("Nights:"),     0, nightsRow + 1); g.add(daysField, 1, nightsRow + 1);
        daysDialog.getDialogPane().setContent(g);

        daysDialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try { int d = Integer.parseInt(daysField.getText().trim()); if (d <= 0) { showNotification("Nights must be \u2265 1."); return null; } return d;
                } catch (NumberFormatException ex) { showNotification("Invalid number."); }
            } return null;
        });

        daysDialog.showAndWait().ifPresent(days -> {
            try {
                showCheckoutReceiptDialog(sel, room, days);
                bookingService.checkout(loggedInUser, roomNumber, days);
                showNotification("\u2713  " + sel.getName() + " checked out. Dispatching housekeeping\u2026");
                refreshTables();
                CleaningThread cleaner = new CleaningThread(room, roomService, () -> { refreshTables(); showNotification("\u2713  Room " + roomNumber + " cleaned and ready."); });
                cleaner.start();
            } catch (Exception ex) { showNotification("Checkout Error: " + ex.getMessage()); }
        });
    }

    private void showCheckoutReceiptDialog(Customer customer, Room room, int days) {
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
        String now = dtf.format(java.time.LocalDateTime.now());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Checkout Receipt \u2014 The Luxe Hotel");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox receipt = buildReceiptLayout(customer, room, days, now, loggedInUser.getUsername(), true);
        dialog.getDialogPane().setContent(receipt);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().getStyleClass().add("bill-dialog");
        dialog.showAndWait();
    }

    private void showBillPreviewDialog(Customer customer) {
        Room room = roomService.getRoomByNumber(customer.getAllocatedRoomNumber());
        if (room == null) return;
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
        String now = dtf.format(java.time.LocalDateTime.now());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Guest Bill Preview \u2014 " + customer.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox bill = buildReceiptLayout(customer, room, -1, now, loggedInUser.getUsername(), false);
        dialog.getDialogPane().setContent(bill);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().getStyleClass().add("bill-dialog");
        dialog.showAndWait();
    }

    private VBox buildReceiptLayout(Customer customer, Room room, int days, String timestamp, String previewedBy, boolean isFinal) {
        VBox root = new VBox(0);
        root.getStyleClass().add("bill-preview-pane");
        root.setMinWidth(460);

        // Logo in receipt
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitWidth(50); logoView.setFitHeight(50); logoView.setPreserveRatio(true);
        } catch (Exception ignored) {}

        Label hotelName = new Label("THE LUXE HOTEL");
        hotelName.getStyleClass().add("bill-hotel-name");
        hotelName.setMaxWidth(Double.MAX_VALUE); hotelName.setTextAlignment(TextAlignment.CENTER);
        Label tagline = new Label("LUXURY \u00b7 COMFORT \u00b7 EXCELLENCE");
        tagline.getStyleClass().add("bill-tagline");
        tagline.setMaxWidth(Double.MAX_VALUE); tagline.setTextAlignment(TextAlignment.CENTER);
        Label receiptType = new Label(isFinal ? "CHECKOUT RECEIPT" : "BILL PREVIEW \u2014 DRAFT");
        receiptType.getStyleClass().add("bill-section-header");
        receiptType.setMaxWidth(Double.MAX_VALUE); receiptType.setTextAlignment(TextAlignment.CENTER);

        VBox header;
        if (logoView != null) {
            HBox logoRow = new HBox(logoView); logoRow.setAlignment(Pos.CENTER);
            header = new VBox(6, logoRow, hotelName, tagline, mySep(), receiptType);
        } else {
            header = new VBox(6, hotelName, tagline, mySep(), receiptType);
        }
        header.setAlignment(Pos.CENTER); header.setPadding(new Insets(0, 0, 12, 0));

        GridPane guestGrid = buildBillGrid("Guest Name", customer.getName(), "Guest ID", customer.getCustomerId(), "Contact", customer.getContact());
        String daysStr = (days < 0) ? "Pending checkout" : days + " night(s)";
        GridPane stayGrid = buildBillGrid(
            "Room", "#" + room.getRoomNumber() + "  [" + room.getType() + "]",
            "Amenities", room.getAmenitiesDisplay(),
            "Check-In", customer.getCheckInTime() != null ? customer.getCheckInTime() : "N/A",
            "Check-Out", isFinal ? timestamp : "Still checked in",
            "Nights", daysStr,
            "Processed By", customer.getCheckedInBy() != null ? customer.getCheckedInBy() : "N/A"
        );

        // Billing with amenity surcharges
        double baseRate = room.getPrice();
        double acSur = room.getAcSurcharge();
        double wifiSur = room.getWifiSurcharge();
        double effRate = room.getEffectiveRate();
        int actualDays = days < 0 ? 1 : days;
        double subtotal = effRate * actualDays;
        double tax = subtotal * 0.12;
        double grandTotal = subtotal + tax;

        String baseStr = "₹" + String.format("%.2f", baseRate) + " / night";
        String subStr = days < 0 ? baseStr + "  (per night, full total at checkout)" : "₹" + String.format("%.2f", subtotal);

        GridPane billingGrid;
        if (acSur > 0 || wifiSur > 0) {
            java.util.List<String> pairs = new java.util.ArrayList<>();
            pairs.add("Base Rate"); pairs.add(baseStr);
            if (acSur > 0) { pairs.add("AC Surcharge"); pairs.add("+₹" + String.format("%.2f", acSur) + " / night"); }
            if (wifiSur > 0) { pairs.add("WiFi Surcharge"); pairs.add("+₹" + String.format("%.2f", wifiSur) + " / night"); }
            pairs.add("Effective Rate"); pairs.add("₹" + String.format("%.2f", effRate) + " / night");
            pairs.add("Subtotal"); pairs.add(subStr);
            pairs.add("Tax (12%)"); pairs.add("₹" + String.format("%.2f", tax));
            billingGrid = buildBillGrid(pairs.toArray(new String[0]));
        } else {
            billingGrid = buildBillGrid("Nightly Rate", baseStr, "Subtotal", subStr, "Tax (12%)", "₹" + String.format("%.2f", tax));
        }

        Label totalKey = new Label("TOTAL AMOUNT");
        totalKey.getStyleClass().add("bill-total-label");
        String totalVStr = days < 0 ? "Calculated on checkout" : "₹" + String.format("%.2f", grandTotal);
        Label totalVal = new Label(totalVStr);
        totalVal.getStyleClass().add("bill-total-value");
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Region ts = new Region(); HBox.setHgrow(ts, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalKey, ts, totalVal);
        totalRow.setPadding(new Insets(14, 0, 14, 0));

        Label reviewer = new Label("Preview generated by:  " + previewedBy + "  \u00b7  " + timestamp);
        reviewer.getStyleClass().add("bill-footer");
        reviewer.setMaxWidth(Double.MAX_VALUE); reviewer.setTextAlignment(TextAlignment.CENTER);
        Label thanks = new Label(isFinal ? "Thank you for staying at The Luxe Hotel. We hope to see you again soon."
                : "This is a live preview. The final amount will be calculated at checkout.");
        thanks.getStyleClass().add("bill-footer");
        thanks.setMaxWidth(Double.MAX_VALUE); thanks.setWrapText(true); thanks.setTextAlignment(TextAlignment.CENTER);

        root.getChildren().addAll(header, sectionCard("GUEST INFORMATION", guestGrid), sectionCard("STAY DETAILS", stayGrid),
            sectionCard("BILLING BREAKDOWN", billingGrid), mySep(), totalRow, mySep(), new VBox(6, reviewer, thanks));
        root.setAlignment(Pos.TOP_CENTER);
        return root;
    }

    private GridPane buildBillGrid(String... pairs) {
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(8); grid.setPadding(new Insets(8, 0, 8, 0));
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(145);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Label key = new Label(pairs[i]); key.getStyleClass().add("bill-field-key");
            Label val = new Label(pairs[i + 1]); val.getStyleClass().add("bill-field-value"); val.setWrapText(true);
            GridPane.setHalignment(key, HPos.LEFT); GridPane.setHalignment(val, HPos.LEFT);
            grid.add(key, 0, i / 2); grid.add(val, 1, i / 2);
        }
        return grid;
    }

    private VBox sectionCard(String title, javafx.scene.Node content) {
        Label heading = new Label(title); heading.getStyleClass().add("bill-section-header");
        VBox box = new VBox(6, heading, content); box.setPadding(new Insets(10, 0, 6, 0)); return box;
    }

    private Separator mySep() {
        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #C9A84C;"); sep.setPadding(new Insets(4, 0, 4, 0)); return sep;
    }

    // =========================================================================
    // ROOM SERVICE / NOTIFICATIONS / UTIL
    // =========================================================================

    private void callRoomService() {
        Customer sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showNotification("Select a guest to call room service."); return; }
        Thread t = new Thread(new RoomServiceThread(sel.getAllocatedRoomNumber(), this::showNotification));
        t.setDaemon(true); t.start();
    }

    private void showNotification(String message) {
        Platform.runLater(() -> {
            notificationLabel.setText(message); notificationLabel.setVisible(true);
            new Thread(() -> { try { Thread.sleep(4500); Platform.runLater(() -> notificationLabel.setVisible(false)); } catch (InterruptedException ignored) {} }).start();
        });
    }

    private Button makeButton(String text, String styleClass) { Button b = new Button(text); b.getStyleClass().add(styleClass); return b; }
    private <T extends TextField> T styled(T field, String styleClass) { field.getStyleClass().add(styleClass); return field; }
}
