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
            return launcherId != null ? launcherId : "";
        }

        public void setLauncherId(String launcherId) {
            this.launcherId = launcherId;
        }

        public String getWeaponId() {
            return weaponId != null ? weaponId : "";
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
        if (dropdownValue != null && dropdownValue.contains("(") && dropdownValue.contains(")")) {
            int start = dropdownValue.lastIndexOf("(") + 1;
            int end = dropdownValue.lastIndexOf(")");
            if (start < end) {
                return dropdownValue.substring(start, end);
            }
        }
        return dropdownValue; // Return as is if no parentheses
    }


    /**
     * Validates required fields for mission creation.
     *
     * @param owner The owner window for showing error alerts
     * @return true if all required fields are valid, false otherwise
     */
    private boolean validateRequiredFields(Window owner) {
        // Get required field values
        Aircraft selectedAircraft = aircraftComboBox.getValue();
        LocalDate missionDate = missionDatePicker.getValue();
        String flightNumber = flightNumberField.getText();

        // CRITICAL DEBUG - Show exactly what's selected
        System.out.println("Selected aircraft: " + (selectedAircraft != null ?
                selectedAircraft.getMatricolaVelivolo() + " (class: " + selectedAircraft.getClass().getName() + ")" : "NULL"));
        System.out.println("Mission date: " + missionDate);
        System.out.println("Flight number: " + flightNumber);

        if (selectedAircraft == null) {
            AlertUtils.showError(owner, "Validation Error", "Please select an aircraft");
            return false;
        }

        if (missionDate == null) {
            AlertUtils.showError(owner, "Validation Error", "Please select a mission date");
            return false;
        }

        if (flightNumber == null || flightNumber.isEmpty()) {
            AlertUtils.showError(owner, "Validation Error", "Please enter a flight number");
            return false;
        }

        // Check if flight number contains only digits
        if (!flightNumber.matches("\\d+")) {
            AlertUtils.showError(owner, "Validation Error", "Flight number must contain only digits");
            return false;
        }

        return true;
    }
    /**
     * Handles the "Save All" button click - saves the mission with launcher and missile data.
     * Includes improved validation, error handling, and transaction management.
     *
     * @param event The ActionEvent object
     */
    @FXML
    protected void onSaveAllClick(ActionEvent event) {
        Window owner = ((Node) event.getSource()).getScene().getWindow();
        System.out.println("\n----- SAVING MISSION USING DAO -----");

        // Test database connection and triggers first
        testDirectDatabaseAccess();

        // Validate input fields
        if (!validateRequiredFields(owner)) {
            return;
        }
        if (!validatePartNumbers(owner)) {
            return;
        }

        // Get basic mission data
        Aircraft selectedAircraft = aircraftComboBox.getValue();
        LocalDate missionDate = missionDatePicker.getValue();
        int flightNum = Integer.parseInt(flightNumberField.getText());

        // Get launcher and missile data with detailed logging
        String launcherP1 = "";
        String missileP1 = "";
        String launcherP13 = "";
        String missileP13 = "";

        // Extract configuration data for P1 and P13
        if (missilePositionsData.containsKey("P1")) {
            MissionWeaponConfig p1Config = missilePositionsData.get("P1");
            if (p1Config != null && p1Config.hasLauncher()) {
                launcherP1 = p1Config.getLauncherId();
                missileP1 = p1Config.hasWeapon() ? p1Config.getWeaponId() : "";
                System.out.println("P1 configuration: Launcher=" + launcherP1 + ", Missile=" + missileP1);
            }
        }

        if (missilePositionsData.containsKey("P13")) {
            MissionWeaponConfig p13Config = missilePositionsData.get("P13");
            if (p13Config != null && p13Config.hasLauncher()) {
                launcherP13 = p13Config.getLauncherId();
                missileP13 = p13Config.hasWeapon() ? p13Config.getWeaponId() : "";
                System.out.println("P13 configuration: Launcher=" + launcherP13 + ", Missile=" + missileP13);
            }
        }

        try {
            // Create Mission object with all fields
            Mission mission = new Mission();
            mission.setMatricolaVelivolo(selectedAircraft.getMatricolaVelivolo());
            mission.setDataMissione(java.sql.Date.valueOf(missionDate));
            mission.setNumeroVolo(flightNum);

            // Set times with proper validation
            if (!timeStartField.getText().isEmpty() && validateTimeField(timeStartField)) {
                LocalTime localTimeStart = LocalTime.parse(timeStartField.getText(), timeFormatter);
                mission.setOraPartenza(Time.valueOf(localTimeStart));
            } else {
                mission.setOraPartenza(null);
            }

            if (!timeFinishField.getText().isEmpty() && validateTimeField(timeFinishField)) {
                LocalTime localTimeFinish = LocalTime.parse(timeFinishField.getText(), timeFormatter);
                mission.setOraArrivo(Time.valueOf(localTimeFinish));
            } else {
                mission.setOraArrivo(null);
            }

            // Set launcher and missile values
            mission.setPartNumberLanciatoreP1(launcherP1.isEmpty() ? null : launcherP1);
            mission.setPartNumberMissileP1(missileP1.isEmpty() ? null : missileP1);
            mission.setPartNumberLanciatoreP13(launcherP13.isEmpty() ? null : launcherP13);
            mission.setPartNumberMissileP13(missileP13.isEmpty() ? null : missileP13);

            // Save using DAO
            System.out.println("Calling MissionDAO.insertAndGetId...");
            int missionId = missionDAO.insertAndGetId(mission);
            System.out.println("DAO returned missionId: " + missionId);

            if (missionId > 0) {
                System.out.println("Mission saved successfully with ID: " + missionId);

                // Wait a moment for triggers to execute
                Thread.sleep(500);

                // Run diagnostics
                runPostInsertDiagnostics(missionId);

                // Check if dichiarazione_missile_gui entries were created
                boolean entriesCreated = checkDichiarazioniCreated(missionId);

                if (!entriesCreated) {
                    System.out.println("Triggers failed to create missile declarations, inserting manually");

                    // Insert manually for P1
                    if (!missileP1.isEmpty()) {
                        insertMissileDichiarazioniManually(missionId, "P1", missileP1);
                    }

                    // Insert manually for P13
                    if (!missileP13.isEmpty()) {
                        insertMissileDichiarazioniManually(missionId, "P13", missileP13);
                    }
                }

                // Check error_log for any trigger issues
                checkErrorLog(missionId);

                // Count configured positions
                int positionsCount = 0;
                if (!launcherP1.isEmpty()) positionsCount++;
                if (!launcherP13.isEmpty()) positionsCount++;

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
                System.out.println("Failed to save mission - DAO returned ID <= 0");
                AlertUtils.showError(owner, "Error", "Failed to save mission. Check console for details.");
            }
        } catch (Exception e) {
            System.err.println("Error saving mission: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(owner, "Database Error", "Error saving mission: " + e.getMessage());
        }

        System.out.println("----- SAVE MISSION COMPLETED -----\n");
    }
    /**
     * Checks if dichiarazione_missile_gui entries were created for a mission
     */
    private boolean checkDichiarazioniCreated(int missionId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM dichiarazione_missile_gui WHERE ID_Missione = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, missionId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Found " + count + " dichiarazione entries for mission " + missionId);
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error checking dichiarazioni: " + e.getMessage());
            return false;
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }
    }

    /**
     * Performs comprehensive validation after a mission insert
     */
    private void runPostInsertDiagnostics(int missionId) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();

            System.out.println("\n--- POST-INSERT DIAGNOSTICS FOR MISSION ID " + missionId + " ---");

            // Check mission record
            rs = stmt.executeQuery("SELECT * FROM missione WHERE ID = " + missionId);
            if (rs.next()) {
                System.out.println("Mission record exists with correct ID");
                System.out.println("- MatricolaVelivolo: " + rs.getString("MatricolaVelivolo"));
                System.out.println("- P1 Launcher: " + rs.getString("PartNumberLanciatoreP1"));
                System.out.println("- P1 Missile: " + rs.getString("PartNumberMissileP1"));
                System.out.println("- P13 Launcher: " + rs.getString("PartNumberLanciatoreP13"));
                System.out.println("- P13 Missile: " + rs.getString("PartNumberMissileP13"));
            } else {
                System.out.println("CRITICAL ERROR: Mission record not found after insert!");
            }
            rs.close();

            // Check dichiarazione_missile_gui
            rs = stmt.executeQuery("SELECT * FROM dichiarazione_missile_gui WHERE ID_Missione = " + missionId);
            System.out.println("\nDichiarazione_missile_gui entries:");
            boolean hasDichiarazioni = false;
            while (rs.next()) {
                hasDichiarazioni = true;
                System.out.println("- ID: " + rs.getInt("ID"));
                System.out.println("  Position: " + rs.getString("PosizioneVelivolo"));
                System.out.println("  Status: " + rs.getString("Missile_Sparato"));
            }
            if (!hasDichiarazioni) {
                System.out.println("WARNING: No dichiarazione_missile_gui entries found!");
            }
            rs.close();

            // Check storico_carico and storico_lanciatore entries
            System.out.println("\nStorico entries:");
            rs = stmt.executeQuery("SELECT * FROM storico_carico WHERE DataImbarco = '" +
                    java.sql.Date.valueOf(missionDatePicker.getValue()) + "' AND " +
                    "MatricolaVelivolo = '" + aircraftComboBox.getValue().getMatricolaVelivolo() + "'");
            boolean hasStorico = false;
            while (rs.next()) {
                hasStorico = true;
                System.out.println("- Carico ID: " + rs.getInt("ID"));
                System.out.println("  Position: " + rs.getString("PosizioneVelivolo"));
                System.out.println("  PartNumber: " + rs.getString("PartNumber"));
            }

            rs = stmt.executeQuery("SELECT * FROM storico_lanciatore WHERE DataInstallazione = '" +
                    java.sql.Date.valueOf(missionDatePicker.getValue()) + "' AND " +
                    "MatricolaVelivolo = '" + aircraftComboBox.getValue().getMatricolaVelivolo() + "'");
            while (rs.next()) {
                hasStorico = true;
                System.out.println("- Lanciatore ID: " + rs.getInt("ID"));
                System.out.println("  Position: " + rs.getString("PosizioneVelivolo"));
                System.out.println("  PartNumber: " + rs.getString("PartNumber"));
            }

            if (!hasStorico) {
                System.out.println("WARNING: No storico entries found!");
            }

            System.out.println("\n--- END DIAGNOSTICS ---\n");

        } catch (SQLException e) {
            System.err.println("Error in post-insert diagnostics: " + e.getMessage());
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }
    }
    /**
     * Check error_log for any trigger issues
     */
    private void checkErrorLog(int missionId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String query = "SELECT * FROM error_log WHERE error_message LIKE ? ORDER BY id DESC LIMIT 10";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + missionId + "%");

            rs = stmt.executeQuery();
            System.out.println("Checking error log for mission ID " + missionId + ":");

            while (rs.next()) {
                System.out.println("  - [" + rs.getString("error_source") + "] " + rs.getString("error_message"));
            }
        } catch (SQLException e) {
            System.err.println("Error checking error log: " + e.getMessage());
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }
    }

    /**
     * Debug method to test direct database connection
     */
    private void testDirectDatabaseAccess() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();

            // Show database name
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT DATABASE()");
            if (rs.next()) {
                System.out.println("Connected to database: " + rs.getString(1));
            }
            rs.close();

            // Check table structure
            rs = stmt.executeQuery("DESCRIBE missione");
            System.out.println("Missione table structure:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("Field") + ": " + rs.getString("Type"));
            }
            rs.close();

            // Check triggers
            rs = stmt.executeQuery("SHOW TRIGGERS WHERE `Table` = 'missione'");
            System.out.println("Database triggers on missione table:");
            boolean hasTriggers = false;
            while (rs.next()) {
                hasTriggers = true;
                System.out.println("- " + rs.getString("Trigger") + " (" + rs.getString("Timing") + " " +
                        rs.getString("Event") + ")");
            }

            if (!hasTriggers) {
                System.out.println("WARNING: No triggers found on missione table!");
            }

        } catch (SQLException e) {
            System.err.println("Database test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }
    }
    /**
     * Manually inserts missile declarations if the trigger fails
     */
    private void insertMissileDichiarazioniManually(int missionId, String position, String missilePartNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBUtil.getConnection();
            String sql = "INSERT INTO dichiarazione_missile_gui (ID_Missione, PosizioneVelivolo, Missile_Sparato) " +
                    "VALUES (?, ?, 'NO')";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, missionId);
            stmt.setString(2, position);

            int rows = stmt.executeUpdate();
            System.out.println("Manually inserted " + rows + " missile declaration entries");
        } catch (SQLException e) {
            System.err.println("Error manually inserting missile declaration: " + e.getMessage());
        } finally {
            DBUtil.closeResources(conn, stmt, null);
        }
    }

    /**
     * Validates that the launcher and missile part numbers exist in the database.
     * @return true if all part numbers are valid or empty, false otherwise
     */
    private boolean validatePartNumbers(Window owner) {
        // Check P1 launcher if specified
        if (missilePositionsData.containsKey("P1")) {
            MissionWeaponConfig p1Config = missilePositionsData.get("P1");
            if (p1Config != null && p1Config.hasLauncher()) {
                String launcherId = p1Config.getLauncherId();
                if (!launcherDAO.exists(launcherId)) {
                    AlertUtils.showError(owner, "Invalid Part Number",
                            "Launcher part number '" + launcherId + "' for position P1 does not exist in the database");
                    return false;
                }

                // Check missile if specified
                if (p1Config.hasWeapon()) {
                    String missileId = p1Config.getWeaponId();
                    if (!weaponDAO.exists(missileId)) {
                        AlertUtils.showError(owner, "Invalid Part Number",
                                "Missile part number '" + missileId + "' for position P1 does not exist in the database");
                        return false;
                    }
                }
            }
        }

        // Check P13 launcher if specified
        if (missilePositionsData.containsKey("P13")) {
            MissionWeaponConfig p13Config = missilePositionsData.get("P13");
            if (p13Config != null && p13Config.hasLauncher()) {
                String launcherId = p13Config.getLauncherId();
                if (!launcherDAO.exists(launcherId)) {
                    AlertUtils.showError(owner, "Invalid Part Number",
                            "Launcher part number '" + launcherId + "' for position P13 does not exist in the database");
                    return false;
                }

                // Check missile if specified
                if (p13Config.hasWeapon()) {
                    String missileId = p13Config.getWeaponId();
                    if (!weaponDAO.exists(missileId)) {
                        AlertUtils.showError(owner, "Invalid Part Number",
                                "Missile part number '" + missileId + "' for position P13 does not exist in the database");
                        return false;
                    }
                }
            }
        }

        return true;
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

    private void testDatabaseConnection() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();

            // Test query
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM matricola_velivolo");
            if (rs.next()) {
                System.out.println("Database connection success - Aircraft count: " + rs.getInt("count"));
            }

            // Test available aircraft
            rs = stmt.executeQuery("SELECT MatricolaVelivolo FROM matricola_velivolo");
            System.out.println("Available aircraft in database:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("MatricolaVelivolo"));
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
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