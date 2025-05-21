package com.aircraft.controller;

import com.aircraft.dao.AircraftDAO;
import com.aircraft.dao.LauncherDAO;
import com.aircraft.dao.MissionDAO;
import com.aircraft.dao.WeaponDAO;
import com.aircraft.model.Aircraft;
import com.aircraft.model.Launcher;
import com.aircraft.model.Mission;
import com.aircraft.model.Weapon;
import com.aircraft.util.AlertUtils;
import com.aircraft.util.DBUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller for the Mission Management screen with integrated missile position visualization.
 * Updated to support the new database structure with direct missile and launcher references in the mission table.
 */
public class MissionManagementController {

    @FXML
    private ComboBox<Aircraft> aircraftComboBox;

    @FXML
    private DatePicker missionDatePicker;

    @FXML
    private TextField flightNumberField;

    @FXML
    private TextField timeStartField;

    @FXML
    private TextField timeFinishField;

    @FXML
    private StackPane aircraftContainer;

    @FXML
    private WebView aircraftSvgView;

    @FXML
    private AnchorPane missilePointsContainer;

    @FXML
    private GridPane weaponSelectionPanel;

    @FXML
    private Label selectedPositionLabel;

    @FXML
    private Label validationMessageLabel;

    @FXML
    private ComboBox<String> launcherComboBox;

    @FXML
    private ComboBox<String> weaponComboBox;

    @FXML
    private Button savePositionButton;

    @FXML
    private Button saveAllButton;

    @FXML
    private Button clearAllButton;

    private final Map<String, Pane> missilePointsMap = new HashMap<>();
    private final Map<String, MissionWeaponConfig> missilePositionsData = new HashMap<>();
    private String currentSelectedPosition = null;

    private final AircraftDAO aircraftDAO = new AircraftDAO();
    private final MissionDAO missionDAO = new MissionDAO();
    private final LauncherDAO launcherDAO = new LauncherDAO();
    private final WeaponDAO weaponDAO = new WeaponDAO();

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // Class to hold the data for each missile position
    private static class MissionWeaponConfig {
        private final String position;
        private String launcherId;
        private String weaponId;

        public MissionWeaponConfig(String position) {
            this.position = position;
            this.launcherId = null;
            this.weaponId = null;
        }

        public String getPosition() {
            return position;
        }

        public String getLauncherId() {
            return launcherId;
        }

        public void setLauncherId(String launcherId) {
            this.launcherId = launcherId;
        }

        public String getWeaponId() {
            return weaponId;
        }

        public void setWeaponId(String weaponId) {
            this.weaponId = weaponId;
        }

        public boolean hasLauncher() {
            return launcherId != null && !launcherId.isEmpty();
        }

        public boolean hasWeapon() {
            return weaponId != null && !weaponId.isEmpty();
        }

        public boolean isLoaded() {
            return hasLauncher() && hasWeapon();
        }
    }

    /**
     * Initializes the controller after its root element has been processed.
     */
    @FXML
    public void initialize() {
        setupTimeFields();
        setupComboBoxes();
        setupDatePicker();

        // Load SVG after the WebView is fully initialized
        Platform.runLater(this::loadAircraftSvg);
    }

