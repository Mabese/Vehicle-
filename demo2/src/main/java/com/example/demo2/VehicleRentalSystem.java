package com.example.demo2;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class VehicleRentalSystem extends Application {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "53143519";

    private Connection connection;
    private Stage primaryStage;
    private User currentUser;

    private TabPane mainTabPane;
    private Tab vehiclesTab, customersTab, paymentsTab, reportsTab, usersTab, bookingsTab;

    private TableView<Vehicle> vehicleTable;
    private ObservableList<Vehicle> vehicleData = FXCollections.observableArrayList();
    private TextField searchField;

    private TableView<Customer> customerTable;
    private ObservableList<Customer> customerData = FXCollections.observableArrayList();

    private TableView<Booking> bookingTable;
    private ObservableList<Booking> bookingData = FXCollections.observableArrayList();

    private DatePicker bookingStartDate, bookingEndDate;
    private ComboBox<Vehicle> availableVehiclesCombo;
    private ComboBox<Customer> customerCombo;
    private Label priceLabel;

    private TableView<Payment> paymentTable;
    private ObservableList<Payment> paymentData = FXCollections.observableArrayList();

    private TableView<User> userTable;
    private ObservableList<User> userData = FXCollections.observableArrayList();

    // Background image
    private final Image backgroundImage = new Image(getClass().getResourceAsStream("/WhatsApp Image 2025-04-19 at 12.17.11_0c6a3c8b.jpg"));
    private final BackgroundImage bgImage = new BackgroundImage(
            backgroundImage,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(100, 100, true, true, false, true)
    );

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Vehicle Rental System");
        initializeDatabase();
        showAuthScreen();
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to database: " + e.getMessage());
            System.exit(1);
        }
    }

    private void createTables() throws SQLException {
        String[] createTables = {
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(50) UNIQUE NOT NULL," +
                        "password VARCHAR(100) NOT NULL," +
                        "role VARCHAR(20) NOT NULL," +
                        "name VARCHAR(100) NOT NULL)",

                "CREATE TABLE IF NOT EXISTS vehicles (" +
                        "vehicleId VARCHAR(20) PRIMARY KEY," +
                        "brand VARCHAR(50) NOT NULL," +
                        "model VARCHAR(50) NOT NULL," +
                        "category VARCHAR(30) NOT NULL," +
                        "year INT," +
                        "pricePerDay DECIMAL(10,2) NOT NULL," +
                        "available BOOLEAN NOT NULL DEFAULT TRUE)",

                "CREATE TABLE IF NOT EXISTS customers (" +
                        "customerId VARCHAR(20) PRIMARY KEY," +
                        "name VARCHAR(100) NOT NULL," +
                        "phone VARCHAR(20) NOT NULL," +
                        "email VARCHAR(100)," +
                        "licenseNumber VARCHAR(50) NOT NULL," +
                        "address TEXT)",

                "CREATE TABLE IF NOT EXISTS bookings (" +
                        "bookingId VARCHAR(20) PRIMARY KEY," +
                        "customerId VARCHAR(20) NOT NULL," +
                        "vehicleId VARCHAR(20) NOT NULL," +
                        "startDate DATE NOT NULL," +
                        "endDate DATE NOT NULL," +
                        "totalPrice DECIMAL(10,2) NOT NULL," +
                        "status VARCHAR(20) NOT NULL," +
                        "FOREIGN KEY (customerId) REFERENCES customers(customerId)," +
                        "FOREIGN KEY (vehicleId) REFERENCES vehicles(vehicleId))",

                "CREATE TABLE IF NOT EXISTS payments (" +
                        "paymentId VARCHAR(20) PRIMARY KEY," +
                        "bookingId VARCHAR(20) NOT NULL," +
                        "amount DECIMAL(10,2) NOT NULL," +
                        "paymentMethod VARCHAR(20) NOT NULL," +
                        "paymentDate DATETIME NOT NULL," +
                        "FOREIGN KEY (bookingId) REFERENCES bookings(bookingId))"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTables) {
                stmt.execute(sql);
            }

            // Insert an admin user if does not exist
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Admin'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users (username, password, role, name) VALUES " +
                        "('admin', 'admin123', 'Admin', 'System Administrator')");
            }
        }
    }

    private void showAuthScreen() {
        GridPane authPane = new GridPane();
        authPane.setAlignment(Pos.CENTER);
        authPane.setHgap(10);
        authPane.setVgap(10);
        authPane.setPadding(new Insets(25));
        authPane.setBackground(new Background(bgImage));

        Label titleLabel = new Label("Vehicle Rental System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        TabPane authTabPane = new TabPane();
        Tab loginTab = new Tab("Login");
        Tab registerTab = new Tab("Register");

        GridPane loginPane = createLoginPane();
        GridPane registerPane = createRegisterPane();

        loginTab.setContent(loginPane);
        registerTab.setContent(registerPane);
        authTabPane.getTabs().addAll(loginTab, registerTab);

        // Style the tab pane
        authTabPane.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 10;");

        authPane.add(titleLabel, 0, 0, 2, 1);
        authPane.add(authTabPane, 0, 1, 2, 1);
        Scene authScene = new Scene(authPane, 500, 400);
        primaryStage.setScene(authScene);
        primaryStage.show();
    }

    private GridPane createLoginPane() {
        GridPane loginPane = new GridPane();
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(20));

        TextField loginUsername = new TextField();
        loginUsername.setPromptText("Username");
        loginUsername.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        PasswordField loginPassword = new PasswordField();
        loginPassword.setPromptText("Password");
        loginPassword.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        Button loginButton = createStyledButton("Login", "#2ecc71");

        loginPane.add(new Label("Username:"), 0, 0);
        loginPane.add(loginUsername, 1, 0);
        loginPane.add(new Label("Password:"), 0, 1);
        loginPane.add(loginPassword, 1, 1);
        loginPane.add(loginButton, 1, 2);

        loginButton.setOnAction(e -> handleLogin(loginUsername, loginPassword));

        return loginPane;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 5;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: derive(" + color + ", -20%);" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 5;"));
        return button;
    }

    private void handleLogin(TextField loginUsername, PasswordField loginPassword) {
        String username = loginUsername.getText();
        String password = loginPassword.getText();

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                currentUser = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("name")
                );
                showMainDashboard();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password");
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    private GridPane createRegisterPane() {
        GridPane registerPane = new GridPane();
        registerPane.setAlignment(Pos.CENTER);
        registerPane.setHgap(10);
        registerPane.setVgap(10);
        registerPane.setPadding(new Insets(20));

        TextField regName = new TextField();
        regName.setPromptText("Full Name");
        regName.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField regUsername = new TextField();
        regUsername.setPromptText("Username");
        regUsername.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Password");
        regPassword.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> regRole = new ComboBox<>();
        regRole.getItems().addAll("Admin", "Employee");
        regRole.setValue("Employee");
        regRole.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        Button registerButton = createStyledButton("Register", "#3498db");

        registerPane.add(new Label("Full Name:"), 0, 0);
        registerPane.add(regName, 1, 0);
        registerPane.add(new Label("Username:"), 0, 1);
        registerPane.add(regUsername, 1, 1);
        registerPane.add(new Label("Password:"), 0, 2);
        registerPane.add(regPassword, 1, 2);
        registerPane.add(new Label("Role:"), 0, 3);
        registerPane.add(regRole, 1, 3);
        registerPane.add(registerButton, 1, 4);

        registerButton.setOnAction(e -> handleRegistration(regName, regUsername, regPassword, regRole));

        return registerPane;
    }

    private void handleRegistration(TextField regName, TextField regUsername, PasswordField regPassword, ComboBox<String> regRole) {
        String name = regName.getText();
        String username = regUsername.getText();
        String password = regPassword.getText();
        String role = regRole.getValue();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration Error", "All fields are required");
            return;
        }

        try {
            PreparedStatement checkStmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.WARNING, "Registration Error", "Username already exists");
                return;
            }

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.setString(4, name);

            stmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! Please login.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    private void showMainDashboard() {
        mainTabPane = new TabPane();
        mainTabPane.setStyle("-fx-background-color: #f5f5f5;");

        paymentsTab = new Tab("Payments");

        if (currentUser.getRole().equals("Admin")) {
            vehiclesTab = new Tab("Vehicles");
            customersTab = new Tab("Customers");
            reportsTab = new Tab("Reports");
            usersTab = new Tab("Users");

            setupVehiclesTab();
            setupCustomersTab();
            setupReportsTab();
            setupUsersTab();

            mainTabPane.getTabs().addAll(vehiclesTab, customersTab, paymentsTab, reportsTab, usersTab);
        } else {
            bookingsTab = new Tab("Bookings");
            setupBookingsTab();
            setupPaymentsTab();
            mainTabPane.getTabs().addAll(bookingsTab, paymentsTab);
        }

        HBox topMenu = new HBox(10);
        Label welcomeLabel = new Label("Welcome, " + currentUser.getName() + " (" + currentUser.getRole() + ")");
        welcomeLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        Button logoutButton = createStyledButton("Logout", "#e74c3c");

        logoutButton.setOnAction(e -> {
            currentUser = null;
            showAuthScreen();
        });

        topMenu.getChildren().addAll(welcomeLabel, logoutButton);
        topMenu.setAlignment(Pos.CENTER_RIGHT);
        topMenu.setPadding(new Insets(10));
        topMenu.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10;");

        VBox mainLayout = new VBox(topMenu, mainTabPane);
        mainLayout.setBackground(new Background(bgImage));
        Scene mainScene = new Scene(mainLayout, 1200, 800);
        primaryStage.setScene(mainScene);

        loadVehicleData();
        loadCustomerData();
        loadPaymentData();
        loadBookingData();
        loadUserData();
    }

    private void setupUsersTab() {
        VBox usersPane = new VBox(10);
        usersPane.setPadding(new Insets(10));
        usersPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        userTable = new TableView<>();
        userTable.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5;");

        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        userTable.getColumns().addAll(idCol, usernameCol, nameCol, roleCol);
        userTable.setItems(userData);

        HBox userControls = new HBox(10);
        Button addUserBtn = createStyledButton("Add User", "#2ecc71");
        Button editUserBtn = createStyledButton("Edit User", "#3498db");
        Button deleteUserBtn = createStyledButton("Delete User", "#e74c3c");

        addUserBtn.setOnAction(e -> showUserDialog(null));
        editUserBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showUserDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to edit.");
            }
        });

        deleteUserBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmation("Delete User", "Are you sure you want to delete this user?")) {
                    deleteUser(selected.getId());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete.");
            }
        });

        userControls.getChildren().addAll(addUserBtn, editUserBtn, deleteUserBtn);
        usersPane.getChildren().addAll(userTable, userControls);
        usersTab.setContent(usersPane);
    }

    private void showUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add New User" : "Edit User");
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Employee");
        roleCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        if (user != null) {
            usernameField.setText(user.getUsername());
            nameField.setText(user.getName());
            roleCombo.setValue(user.getRole());
        } else {
            roleCombo.setValue("Employee");
        }

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Name:"), 0, 2);
        grid.add(nameField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new User(
                        user != null ? user.getId() : 0,
                        usernameField.getText(),
                        passwordField.getText(),
                        roleCombo.getValue(),
                        nameField.getText()
                );
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            if (user == null) {
                addUser(updatedUser);
            } else {
                updateUser(updatedUser);
            }
        });
    }

    private void loadUserData() {
        userData.clear();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            while (rs.next()) {
                userData.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void addUser(User user) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)");

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getName());

            stmt.executeUpdate();
            loadUserData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void updateUser(User user) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET username=?, password=?, role=?, name=? WHERE id=?");

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getName());
            stmt.setInt(5, user.getId());

            stmt.executeUpdate();
            loadUserData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void deleteUser(int userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM users WHERE id=?");
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            loadUserData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void setupVehiclesTab() {
        VBox vehiclesPane = new VBox(10);
        vehiclesPane.setPadding(new Insets(10));
        vehiclesPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        searchField = new TextField();
        searchField.setPromptText("Search Vehicles...");
        searchField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchVehicles(newValue));

        vehicleTable = new TableView<>();
        vehicleTable.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5;");

        TableColumn<Vehicle, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Vehicle, Number> priceCol = new TableColumn<>("Price/Day");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));

        TableColumn<Vehicle, Boolean> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("available"));

        vehicleTable.getColumns().addAll(idCol, brandCol, modelCol, categoryCol, priceCol, availableCol);
        vehicleTable.setItems(vehicleData);

        HBox vehicleControls = new HBox(10);
        Button addVehicleBtn = createStyledButton("Add Vehicle", "#2ecc71");
        Button editVehicleBtn = createStyledButton("Edit Vehicle", "#3498db");
        Button deleteVehicleBtn = createStyledButton("Delete Vehicle", "#e74c3c");

        addVehicleBtn.setOnAction(e -> showVehicleDialog(null));
        editVehicleBtn.setOnAction(e -> {
            Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showVehicleDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a vehicle to edit.");
            }
        });

        deleteVehicleBtn.setOnAction(e -> {
            Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmation("Delete Vehicle", "Are you sure you want to delete this vehicle?")) {
                    deleteVehicle(selected.getVehicleId());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a vehicle to delete.");
            }
        });

        vehicleControls.getChildren().addAll(addVehicleBtn, editVehicleBtn, deleteVehicleBtn);
        vehiclesPane.getChildren().addAll(searchField, vehicleTable, vehicleControls);
        vehiclesTab.setContent(vehiclesPane);
    }

    private void searchVehicles(String query) {
        ObservableList<Vehicle> filteredData = FXCollections.observableArrayList();

        for (Vehicle vehicle : vehicleData) {
            if (vehicle.getBrand().toLowerCase().contains(query.toLowerCase()) ||
                    vehicle.getModel().toLowerCase().contains(query.toLowerCase())) {
                filteredData.add(vehicle);
            }
        }

        vehicleTable.setItems(filteredData);
    }

    private void showVehicleDialog(Vehicle vehicle) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle(vehicle == null ? "Add New Vehicle" : "Edit Vehicle");
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField idField = new TextField();
        idField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField brandField = new TextField();
        brandField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField modelField = new TextField();
        modelField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Car", "SUV", "Truck", "Van", "Motorcycle");
        categoryCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField yearField = new TextField();
        yearField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField priceField = new TextField();
        priceField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        CheckBox availableCheck = new CheckBox("Available");
        availableCheck.setStyle("-fx-text-fill: #2c3e50;");

        if (vehicle != null) {
            idField.setText(vehicle.getVehicleId());
            brandField.setText(vehicle.getBrand());
            modelField.setText(vehicle.getModel());
            categoryCombo.setValue(vehicle.getCategory());
            yearField.setText(String.valueOf(vehicle.getYear()));
            priceField.setText(String.format("%.2f", vehicle.getPricePerDay()));
            availableCheck.setSelected(vehicle.isAvailable());
            idField.setDisable(true);
        }

        grid.add(new Label("Vehicle ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Year:"), 0, 4);
        grid.add(yearField, 1, 4);
        grid.add(new Label("Price/Day:"), 0, 5);
        grid.add(priceField, 1, 5);
        grid.add(availableCheck, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String vehicleId = idField.getText();
                    String brand = brandField.getText();
                    String model = modelField.getText();
                    String category = categoryCombo.getValue();
                    int year = Integer.parseInt(yearField.getText());
                    double price = Double.parseDouble(priceField.getText());
                    boolean available = availableCheck.isSelected();

                    return new Vehicle(vehicleId, brand, model, category, year, price, available);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for year and price.");
                    return null;
                }
            }
            return null;
        });

        Optional<Vehicle> result = dialog.showAndWait();
        result.ifPresent(updatedVehicle -> {
            if (vehicle == null) {
                addVehicle(updatedVehicle);
            } else {
                updateVehicle(updatedVehicle);
            }
        });
    }

    private void loadVehicleData() {
        vehicleData.clear();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM vehicles");

            while (rs.next()) {
                vehicleData.add(new Vehicle(
                        rs.getString("vehicleId"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("category"),
                        rs.getInt("year"),
                        rs.getDouble("pricePerDay"),
                        rs.getBoolean("available")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void addVehicle(Vehicle vehicle) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO vehicles (vehicleId, brand, model, category, year, pricePerDay, available) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)");

            stmt.setString(1, vehicle.getVehicleId());
            stmt.setString(2, vehicle.getBrand());
            stmt.setString(3, vehicle.getModel());
            stmt.setString(4, vehicle.getCategory());
            stmt.setInt(5, vehicle.getYear());
            stmt.setDouble(6, vehicle.getPricePerDay());
            stmt.setBoolean(7, vehicle.isAvailable());

            stmt.executeUpdate();
            loadVehicleData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void updateVehicle(Vehicle vehicle) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE vehicles SET brand=?, model=?, category=?, year=?, pricePerDay=?, available=? " +
                            "WHERE vehicleId=?");

            stmt.setString(1, vehicle.getBrand());
            stmt.setString(2, vehicle.getModel());
            stmt.setString(3, vehicle.getCategory());
            stmt.setInt(4, vehicle.getYear());
            stmt.setDouble(5, vehicle.getPricePerDay());
            stmt.setBoolean(6, vehicle.isAvailable());
            stmt.setString(7, vehicle.getVehicleId());

            stmt.executeUpdate();
            loadVehicleData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void deleteVehicle(String vehicleId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM vehicles WHERE vehicleId=?");
            stmt.setString(1, vehicleId);
            stmt.executeUpdate();
            loadVehicleData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void setupCustomersTab() {
        VBox customersPane = new VBox(10);
        customersPane.setPadding(new Insets(10));
        customersPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        customerTable = new TableView<>();
        customerTable.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5;");

        TableColumn<Customer, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> licenseCol = new TableColumn<>("License");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));

        customerTable.getColumns().addAll(idCol, nameCol, phoneCol, licenseCol);
        customerTable.setItems(customerData);

        HBox customerControls = new HBox(10);
        Button addCustomerBtn = createStyledButton("Add Customer", "#2ecc71");
        Button editCustomerBtn = createStyledButton("Edit Customer", "#3498db");
        Button deleteCustomerBtn = createStyledButton("Delete Customer", "#e74c3c");

        addCustomerBtn.setOnAction(e -> showCustomerDialog(null));
        editCustomerBtn.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a customer to edit.");
            }
        });

        deleteCustomerBtn.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmation("Delete Customer", "Are you sure you want to delete this customer?")) {
                    deleteCustomer(selected.getCustomerId());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a customer to delete.");
            }
        });

        customerControls.getChildren().addAll(addCustomerBtn, editCustomerBtn, deleteCustomerBtn);
        customersPane.getChildren().addAll(customerTable, customerControls);
        customersTab.setContent(customersPane);
    }

    private void showCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(customer == null ? "Add New Customer" : "Edit Customer");
        dialog.setHeaderText(customer == null ? "Enter customer details" : "Edit customer details");
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField idField = new TextField();
        idField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField phoneField = new TextField();
        phoneField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField emailField = new TextField();
        emailField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField licenseField = new TextField();
        licenseField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextArea addressArea = new TextArea();
        addressArea.setPrefRowCount(3);
        addressArea.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        if (customer != null) {
            idField.setText(customer.getCustomerId());
            nameField.setText(customer.getName());
            phoneField.setText(customer.getPhone());
            emailField.setText(customer.getEmail());
            licenseField.setText(customer.getLicenseNumber());
            addressArea.setText(customer.getAddress());
            idField.setDisable(true);
        }

        grid.add(new Label("Customer ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(new Label("License Number:"), 0, 4);
        grid.add(licenseField, 1, 4);
        grid.add(new Label("Address:"), 0, 5);
        grid.add(addressArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Customer(
                        idField.getText(),
                        nameField.getText(),
                        phoneField.getText(),
                        emailField.getText(),
                        licenseField.getText(),
                        addressArea.getText()
                );
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(updatedCustomer -> {
            if (customer == null) {
                addCustomer(updatedCustomer);
            } else {
                updateCustomer(updatedCustomer);
            }
        });
    }

    private void loadCustomerData() {
        customerData.clear();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM customers");

            while (rs.next()) {
                customerData.add(new Customer(
                        rs.getString("customerId"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("licenseNumber"),
                        rs.getString("address")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void addCustomer(Customer customer) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO customers (customerId, name, phone, email, licenseNumber, address) " +
                            "VALUES (?, ?, ?, ?, ?, ?)");

            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getLicenseNumber());
            stmt.setString(6, customer.getAddress());

            stmt.executeUpdate();
            loadCustomerData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void updateCustomer(Customer customer) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE customers SET name=?, phone=?, email=?, licenseNumber=?, address=? " +
                            "WHERE customerId=?");

            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getLicenseNumber());
            stmt.setString(5, customer.getAddress());
            stmt.setString(6, customer.getCustomerId());

            stmt.executeUpdate();
            loadCustomerData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void deleteCustomer(String customerId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM customers WHERE customerId=?");
            stmt.setString(1, customerId);
            stmt.executeUpdate();
            loadCustomerData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void setupBookingsTab() {
        VBox bookingsPane = new VBox(10);
        bookingsPane.setPadding(new Insets(10));
        bookingsPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        bookingTable = new TableView<>();
        bookingTable.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5;");

        TableColumn<Booking, String> idCol = new TableColumn<>("Booking ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));

        TableColumn<Booking, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            String customerId = cellData.getValue().getCustomerId();
            return new SimpleStringProperty(getCustomerName(customerId));
        });

        TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(cellData -> {
            String vehicleId = cellData.getValue().getVehicleId();
            return new SimpleStringProperty(getVehicleDescription(vehicleId));
        });

        TableColumn<Booking, LocalDate> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<Booking, LocalDate> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        TableColumn<Booking, Number> priceCol = new TableColumn<>("Total Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookingTable.getColumns().addAll(idCol, customerCol, vehicleCol, startCol, endCol, priceCol, statusCol);
        bookingTable.setItems(bookingData);

        HBox bookingControls = new HBox(10);
        Button addBookingBtn = createStyledButton("Add Booking", "#2ecc71");
        Button editBookingBtn = createStyledButton("Edit Booking", "#3498db");
        Button deleteBookingBtn = createStyledButton("Delete Booking", "#e74c3c");

        addBookingBtn.setOnAction(e -> showBookingDialog(null));
        editBookingBtn.setOnAction(e -> {
            Booking selected = bookingTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookingDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking to edit.");
            }
        });

        deleteBookingBtn.setOnAction(e -> {
            Booking selected = bookingTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmation("Delete Booking", "Are you sure you want to delete this booking?")) {
                    deleteBooking(selected.getBookingId());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking to delete.");
            }
        });

        bookingControls.getChildren().addAll(addBookingBtn, editBookingBtn, deleteBookingBtn);
        bookingsPane.getChildren().addAll(bookingTable, bookingControls);
        bookingsTab.setContent(bookingsPane);
    }

    private void showBookingDialog(Booking booking) {
        Dialog<Booking> dialog = new Dialog<>();
        dialog.setTitle(booking == null ? "Add New Booking" : "Edit Booking");
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField bookingIdField = new TextField();
        bookingIdField.setDisable(booking != null);
        bookingIdField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<Customer> customerCombo = new ComboBox<>(customerData);
        customerCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<Vehicle> vehicleCombo = new ComboBox<>(vehicleData);
        vehicleCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        DatePicker startDateField = new DatePicker();
        startDateField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        DatePicker endDateField = new DatePicker();
        endDateField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField totalPriceField = new TextField();
        totalPriceField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Confirmed", "Cancelled", "Completed");
        statusCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        if (booking != null) {
            bookingIdField.setText(booking.getBookingId());
            customerCombo.setValue(findCustomerById(booking.getCustomerId()));
            vehicleCombo.setValue(findVehicleById(booking.getVehicleId()));
            startDateField.setValue(booking.getStartDate());
            endDateField.setValue(booking.getEndDate());
            totalPriceField.setText(String.valueOf(booking.getTotalPrice()));
            statusCombo.setValue(booking.getStatus());
        } else {
            bookingIdField.setText("B" + System.currentTimeMillis());
            statusCombo.setValue("Confirmed");
        }

        grid.add(new Label("Booking ID:"), 0, 0);
        grid.add(bookingIdField, 1, 0);
        grid.add(new Label("Customer:"), 0, 1);
        grid.add(customerCombo, 1, 1);
        grid.add(new Label("Vehicle:"), 0, 2);
        grid.add(vehicleCombo, 1, 2);
        grid.add(new Label("Start Date:"), 0, 3);
        grid.add(startDateField, 1, 3);
        grid.add(new Label("End Date:"), 0, 4);
        grid.add(endDateField, 1, 4);
        grid.add(new Label("Total Price:"), 0, 5);
        grid.add(totalPriceField, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new Booking(
                            bookingIdField.getText(),
                            customerCombo.getValue().getCustomerId(),
                            vehicleCombo.getValue().getVehicleId(),
                            startDateField.getValue(),
                            endDateField.getValue(),
                            Double.parseDouble(totalPriceField.getText()),
                            statusCombo.getValue()
                    );
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please fill all fields correctly");
                    return null;
                }
            }
            return null;
        });

        Optional<Booking> result = dialog.showAndWait();
        result.ifPresent(updatedBooking -> {
            if (booking == null) {
                addBooking(updatedBooking);
            } else {
                updateBooking(updatedBooking);
            }
        });
    }

    private Customer findCustomerById(String customerId) {
        for (Customer customer : customerData) {
            if (customer.getCustomerId().equals(customerId)) {
                return customer;
            }
        }
        return null;
    }

    private Vehicle findVehicleById(String vehicleId) {
        for (Vehicle vehicle : vehicleData) {
            if (vehicle.getVehicleId().equals(vehicleId)) {
                return vehicle;
            }
        }
        return null;
    }

    private void addBooking(Booking booking) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO bookings (bookingId, customerId, vehicleId, startDate, endDate, totalPrice, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)");

            stmt.setString(1, booking.getBookingId());
            stmt.setString(2, booking.getCustomerId());
            stmt.setString(3, booking.getVehicleId());
            stmt.setDate(4, Date.valueOf(booking.getStartDate()));
            stmt.setDate(5, Date.valueOf(booking.getEndDate()));
            stmt.setDouble(6, booking.getTotalPrice());
            stmt.setString(7, booking.getStatus());

            stmt.executeUpdate();
            loadBookingData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void updateBooking(Booking booking) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE bookings SET customerId=?, vehicleId=?, startDate=?, endDate=?, totalPrice=?, status=? " +
                            "WHERE bookingId=?");

            stmt.setString(1, booking.getCustomerId());
            stmt.setString(2, booking.getVehicleId());
            stmt.setDate(3, Date.valueOf(booking.getStartDate()));
            stmt.setDate(4, Date.valueOf(booking.getEndDate()));
            stmt.setDouble(5, booking.getTotalPrice());
            stmt.setString(6, booking.getStatus());
            stmt.setString(7, booking.getBookingId());

            stmt.executeUpdate();
            loadBookingData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void deleteBooking(String bookingId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM bookings WHERE bookingId=?");
            stmt.setString(1, bookingId);
            stmt.executeUpdate();
            loadBookingData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void loadBookingData() {
        bookingData.clear();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM bookings ORDER BY startDate DESC");

            while (rs.next()) {
                bookingData.add(new Booking(
                        rs.getString("bookingId"),
                        rs.getString("customerId"),
                        rs.getString("vehicleId"),
                        rs.getDate("startDate").toLocalDate(),
                        rs.getDate("endDate").toLocalDate(),
                        rs.getDouble("totalPrice"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void setupPaymentsTab() {
        VBox paymentsPane = new VBox(10);
        paymentsPane.setPadding(new Insets(10));
        paymentsPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        paymentTable = new TableView<>();
        paymentTable.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5;");

        TableColumn<Payment, String> paymentIdCol = new TableColumn<>("Payment ID");
        paymentIdCol.setCellValueFactory(new PropertyValueFactory<>("paymentId"));

        TableColumn<Payment, String> bookingIdCol = new TableColumn<>("Booking ID");
        bookingIdCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));

        TableColumn<Payment, Number> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Payment, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        TableColumn<Payment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        paymentTable.getColumns().addAll(paymentIdCol, bookingIdCol, amountCol, methodCol, dateCol);
        paymentTable.setItems(paymentData);

        HBox paymentControls = new HBox(10);
        Button createPaymentBtn = createStyledButton("Create Payment", "#2ecc71");
        Button updatePaymentBtn = createStyledButton("Update Payment", "#3498db");
        Button deletePaymentBtn = createStyledButton("Delete Payment", "#e74c3c");

        createPaymentBtn.setOnAction(e -> {
            Booking selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                showPaymentDialog(selectedBooking);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booking to create a payment for.");
            }
        });

        updatePaymentBtn.setOnAction(e -> {
            Payment selectedPayment = paymentTable.getSelectionModel().getSelectedItem();
            if (selectedPayment != null) {
                showUpdatePaymentDialog(selectedPayment);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payment to update.");
            }
        });

        deletePaymentBtn.setOnAction(e -> {
            Payment selectedPayment = paymentTable.getSelectionModel().getSelectedItem();
            if (selectedPayment != null) {
                if (showConfirmation("Delete Payment", "Are you sure you want to delete this payment?")) {
                    deletePayment(selectedPayment.getPaymentId());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a payment to delete.");
            }
        });

        paymentControls.getChildren().addAll(createPaymentBtn, updatePaymentBtn, deletePaymentBtn);
        paymentsPane.getChildren().addAll(paymentTable, paymentControls);
        paymentsTab.setContent(paymentsPane);

        loadPaymentData();
    }

    private void showPaymentDialog(Booking booking) {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Process Payment");
        dialog.setHeaderText("Process payment for booking: " + booking.getBookingId());
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType processButtonType = new ButtonType("Process", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(processButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField bookingField = new TextField(booking.getBookingId());
        bookingField.setDisable(true);
        bookingField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField amountField = new TextField(String.format("%.2f", booking.getTotalPrice()));
        amountField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("Cash", "Credit Card", "Online");
        methodCombo.setValue("Cash");
        methodCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        grid.add(new Label("Booking ID:"), 0, 0);
        grid.add(bookingField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Payment Method:"), 0, 2);
        grid.add(methodCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == processButtonType) {
                try {
                    String paymentDate = LocalDate.now().toString();
                    return new Payment(
                            "P" + System.currentTimeMillis(),
                            booking.getBookingId(),
                            Double.parseDouble(amountField.getText()),
                            methodCombo.getValue(),
                            paymentDate
                    );
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Please enter a valid payment amount.");
                    return null;
                }
            }
            return null;
        });

        Optional<Payment> result = dialog.showAndWait();
        result.ifPresent(payment -> {
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO payments (paymentId, bookingId, amount, paymentMethod, paymentDate) " +
                                "VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, payment.getPaymentId());
                stmt.setString(2, payment.getBookingId());
                stmt.setDouble(3, payment.getAmount());
                stmt.setString(4, payment.getPaymentMethod());
                stmt.setString(5, payment.getPaymentDate());

                stmt.executeUpdate();
                loadPaymentData();

                showAlert(Alert.AlertType.INFORMATION, "Success", "Payment processed successfully!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
            }
        });
    }

    private void loadPaymentData() {
        paymentData.clear();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM payments ORDER BY paymentDate DESC");

            while (rs.next()) {
                paymentData.add(new Payment(
                        rs.getString("paymentId"),
                        rs.getString("bookingId"),
                        rs.getDouble("amount"),
                        rs.getString("paymentMethod"),
                        rs.getString("paymentDate")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showUpdatePaymentDialog(Payment payment) {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Update Payment");
        dialog.setHeaderText("Update payment details");
        dialog.getDialogPane().setStyle("-fx-background-color: #ecf0f1;");

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField bookingField = new TextField(payment.getBookingId());
        bookingField.setDisable(true);
        bookingField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        TextField amountField = new TextField(String.format("%.2f", payment.getAmount()));
        amountField.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("Cash", "Credit Card", "Online");
        methodCombo.setValue(payment.getPaymentMethod());
        methodCombo.setStyle("-fx-background-color: white; -fx-border-color: #3498db;");

        grid.add(new Label("Booking ID:"), 0, 0);
        grid.add(bookingField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Payment Method:"), 0, 2);
        grid.add(methodCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    return new Payment(
                            payment.getPaymentId(),
                            payment.getBookingId(),
                            Double.parseDouble(amountField.getText()),
                            methodCombo.getValue(),
                            payment.getPaymentDate()
                    );
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Please enter a valid payment amount.");
                    return null;
                }
            }
            return null;
        });

        Optional<Payment> result = dialog.showAndWait();
        result.ifPresent(updatedPayment -> updatePayment(updatedPayment));
    }

    private void updatePayment(Payment payment) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE payments SET amount=?, paymentMethod=? WHERE paymentId=?");

            stmt.setDouble(1, payment.getAmount());
            stmt.setString(2, payment.getPaymentMethod());
            stmt.setString(3, payment.getPaymentId());

            stmt.executeUpdate();
            loadPaymentData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void deletePayment(String paymentId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM payments WHERE paymentId=?");
            stmt.setString(1, paymentId);
            stmt.executeUpdate();
            loadPaymentData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void setupReportsTab() {
        VBox reportsPane = new VBox(10);
        reportsPane.setPadding(new Insets(10));
        reportsPane.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        Button availableVehiclesButton = createStyledButton("Available Vehicles Report", "#3498db");
        Button rentalHistoryButton = createStyledButton("Rental History Report", "#3498db");
        Button revenueButton = createStyledButton("Revenue Report", "#3498db");
        Button userStatsButton = createStyledButton("User Statistics", "#3498db");
        Button vehicleStatsButton = createStyledButton("Vehicle Statistics", "#3498db");

        availableVehiclesButton.setOnAction(e -> showAvailableVehiclesReport());
        rentalHistoryButton.setOnAction(e -> showRentalHistoryReport());
        revenueButton.setOnAction(e -> showRevenueReport());
        userStatsButton.setOnAction(e -> showUserStatistics());
        vehicleStatsButton.setOnAction(e -> showVehicleStatistics());

        reportsPane.getChildren().addAll(availableVehiclesButton, rentalHistoryButton,
                revenueButton, userStatsButton, vehicleStatsButton);
        reportsTab = new Tab("Reports");
        reportsTab.setContent(reportsPane);
    }

    private void showAvailableVehiclesReport() {
        try {
            Stage reportStage = new Stage();
            reportStage.setTitle("Available Vehicles Report");

            TableView<Vehicle> reportTable = new TableView<>();
            reportTable.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
            ObservableList<Vehicle> availableVehicles = FXCollections.observableArrayList();

            String query = "SELECT * FROM vehicles WHERE available = TRUE";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                availableVehicles.add(new Vehicle(
                        rs.getString("vehicleId"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getString("category"),
                        rs.getInt("year"),
                        rs.getDouble("pricePerDay"),
                        true
                ));
            }

            TableColumn<Vehicle, String> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));

            TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
            brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

            TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
            modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

            TableColumn<Vehicle, String> categoryCol = new TableColumn<>("Category");
            categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

            TableColumn<Vehicle, Number> pricePerDayCol = new TableColumn<>("Price/Day");
            pricePerDayCol.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));

            reportTable.getColumns().addAll(idCol, brandCol, modelCol, categoryCol, pricePerDayCol);
            reportTable.setItems(availableVehicles);

            VBox reportLayout = new VBox(reportTable);
            reportLayout.setStyle("-fx-background-color: #f5f5f5;");
            Scene reportScene = new Scene(reportLayout, 800, 600);
            reportStage.setScene(reportScene);
            reportStage.show();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showRentalHistoryReport() {
        try {
            Stage reportStage = new Stage();
            reportStage.setTitle("Customer Rental History Report");

            TableView<Booking> reportTable = new TableView<>();
            reportTable.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
            ObservableList<Booking> rentalHistory = FXCollections.observableArrayList();

            String query = "SELECT * FROM bookings";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                rentalHistory.add(new Booking(
                        rs.getString("bookingId"),
                        rs.getString("customerId"),
                        rs.getString("vehicleId"),
                        rs.getDate("startDate").toLocalDate(),
                        rs.getDate("endDate").toLocalDate(),
                        rs.getDouble("totalPrice"),
                        rs.getString("status")
                ));
            }

            TableColumn<Booking, String> bookingIdCol = new TableColumn<>("Booking ID");
            bookingIdCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));

            TableColumn<Booking, String> customerCol = new TableColumn<>("Customer");
            customerCol.setCellValueFactory(cellData -> {
                String customerId = cellData.getValue().getCustomerId();
                return new SimpleStringProperty(getCustomerName(customerId));
            });

            TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
            vehicleCol.setCellValueFactory(cellData -> {
                String vehicleId = cellData.getValue().getVehicleId();
                return new SimpleStringProperty(getVehicleDescription(vehicleId));
            });

            TableColumn<Booking, LocalDate> startDateCol = new TableColumn<>("Start Date");
            startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

            TableColumn<Booking, LocalDate> endDateCol = new TableColumn<>("End Date");
            endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

            TableColumn<Booking, Number> totalPriceCol = new TableColumn<>("Total Price");
            totalPriceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

            reportTable.getColumns().addAll(bookingIdCol, customerCol, vehicleCol, startDateCol, endDateCol, totalPriceCol);
            reportTable.setItems(rentalHistory);

            VBox reportLayout = new VBox(reportTable);
            reportLayout.setStyle("-fx-background-color: #f5f5f5;");
            Scene reportScene = new Scene(reportLayout, 1000, 600);
            reportStage.setScene(reportScene);
            reportStage.show();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showRevenueReport() {
        try {
            Stage reportStage = new Stage();
            reportStage.setTitle("Revenue Report");

            // Create a bar chart to show revenue by payment method
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Payment Method");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Amount");

            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setTitle("Revenue by Payment Method");
            barChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenue");

            // Query to get revenue by payment method
            String query = "SELECT paymentMethod, SUM(amount) as total FROM payments GROUP BY paymentMethod";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(
                        rs.getString("paymentMethod"),
                        rs.getDouble("total")
                ));
            }

            barChart.getData().add(series);

            // Create a pie chart to show revenue distribution
            PieChart pieChart = new PieChart();
            pieChart.setTitle("Revenue Distribution");
            pieChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            // Query to get total revenue
            query = "SELECT SUM(amount) as total FROM payments";
            rs = stmt.executeQuery(query);
            double totalRevenue = rs.next() ? rs.getDouble("total") : 0;

            if (totalRevenue > 0) {
                // Query to get revenue by month
                query = "SELECT MONTH(paymentDate) as month, SUM(amount) as monthlyTotal " +
                        "FROM payments GROUP BY MONTH(paymentDate)";
                rs = stmt.executeQuery(query);

                while (rs.next()) {
                    int month = rs.getInt("month");
                    double monthlyTotal = rs.getDouble("monthlyTotal");
                    pieChart.getData().add(new PieChart.Data(
                            "Month " + month + " (" + String.format("%.2f", (monthlyTotal/totalRevenue)*100) + "%)",
                            monthlyTotal
                    ));
                }
            }

            VBox reportLayout = new VBox(20, barChart, pieChart);
            reportLayout.setPadding(new Insets(20));
            reportLayout.setStyle("-fx-background-color: #f5f5f5;");
            Scene reportScene = new Scene(reportLayout, 1000, 800);
            reportStage.setScene(reportScene);
            reportStage.show();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showUserStatistics() {
        try {
            Stage reportStage = new Stage();
            reportStage.setTitle("User Statistics");

            // Create a pie chart for user roles
            PieChart rolePieChart = new PieChart();
            rolePieChart.setTitle("User Roles Distribution");
            rolePieChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            String query = "SELECT role, COUNT(*) as count FROM users GROUP BY role";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                rolePieChart.getData().add(new PieChart.Data(
                        rs.getString("role") + " (" + rs.getInt("count") + ")",
                        rs.getInt("count")
                ));
            }

            // Create a bar chart for user activity (users with most bookings)
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("User");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Number of Bookings");

            BarChart<String, Number> activityChart = new BarChart<>(xAxis, yAxis);
            activityChart.setTitle("User Activity (Bookings per User)");
            activityChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Bookings");

            query = "SELECT u.name, COUNT(b.bookingId) as bookingCount " +
                    "FROM users u LEFT JOIN bookings b ON u.name = " +
                    "(SELECT name FROM customers WHERE customerId = b.customerId) " +
                    "GROUP BY u.name ORDER BY bookingCount DESC LIMIT 5";
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(
                        rs.getString("name"),
                        rs.getInt("bookingCount")
                ));
            }

            activityChart.getData().add(series);

            VBox reportLayout = new VBox(20, rolePieChart, activityChart);
            reportLayout.setPadding(new Insets(20));
            reportLayout.setStyle("-fx-background-color: #f5f5f5;");
            Scene reportScene = new Scene(reportLayout, 800, 800);
            reportStage.setScene(reportScene);
            reportStage.show();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showVehicleStatistics() {
        try {
            Stage reportStage = new Stage();
            reportStage.setTitle("Vehicle Statistics");

            // Create a pie chart for vehicle categories
            PieChart categoryPieChart = new PieChart();
            categoryPieChart.setTitle("Vehicle Categories");
            categoryPieChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            String query = "SELECT category, COUNT(*) as count FROM vehicles GROUP BY category";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                categoryPieChart.getData().add(new PieChart.Data(
                        rs.getString("category") + " (" + rs.getInt("count") + ")",
                        rs.getInt("count")
                ));
            }

            // Create a bar chart for most rented vehicles
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Vehicle");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Number of Rentals");

            BarChart<String, Number> rentalChart = new BarChart<>(xAxis, yAxis);
            rentalChart.setTitle("Most Rented Vehicles");
            rentalChart.setStyle("-fx-background-color: white; -fx-background-radius: 5;");

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rentals");

            query = "SELECT v.brand, v.model, COUNT(b.bookingId) as rentalCount " +
                    "FROM vehicles v LEFT JOIN bookings b ON v.vehicleId = b.vehicleId " +
                    "GROUP BY v.vehicleId ORDER BY rentalCount DESC LIMIT 5";
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(
                        rs.getString("brand") + " " + rs.getString("model"),
                        rs.getInt("rentalCount")
                ));
            }

            rentalChart.getData().add(series);

            VBox reportLayout = new VBox(20, categoryPieChart, rentalChart);
            reportLayout.setPadding(new Insets(20));
            reportLayout.setStyle("-fx-background-color: #f5f5f5;");
            Scene reportScene = new Scene(reportLayout, 800, 800);
            reportStage.setScene(reportScene);
            reportStage.show();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ecf0f1;");
        dialogPane.getScene().getWindow().setOnCloseRequest(e -> dialogPane.getScene().getWindow().hide());

        alert.showAndWait();
    }

    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the confirmation dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ecf0f1;");
        dialogPane.getScene().getWindow().setOnCloseRequest(e -> dialogPane.getScene().getWindow().hide());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String getCustomerName(String customerId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT name FROM customers WHERE customerId = ?");
            stmt.setString(1, customerId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("name") : "Unknown";
        } catch (SQLException e) {
            return "Error";
        }
    }

    private String getVehicleDescription(String vehicleId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT brand, model FROM vehicles WHERE vehicleId = ?");
            stmt.setString(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("brand") + " " + rs.getString("model") : "Unknown";
        } catch (SQLException e) {
            return "Error";
        }
    }

    // Model Classes
    public static class User {
        private final int id;
        private final String username;
        private final String password;
        private final String role;
        private final String name;

        public User(int id, String username, String password, String role, String name) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.role = role;
            this.name = name;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getName() { return name; }
    }

    public static class Vehicle {
        private final String vehicleId;
        private final String brand;
        private final String model;
        private final String category;
        private final int year;
        private final double pricePerDay;
        private final boolean available;

        public Vehicle(String vehicleId, String brand, String model, String category, int year, double pricePerDay, boolean available) {
            this.vehicleId = vehicleId;
            this.brand = brand;
            this.model = model;
            this.category = category;
            this.year = year;
            this.pricePerDay = pricePerDay;
            this.available = available;
        }

        public String getVehicleId() { return vehicleId; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public String getCategory() { return category; }
        public int getYear() { return year; }
        public double getPricePerDay() { return pricePerDay; }
        public boolean isAvailable() { return available; }
    }

    public static class Customer {
        private final String customerId;
        private final String name;
        private final String phone;
        private final String email;
        private final String licenseNumber;
        private final String address;

        public Customer(String customerId, String name, String phone, String email, String licenseNumber, String address) {
            this.customerId = customerId;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.licenseNumber = licenseNumber;
            this.address = address;
        }

        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getLicenseNumber() { return licenseNumber; }
        public String getAddress() { return address; }
    }

    public static class Booking {
        private final String bookingId;
        private final String customerId;
        private final String vehicleId;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final double totalPrice;
        private final String status;

        public Booking(String bookingId, String customerId, String vehicleId, LocalDate startDate, LocalDate endDate, double totalPrice, String status) {
            this.bookingId = bookingId;
            this.customerId = customerId;
            this.vehicleId = vehicleId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalPrice = totalPrice;
            this.status = status;
        }

        public String getBookingId() { return bookingId; }
        public String getCustomerId() { return customerId; }
        public String getVehicleId() { return vehicleId; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public double getTotalPrice() { return totalPrice; }
        public String getStatus() { return status; }
    }

    public static class Payment {
        private final String paymentId;
        private final String bookingId;
        private final double amount;
        private final String paymentMethod;
        private final String paymentDate;

        public Payment(String paymentId, String bookingId, double amount, String paymentMethod, String paymentDate) {
            this.paymentId = paymentId;
            this.bookingId = bookingId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDate = paymentDate;
        }

        public String getPaymentId() { return paymentId; }
        public String getBookingId() { return bookingId; }
        public double getAmount() { return amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getPaymentDate() { return paymentDate; }
    }
}