    /**
     * Sets up the time fields with input validation.
     */
    private void setupTimeFields() {
        // Time pattern validation setup
        timeStartField.setPromptText("HH:MM");
        timeFinishField.setPromptText("HH:MM");

        // Add validation to time fields
        timeStartField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // when focus lost
                validateTimeField(timeStartField);
            }
        });

        timeFinishField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // when focus lost
                validateTimeField(timeFinishField);
            }
        });
    }

    /**
     * Validates a time field to ensure it follows the HH:MM format.
     */
    private boolean validateTimeField(TextField field) {
        String timeValue = field.getText().trim();
        if (timeValue.isEmpty()) {
            return true; // Empty is allowed
        }

        try {
            LocalTime.parse(timeValue, timeFormatter);
            field.setStyle(""); // Reset style if valid
            return true;
        } catch (DateTimeParseException e) {
            field.setStyle("-fx-border-color: red;");
            return false;
        }
    }

    /**
     * Sets up the combo boxes with appropriate data.
     */
    private void setupComboBoxes() {
        // Load aircraft list
        List<Aircraft> aircraftList = aircraftDAO.getAll();
        ObservableList<Aircraft> aircraftItems = FXCollections.observableArrayList(aircraftList);
        aircraftComboBox.setItems(aircraftItems);

        // Set up cell factory for aircraft display
        aircraftComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Aircraft item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getMatricolaVelivolo());
                }
            }
        });

        // Set up button cell to display selected aircraft
        aircraftComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Aircraft item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getMatricolaVelivolo());
                }
            }
        });

        // Initially hide weapon selection panel
        weaponSelectionPanel.setVisible(false);

        // Initially hide validation message
        validationMessageLabel.setVisible(false);
    }

    /**
     * Sets up the date picker with default value.
     */
    private void setupDatePicker() {
        missionDatePicker.setValue(LocalDate.now());
    }

    /**
     * Loads the aircraft SVG into the WebView.
     */
    private void loadAircraftSvg() {
        try {
            // Load SVG content from resources
            InputStream svgStream = getClass().getResourceAsStream("/images/aircraft_rear.svg");
            if (svgStream == null) {
                AlertUtils.showError(null, "Error", "Could not load aircraft SVG");
                return;
            }

            String svgContent = new String(svgStream.readAllBytes(), StandardCharsets.UTF_8);

            // Load SVG into WebView
            WebEngine engine = aircraftSvgView.getEngine();
            engine.loadContent("<html><body style='margin:0;overflow:hidden;'>" + svgContent + "</body></html>", "text/html");

            // After SVG is loaded, create missile points
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    createMissilePoints();
                    setupPositionSelectionListeners();
                }
            });
        } catch (Exception e) {
            AlertUtils.showError(null, "Error", "Failed to load aircraft SVG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates missile point indicators at the predefined positions.
     * For simplicity, we'll focus only on P1 and P13 as mentioned in the requirements.
     */
    private void createMissilePoints() {
        // Define positions - adjusted to align better with the SVG
        // We're focusing only on P1 and P13 as per requirements
        Map<String, double[]> positions = new HashMap<>();
        positions.put("P1", new double[]{90, 170});
        positions.put("P13", new double[]{460, 170});

        // Create missile position data objects
        for (String position : positions.keySet()) {
            missilePositionsData.put(position, new MissionWeaponConfig(position));
        }

        // Clear any existing missile points
        missilePointsContainer.getChildren().clear();

        // Create visual indicators for each position
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            String position = entry.getKey();
            double[] coords = entry.getValue();

            // Create a missile point indicator (rectangle) - TRANSPARENT instead of filled
            Rectangle point = new Rectangle(24, 40);
            point.setFill(Color.TRANSPARENT); // Set to transparent instead of filled
            point.setStroke(Color.GRAY); // Add a border
            point.setStrokeWidth(1.5); // Make border visible

            // Create position label
            Text label = new Text(position);
            label.getStyleClass().add("missile-point-label");
            label.setFill(Color.BLACK); // Ensure label is visible

            // Combine in a StackPane
            StackPane missilePoint = new StackPane(point, label);
            missilePoint.setLayoutX(coords[0] - 12); // Center the rectangle
            missilePoint.setLayoutY(coords[1] - 20);

            // Store for later reference
            missilePointsMap.put(position, missilePoint);

            // Add click event
            missilePoint.setOnMouseClicked(event -> selectPosition(position));

            // Add to container
            missilePointsContainer.getChildren().add(missilePoint);
        }
    }

    /**
     * Sets up listeners for position selection changes.
     */
    private void setupPositionSelectionListeners() {
        // Load launcher and weapon lists when a position is selected
        weaponSelectionPanel.visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                loadLauncherAndWeaponLists();
            } else {
                // When the panel is hidden, update the position to its true state
                updatePositionStatus();
            }
        });

        // Add a listener for clicking on the background
        missilePointsContainer.setOnMouseClicked(event -> {
            if (event.getTarget() == missilePointsContainer) {
                // Only if clicked directly on the container, not on a position
                updatePositionStatus();
                currentSelectedPosition = null;
                weaponSelectionPanel.setVisible(false);
            }
        });
    }


    /**
     * Loads the launcher and weapon lists for the position selection panel.
     */
    private void loadLauncherAndWeaponLists() {
        // Load launcher list
        List<Launcher> launcherList = launcherDAO.getAll();
        ObservableList<String> launcherItems = FXCollections.observableArrayList();
        launcherItems.add(""); // Empty option

        // Show both nomenclature and part number in the dropdown
        launcherItems.addAll(launcherList.stream()
                .map(l -> l.getNomenclatura() + " (" + l.getPartNumber() + ")")
                .toList());
        launcherComboBox.setItems(launcherItems);

        // Set up launcher selection listener
        launcherComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                // Extract part number from string like "Nomenclature (PartNumber)"
                String partNumberWithParens = newVal.substring(newVal.lastIndexOf("("));
                String partNumber = partNumberWithParens.substring(1, partNumberWithParens.length() - 1);

                // Find the selected launcher
                Optional<Launcher> selectedLauncher = launcherList.stream()
                        .filter(l -> l.getPartNumber().equals(partNumber))
                        .findFirst();

                // Update part number field
                selectedLauncher.ifPresent(launcher -> {
                    if (currentSelectedPosition != null) {
                        MissionWeaponConfig config = missilePositionsData.get(currentSelectedPosition);
                        config.setLauncherId(launcher.getPartNumber());
                    }

                    // Enable weapon selection only after launcher is selected
                    weaponComboBox.setDisable(false);
                    validationMessageLabel.setVisible(false);
                });
            } else {
                // Existing code for handling null selection
                weaponComboBox.getSelectionModel().clearSelection();
                weaponComboBox.setDisable(true);

                if (currentSelectedPosition != null) {
                    MissionWeaponConfig config = missilePositionsData.get(currentSelectedPosition);
                    config.setLauncherId(null);
                    config.setWeaponId(null);
                }
            }
        });

        // Update the weapons dropdown similarly
        List<Weapon> weaponList = weaponDAO.getAll();
        ObservableList<String> weaponItems = FXCollections.observableArrayList();
        weaponItems.add(""); // Empty option

        // Show both nomenclature and part number in the dropdown
        weaponItems.addAll(weaponList.stream()
                .map(w -> w.getNomenclatura() + " (" + w.getPartNumber() + ")")
                .toList());
        weaponComboBox.setItems(weaponItems);
        weaponComboBox.setDisable(true); // Initially disabled

        // Set up weapon selection listener
        weaponComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                // Check if launcher is selected
                MissionWeaponConfig config = missilePositionsData.get(currentSelectedPosition);
                if (config != null && !config.hasLauncher()) {
                    validationMessageLabel.setText("You must select a launcher before selecting a weapon");
                    validationMessageLabel.setVisible(true);
                    weaponComboBox.setValue("");
                    return;
                }

                // Extract part number from string
                String partNumberWithParens = newVal.substring(newVal.lastIndexOf("("));
                String partNumber = partNumberWithParens.substring(1, partNumberWithParens.length() - 1);

                // Find the selected weapon
                Optional<Weapon> selectedWeapon = weaponList.stream()
                        .filter(w -> w.getPartNumber().equals(partNumber))
                        .findFirst();

                // Update part number field
                selectedWeapon.ifPresent(weapon -> {
                    if (currentSelectedPosition != null) {
                        config.setWeaponId(weapon.getPartNumber());
                    }
                });

                validationMessageLabel.setVisible(false);
            } else {
                if (currentSelectedPosition != null) {
                    MissionWeaponConfig config = missilePositionsData.get(currentSelectedPosition);
                    config.setWeaponId(null);
                }
                validationMessageLabel.setVisible(false);
            }
        });
    }

    /**
     * Selects a missile position and shows its details.
     *
     * @param position The position identifier (e.g., "P1")
     */
    private void selectPosition(String position) {
        // Update previous position based on its actual configuration
        if (currentSelectedPosition != null && missilePointsMap.containsKey(currentSelectedPosition)) {
            // First update based on configuration
            updateMissilePointUI(currentSelectedPosition);

            // Then reset styling
            Pane previousPoint = missilePointsMap.get(currentSelectedPosition);
            Rectangle rect = (Rectangle) ((StackPane) previousPoint).getChildren().get(0);
            rect.setStroke(Color.GRAY); // Reset border color
            rect.setStrokeWidth(1.5);
        }

        // Set new selection
        currentSelectedPosition = position;

        // Update UI - highlight with green border
        Pane selectedPoint = missilePointsMap.get(position);
        Rectangle rect = (Rectangle) ((StackPane) selectedPoint).getChildren().get(0);
        rect.setStroke(Color.GREEN); // Highlight with green border
        rect.setStrokeWidth(2.5); // Make border thicker

        // Update position panel
        selectedPositionLabel.setText("Position: " + position);

        // Load position data
        MissionWeaponConfig config = missilePositionsData.get(position);

        // Set launcher and weapon selections based on loaded data
        if (config.hasLauncher()) {
            // Find launcher by part number
            Optional<Launcher> launcher = launcherDAO.getAll().stream()
                    .filter(l -> l.getPartNumber().equals(config.getLauncherId()))
                    .findFirst();

            launcher.ifPresent(l -> launcherComboBox.setValue(l.getNomenclatura() + " (" + l.getPartNumber() + ")"));
        } else {
            launcherComboBox.setValue("");
        }

        if (config.hasWeapon()) {
            // Find weapon by part number
            Optional<Weapon> weapon = weaponDAO.getAll().stream()
                    .filter(w -> w.getPartNumber().equals(config.getWeaponId()))
                    .findFirst();

            weapon.ifPresent(w -> weaponComboBox.setValue(w.getNomenclatura() + " (" + w.getPartNumber() + ")"));
        } else {
            weaponComboBox.setValue("");
        }

        // Reset validation message
        validationMessageLabel.setVisible(false);

        // Show position panel
        weaponSelectionPanel.setVisible(true);
    }

    /**
     * Updates the UI for a specific missile position.
     */
    private void updateMissilePointUI(String position) {
        MissionWeaponConfig config = missilePositionsData.get(position);
        Pane missilePoint = missilePointsMap.get(position);

        if (missilePoint != null) {
            Rectangle rect = (Rectangle) ((StackPane) missilePoint).getChildren().get(0);

            // Clear existing state styles
            rect.setFill(Color.TRANSPARENT);

            // If the position has a launcher and weapon, show it as loaded
            if (config.isLoaded()) {
                rect.setFill(Color.LIGHTGREEN);
                rect.setOpacity(0.7);
            }
        }
    }

    /**
     * Add this new method to the MissionManagementController class
     * to handle when a user navigates away from a position without saving.
     */
    private void updatePositionStatus() {
        // If there was a previously selected position, update its UI based on actual configuration
        if (currentSelectedPosition != null && missilePointsMap.containsKey(currentSelectedPosition)) {
            updateMissilePointUI(currentSelectedPosition);
        }
    }

    /**
     * Handles the "Save Position" button click.
     */
    @FXML
    protected void onSavePositionClick(ActionEvent event) {
        if (currentSelectedPosition == null) return;

        Window owner = ((Node) event.getSource()).getScene().getWindow();
        MissionWeaponConfig config = missilePositionsData.get(currentSelectedPosition);

        // Validate launcher selection - launcher is required
        String launcherValue = launcherComboBox.getValue();
        if (launcherValue == null || launcherValue.isEmpty()) {
            validationMessageLabel.setText("Launcher is required");
            validationMessageLabel.setVisible(true);
            return;
        }

        // Extract launcher part number from dropdown value
        String launcherPartNumber = extractPartNumber(launcherValue);
        config.setLauncherId(launcherPartNumber);
        System.out.println("Saved launcher: " + launcherPartNumber + " to position " + currentSelectedPosition);

        // Also save weapon if selected (not required)
        String weaponValue = weaponComboBox.getValue();
        if (weaponValue != null && !weaponValue.isEmpty()) {
            String weaponPartNumber = extractPartNumber(weaponValue);
            config.setWeaponId(weaponPartNumber);
            System.out.println("Saved weapon: " + weaponPartNumber + " to position " + currentSelectedPosition);
        } else {
            config.setWeaponId(null); // Clear weapon if not selected
            System.out.println("No weapon selected for position " + currentSelectedPosition);
        }

        // Update UI to reflect saved state
        updateMissilePointUI(currentSelectedPosition);

        // Turn the save button green to indicate successful save
        savePositionButton.setStyle("-fx-background-color: #2e7d32;");

        // Hide validation message
        validationMessageLabel.setVisible(false);

        AlertUtils.showInformation(owner, "Position Saved",
                "Position " + currentSelectedPosition + " configuration saved successfully.");
    }

    // Helper method to extract part number from dropdown text
    private String extractPartNumber(String dropdownValue) {
        // Extract part number from format "Name (PartNumber)"
        if (dropdownValue.contains("(") && dropdownValue.contains(")")) {
            int start = dropdownValue.lastIndexOf("(") + 1;
            int end = dropdownValue.lastIndexOf(")");
            return dropdownValue.substring(start, end);
        }
        return dropdownValue; // Return as is if no parentheses
    }

    /**
     * Handles the "Save All" button click - saves the mission with launcher and missile data.
     * Updated to work with the new database structure according to requirements.
     */
    // src/main/java/com/aircraft/controller/MissionManagementController.java
    @FXML
    protected void onSaveAllClick(ActionEvent event) {
        Window owner = ((Node) event.getSource()).getScene().getWindow();
        System.out.println("\n----- SAVING MISSION -----");

        // Validate input fields
        Aircraft selectedAircraft = aircraftComboBox.getValue();
        LocalDate missionDate = missionDatePicker.getValue();
        String flightNumber = flightNumberField.getText();

        // CRITICAL DEBUG - Show exactly what's selected
        System.out.println("Selected aircraft: " + (selectedAircraft != null ?
                selectedAircraft.getMatricolaVelivolo() + " (class: " + selectedAircraft.getClass().getName() + ")" : "NULL"));
        System.out.println("Mission date: " + missionDate);
        System.out.println("Flight number: " + flightNumber);

        if (selectedAircraft == null || missionDate == null || flightNumber == null || flightNumber.isEmpty()) {
            AlertUtils.showError(owner, "Validation Error", "Aircraft, date, and flight number are required");
            return;
        }

        // Parse flight number
        int flightNum;
        try {
            flightNum = Integer.parseInt(flightNumber);
        } catch (NumberFormatException e) {
            AlertUtils.showError(owner, "Validation Error", "Flight number must be a valid integer");
            return;
        }

        // Get launcher and missile data for positions
        String launcherP1 = null;
        String missileP1 = null;
        String launcherP13 = null;
        String missileP13 = null;

        // Get data from position configurations with detailed logging
        System.out.println("Checking positions data - Available positions: " +
                (missilePositionsData != null ? missilePositionsData.keySet() : "NULL MAP"));

        if (missilePositionsData.containsKey("P1")) {
            MissionWeaponConfig p1Config = missilePositionsData.get("P1");
            if (p1Config != null && p1Config.hasLauncher()) {
                launcherP1 = p1Config.getLauncherId();
                missileP1 = p1Config.hasWeapon() ? p1Config.getWeaponId() : null;
                System.out.println("P1 configuration: Launcher=" + launcherP1 + ", Missile=" + missileP1);
            }
        }

        if (missilePositionsData.containsKey("P13")) {
            MissionWeaponConfig p13Config = missilePositionsData.get("P13");
            if (p13Config != null && p13Config.hasLauncher()) {
                launcherP13 = p13Config.getLauncherId();
                missileP13 = p13Config.hasWeapon() ? p13Config.getWeaponId() : null;
                System.out.println("P13 configuration: Launcher=" + launcherP13 + ", Missile=" + missileP13);
            }
        }

        // Direct database access with explicit transaction management
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        boolean success = false;

        try {
            // Get connection
            conn = DBUtil.getConnection();

            // Important: Create a simple test query first to verify connection works
            Statement testStmt = conn.createStatement();
            ResultSet testRs = testStmt.executeQuery("SELECT DATABASE() as db");
            if (testRs.next()) {
                System.out.println("Connected to database: " + testRs.getString("db"));
            }
            testRs.close();
            testStmt.close();

            // Disable auto-commit for transaction control
            conn.setAutoCommit(false);

            // Prepare the insert statement
            String sql = "INSERT INTO missione (MatricolaVelivolo, DataMissione, NumeroVolo, OraPartenza, OraArrivo, " +
                    "PartNumberLanciatoreP1, PartNumberMissileP1, PartNumberLanciatoreP13, PartNumberMissileP13) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            System.out.println("SQL Insert: " + sql);
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // Set parameters with detailed logging
            System.out.println("Setting parameters:");
            stmt.setString(1, selectedAircraft.getMatricolaVelivolo());
            System.out.println("1. Aircraft: " + selectedAircraft.getMatricolaVelivolo());

            java.sql.Date sqlDate = java.sql.Date.valueOf(missionDate);
            stmt.setDate(2, sqlDate);
            System.out.println("2. Date: " + sqlDate);

            stmt.setInt(3, flightNum);
            System.out.println("3. Flight Number: " + flightNum);

            // Set times if provided
            if (!timeStartField.getText().isEmpty() && validateTimeField(timeStartField)) {
                LocalTime localTimeStart = LocalTime.parse(timeStartField.getText(), timeFormatter);
                Time sqlTimeStart = Time.valueOf(localTimeStart);
                stmt.setTime(4, sqlTimeStart);
                System.out.println("4. Start Time: " + sqlTimeStart);
            } else {
                stmt.setNull(4, java.sql.Types.TIME);
                System.out.println("4. Start Time: NULL");
            }

            if (!timeFinishField.getText().isEmpty() && validateTimeField(timeFinishField)) {
                LocalTime localTimeFinish = LocalTime.parse(timeFinishField.getText(), timeFormatter);
                Time sqlTimeFinish = Time.valueOf(localTimeFinish);
                stmt.setTime(5, sqlTimeFinish);
                System.out.println("5. Finish Time: " + sqlTimeFinish);
            } else {
                stmt.setNull(5, java.sql.Types.TIME);
                System.out.println("5. Finish Time: NULL");
            }

            // Set launcher and missile part numbers
            stmt.setString(6, launcherP1);
            System.out.println("6. Launcher P1: " + launcherP1);

            stmt.setString(7, missileP1);
            System.out.println("7. Missile P1: " + missileP1);

            stmt.setString(8, launcherP13);
            System.out.println("8. Launcher P13: " + launcherP13);

            stmt.setString(9, missileP13);
            System.out.println("9. Missile P13: " + missileP13);

            // Execute the insert
            System.out.println("Executing statement...");
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Insert affected " + rowsAffected + " rows");

            // Get the generated mission ID
            if (rowsAffected > 0) {
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int missionId = generatedKeys.getInt(1);
                    System.out.println("Mission saved with ID: " + missionId);

                    // CRITICAL: Explicitly commit the transaction
                    conn.commit();
                    System.out.println("Transaction committed successfully");
                    success = true;

                    // Message based on loaded positions
                    int positionsCount = 0;
                    if (launcherP1 != null) positionsCount++;
                    if (launcherP13 != null) positionsCount++;

                    // Create success message
                    String message = "Mission saved successfully";
                    if (positionsCount > 0) {
                        message += " with " + positionsCount + " configured positions";
                    }
                    message += ". ID: " + missionId;

                    AlertUtils.showInformation(owner, "Success", message);

                    // Clear form
                    clearForm();
                } else {
                    conn.rollback();
                    System.out.println("Rolled back - could not retrieve generated key");
                    AlertUtils.showError(owner, "Error", "Could not get mission ID");
                }
            } else {
                conn.rollback();
                System.out.println("Rolled back - no rows affected");
                AlertUtils.showError(owner, "Error", "Failed to save mission");
            }
        } catch (SQLException e) {
            // Rollback on error
            try {
                if (conn != null) conn.rollback();
                System.out.println("Transaction rolled back due to error");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(owner, "Database Error", "Error saving mission: " + e.getMessage());
        } finally {
            // Close resources
            DBUtil.closeResources(conn, stmt, generatedKeys);
            System.out.println("----- SAVE MISSION COMPLETED -----\n");
        }
    }

    // Test direct insertion
    public void testDirectInsert() {
        System.out.println("Testing direct mission insert");

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();

            // Try a simple insert
            String sql = "INSERT INTO missione (MatricolaVelivolo, DataMissione, NumeroVolo) " +
                    "VALUES ('100', CURDATE(), 7777)";

            int rows = stmt.executeUpdate(sql);
            System.out.println("Direct insert affected " + rows + " rows");

            // Query to verify
            ResultSet rs = stmt.executeQuery("SELECT * FROM missione WHERE NumeroVolo = 7777");
            if (rs.next()) {
                System.out.println("Mission found with ID: " + rs.getInt("ID"));
            } else {
                System.out.println("Mission NOT found after insert!");
            }

            // Verify DB we're connected to
            rs = stmt.executeQuery("SELECT DATABASE()");
            if (rs.next()) {
                System.out.println("Connected to database: " + rs.getString(1));
            }

        } catch (SQLException e) {
            System.err.println("Test insert error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, null);
        }
    }

    /**
     * Handles the "Clear All" button click.
     */
    @FXML
    protected void onClearAllClick(ActionEvent event) {
        clearForm();
    }

    /**
     * Clears all form fields and resets missile positions.
     */
    private void clearForm() {
        aircraftComboBox.setValue(null);
        flightNumberField.clear();
        missionDatePicker.setValue(LocalDate.now());
        timeStartField.clear();
        timeFinishField.clear();

        // Reset all missile positions
        for (String position : missilePositionsData.keySet()) {
            MissionWeaponConfig config = missilePositionsData.get(position);
            config.setLauncherId(null);
            config.setWeaponId(null);
        }

        // Hide weapon selection panel
        weaponSelectionPanel.setVisible(false);
        currentSelectedPosition = null;

        // Update UI
        for (Map.Entry<String, Pane> entry : missilePointsMap.entrySet()) {
            StackPane pane = (StackPane) entry.getValue();
            Rectangle rect = (Rectangle) pane.getChildren().get(0);

            rect.setFill(Color.TRANSPARENT);
            rect.setStroke(Color.GRAY);
            rect.setStrokeWidth(1.5);
        }

        // Clear weapon selection
        launcherComboBox.setValue("");
        weaponComboBox.setValue("");

        // Hide validation message
        validationMessageLabel.setVisible(false);

        // Reset save button color
        savePositionButton.setStyle("");
    }

    /**
     * Supporting method to update the selected positions map for the parent controller.
     */
    public void updateSelectedPositions(Map<String, Map<String, String>> selectedPositions) {
        // This method can be updated if needed by parent controllers
    }
}