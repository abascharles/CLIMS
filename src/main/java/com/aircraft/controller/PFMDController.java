package com.aircraft.controller;

import com.aircraft.dao.AircraftDAO;
import com.aircraft.dao.MissionDAO;
import com.aircraft.model.Aircraft;
import com.aircraft.model.Mission;
import com.aircraft.util.AlertUtils;
import com.aircraft.util.DBUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Post Flight Management Data (PFMD) module.
 * Handles recording and updating flight data after missions.
 * Updated to work with the new data structure where missile firing status is tracked in dichiarazione_missile_gui.
 */
public class PFMDController {
    @FXML
    private ComboBox<String> aircraftComboBox;

    @FXML
    private ComboBox<String> missionComboBox;

    @FXML
    private TextField gloadMaxField;

    @FXML
    private TextField gloadMinField;

    @FXML
    private TextField quotaMediaField;

    @FXML
    private StackPane aircraftContainer;

    @FXML
    private WebView aircraftSvgView;

    @FXML
    private AnchorPane missilePointsContainer;

    private final AircraftDAO aircraftDAO = new AircraftDAO();
    private final MissionDAO missionDAO = new MissionDAO();

    // Map to track missile positions and their status
    private final Map<String, MissileStatus> missileStatusMap = new HashMap<>();
    private final Map<String, Pane> missilePointsMap = new HashMap<>();

    // Current selected mission ID
    private Integer currentMissionId;
    private String currentAircraft;
    private Integer currentFlightNumber;

    /**
     * Enumeration for missile status
     */
    private enum MissileStatus {
        EMPTY,      // No missile loaded
        ONBOARD,    // Missile loaded and onboard
        FIRED       // Missile was fired
    }

    /**
     * Initializes the controller after its root element has been processed.
     * Sets up event handlers and initializes UI components.
     */
    @FXML
    public void initialize() {
        // Debug message
        System.out.println("PFMDController initializing");

        // Load aircraft data
        loadAircraftData();

        // Set up event handler for aircraft selection
        aircraftComboBox.setOnAction(event -> {
            String selectedAircraft = aircraftComboBox.getValue();
            if (selectedAircraft != null) {
                currentAircraft = selectedAircraft;
                loadMissions(selectedAircraft);
            }
        });

        // Initialize missile status map
        initializeMissileStatusMap();

        // Load SVG after the WebView is fully initialized
        Platform.runLater(this::loadAircraftSvg);

        // Make flight data fields read-only
        gloadMaxField.setEditable(false);
        gloadMinField.setEditable(false);
        quotaMediaField.setEditable(false);
    }

    /**
     * Initializes the missile status map with all positions.
     */
    private void initializeMissileStatusMap() {
        // Initialize with key positions P1 and P13 as per requirements
        missileStatusMap.put("P1", MissileStatus.EMPTY);
        missileStatusMap.put("P13", MissileStatus.EMPTY);

        // Add other positions for completeness
        for (int i = 2; i <= 12; i++) {
            missileStatusMap.put("P" + i, MissileStatus.EMPTY);
        }
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
                }
            });
        } catch (Exception e) {
            AlertUtils.showError(null, "Error", "Failed to load aircraft SVG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates missile point indicators at the predefined positions.
     * Focus on P1 and P13 as mentioned in requirements.
     */
    private void createMissilePoints() {
        // Define positions based on SVG coordinates
        // We'll focus on P1 and P13 as mentioned in requirements
        Map<String, double[]> positions = new HashMap<>();
        positions.put("P1", new double[]{90, 170});
        positions.put("P13", new double[]{460, 170});

        // Clear any existing missile points
        missilePointsContainer.getChildren().clear();
        missilePointsMap.clear();

        // Create visual indicators for each position
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            String position = entry.getKey();
            double[] coords = entry.getValue();

            // Create a missile point indicator (rectangle)
            Rectangle point = new Rectangle(24, 40);

            // Set initial style - transparent with light gray border
            point.setFill(Color.TRANSPARENT);
            point.setStroke(Color.LIGHTGRAY);
            point.setStrokeWidth(1);
            point.setOpacity(0.7);

            // Create position label
            Text label = new Text(position);
            label.setFill(Color.BLACK);
            label.getStyleClass().add("missile-point-label");

            // Combine in a StackPane
            StackPane missilePoint = new StackPane(point, label);
            missilePoint.setLayoutX(coords[0] - 12); // Center the rectangle
            missilePoint.setLayoutY(coords[1] - 20);
            missilePoint.setUserData(position); // Store position ID for click handler

            // Add click event
            missilePoint.setOnMouseClicked(this::onMissilePositionClick);

            // Store for later reference
            missilePointsMap.put(position, missilePoint);

            // Add to container
            missilePointsContainer.getChildren().add(missilePoint);
        }
    }

    /**
     * Loads aircraft data into the aircraft combo box.
     */
    private void loadAircraftData() {
        List<Aircraft> aircraftList = aircraftDAO.getAll();
        ObservableList<String> aircraftOptions = FXCollections.observableArrayList();

        for (Aircraft aircraft : aircraftList) {
            aircraftOptions.add(aircraft.getMatricolaVelivolo());
        }

        aircraftComboBox.setItems(aircraftOptions);
    }

    /**
     * Loads available missions for an aircraft.
     *
     * @param matricolaVelivolo The aircraft serial number
     */
    private void loadMissions(String matricolaVelivolo) {
        missionComboBox.getItems().clear();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();

            // Query missions from vista_gui_missione
            String query = "SELECT ID_Missione, NumeroVolo, DataMissione FROM vista_gui_missione " +
                    "WHERE MatricolaVelivolo = ? " +
                    "ORDER BY DataMissione DESC";

            statement = connection.prepareStatement(query);
            statement.setString(1, matricolaVelivolo);
            resultSet = statement.executeQuery();

            ObservableList<String> missionOptions = FXCollections.observableArrayList();

            while (resultSet.next()) {
                int id = resultSet.getInt("ID_Missione");
                int flightNumber = resultSet.getInt("NumeroVolo");
                Date missionDate = resultSet.getDate("DataMissione");

                // Format as "ID - Flight #X (YYYY-MM-DD)"
                String formattedDate = missionDate.toString();
                String displayText = id + " - Flight #" + flightNumber + " (" + formattedDate + ")";

                missionOptions.add(displayText);
            }

            // Set items
            missionComboBox.setItems(missionOptions);

        } catch (SQLException e) {
            Window owner = aircraftComboBox.getScene().getWindow();
            AlertUtils.showError(owner, "Database Error", "Failed to load missions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Handles the "Load Mission Data" button click.
     * Loads mission data and occupied positions.
     *
     * @param event The ActionEvent object
     */
    @FXML
    protected void onLoadMissionDataClick(ActionEvent event) {
        String selectedAircraft = aircraftComboBox.getValue();
        String selectedMissionStr = missionComboBox.getValue();

        if (selectedAircraft == null || selectedMissionStr == null) {
            Window owner = aircraftComboBox.getScene().getWindow();
            AlertUtils.showError(owner, "Selection Error", "Please select both an aircraft and a mission");
            return;
        }

        // Extract mission ID from the selection (format: "ID - Flight #XX...")
        String idPart = selectedMissionStr.split(" - ")[0];
        int missionId = Integer.parseInt(idPart);
        currentMissionId = missionId;
        currentAircraft = selectedAircraft;

        // Extract flight number from the selection (format: "ID - Flight #XX...")
        String flightPart = selectedMissionStr.split(" - ")[1];
        currentFlightNumber = Integer.parseInt(flightPart.substring(flightPart.indexOf("#") + 1).split(" ")[0]);

        // Load mission data from vista_gui_missione
        loadMissionData(missionId);

        // Load occupied positions from database
        loadOccupiedPositions(missionId);

        // Load existing firing declarations
        loadFiringDeclarations(missionId);

        // Display a message to the user
        Window owner = aircraftComboBox.getScene().getWindow();
        AlertUtils.showInformation(owner, "Mission Loaded",
                "Mission data loaded. Click on occupied positions to mark missiles as fired.");
    }

    /**
     * Loads mission data from vista_gui_missione view for the selected mission.
     *
     * @param missionId The mission ID
     */
    private void loadMissionData(int missionId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();

            // Query from vista_gui_missione
            String query = "SELECT GloadMin, GloadMax, QuotaMedia, PosizioniSparo FROM vista_gui_missione " +
                    "WHERE ID_Missione = ?";

            statement = connection.prepareStatement(query);
            statement.setInt(1, missionId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Populate flight data fields
                double gloadMin = resultSet.getDouble("GloadMin");
                double gloadMax = resultSet.getDouble("GloadMax");
                int quotaMedia = resultSet.getInt("QuotaMedia");
                String posizioniSparo = resultSet.getString("PosizioniSparo");

                gloadMinField.setText(String.valueOf(gloadMin));
                gloadMaxField.setText(String.valueOf(gloadMax));
                quotaMediaField.setText(String.valueOf(quotaMedia));

                // Debug info
                System.out.println("Loaded flight data for mission " + missionId +
                        " - GloadMin: " + gloadMin +
                        ", GloadMax: " + gloadMax +
                        ", QuotaMedia: " + quotaMedia +
                        ", PosizioniSparo: " + posizioniSparo);
            } else {
                // Clear fields if no data found
                gloadMinField.clear();
                gloadMaxField.clear();
                quotaMediaField.clear();
                System.out.println("No flight data found for mission " + missionId);
            }

        } catch (SQLException e) {
            System.err.println("Error loading flight data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Loads occupied positions based on launcher and missile values from the mission table.
     *
     * @param missionId The mission ID
     */
    private void loadOccupiedPositions(int missionId) {
        // Reset all positions to EMPTY
        for (String position : missileStatusMap.keySet()) {
            missileStatusMap.put(position, MissileStatus.EMPTY);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();

            // Query the missione table to check which positions are occupied
            String query = "SELECT " +
                    "PartNumberLanciatoreP1, PartNumberMissileP1, " +
                    "PartNumberLanciatoreP13, PartNumberMissileP13 " +
                    "FROM missione WHERE ID = ?";

            statement = connection.prepareStatement(query);
            statement.setInt(1, missionId);

            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Check P1
                String launcherP1 = resultSet.getString("PartNumberLanciatoreP1");
                String missileP1 = resultSet.getString("PartNumberMissileP1");

                if (launcherP1 != null && !launcherP1.isEmpty() && missileP1 != null && !missileP1.isEmpty()) {
                    missileStatusMap.put("P1", MissileStatus.ONBOARD);
                    System.out.println("Position P1 is occupied with launcher " + launcherP1 + " and missile " + missileP1);
                }

                // Check P13
                String launcherP13 = resultSet.getString("PartNumberLanciatoreP13");
                String missileP13 = resultSet.getString("PartNumberMissileP13");

                if (launcherP13 != null && !launcherP13.isEmpty() && missileP13 != null && !missileP13.isEmpty()) {
                    missileStatusMap.put("P13", MissileStatus.ONBOARD);
                    System.out.println("Position P13 is occupied with launcher " + launcherP13 + " and missile " + missileP13);
                }
            }

            // Update UI to reflect loaded status
            updateMissilePositionStyles();

        } catch (SQLException e) {
            System.err.println("Error loading occupied positions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Loads existing firing declarations from dichiarazione_missile_gui.
     *
     * @param missionId The mission ID
     */
    private void loadFiringDeclarations(int missionId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();

            // Query for firing declarations
            String query = "SELECT PosizioneVelivolo, Missile_Sparato FROM dichiarazione_missile_gui " +
                    "WHERE ID_Missione = ?";

            statement = connection.prepareStatement(query);
            statement.setInt(1, missionId);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String position = resultSet.getString("PosizioneVelivolo");
                String missileStatus = resultSet.getString("Missile_Sparato");

                System.out.println("Declaration for position " + position + ": " + missileStatus);

                // Set position as FIRED if Missile_Sparato is "SI"
                if (position != null && !position.isEmpty() && missileStatusMap.containsKey(position)) {
                    if ("SI".equals(missileStatus)) {
                        missileStatusMap.put(position, MissileStatus.FIRED);
                        System.out.println("Position " + position + " marked as FIRED");
                    } else {
                        // Ensure it's marked as ONBOARD if not fired
                        if (missileStatusMap.get(position) != MissileStatus.EMPTY) {
                            missileStatusMap.put(position, MissileStatus.ONBOARD);
                            System.out.println("Position " + position + " marked as ONBOARD");
                        }
                    }
                }
            }

            // Update UI to reflect loaded status
            updateMissilePositionStyles();

        } catch (SQLException e) {
            System.err.println("Error loading firing declarations: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Updates the styles of all missile positions based on their current status.
     */
    private void updateMissilePositionStyles() {
        for (Map.Entry<String, Pane> entry : missilePointsMap.entrySet()) {
            String position = entry.getKey();
            Pane pane = entry.getValue();
            updateMissilePositionStyle(pane, position);
        }
    }

    /**
     * Updates the style of a single missile position pane.
     * This method applies styles directly to the Rectangle rather than using CSS classes.
     *
     * @param pane The Pane object
     * @param position The position identifier
     */
    private void updateMissilePositionStyle(Pane pane, String position) {
        if (pane == null) return;

        // Get status for this position
        MissileStatus status = missileStatusMap.get(position);
        if (status == null) status = MissileStatus.EMPTY;

        // Get the rectangle from the StackPane
        Rectangle rect = (Rectangle) ((StackPane) pane).getChildren().get(0);
        Text label = (Text) ((StackPane) pane).getChildren().get(1);

        // Apply styles directly to the rectangle based on status
        switch (status) {
            case EMPTY:
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.LIGHTGRAY);
                rect.setStrokeWidth(1);
                rect.setOpacity(0.7);
                label.setFill(Color.BLACK);
                break;
            case ONBOARD:
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.GREEN);
                rect.setStrokeWidth(2);
                rect.setOpacity(0.9);
                label.setFill(Color.BLACK);
                break;
            case FIRED:
                rect.setFill(Color.RED);
                rect.setStroke(Color.DARKRED);
                rect.setStrokeWidth(2);
                rect.setOpacity(0.9);
                label.setFill(Color.WHITE);
                break;
        }
    }

    /**
     * Handles clicks on missile position panes.
     * Toggles the status of the clicked missile position between ONBOARD and FIRED.
     *
     * @param event The MouseEvent object
     */
    @FXML
    protected void onMissilePositionClick(MouseEvent event) {
        if (currentMissionId == null) {
            Window owner = aircraftComboBox.getScene().getWindow();
            AlertUtils.showWarning(owner, "No Mission Selected", "Please load a mission first");
            return;
        }

        StackPane clickedPane = (StackPane) event.getSource();
        String position = (String) clickedPane.getUserData();

        // Get current status
        MissileStatus status = missileStatusMap.get(position);

        // Only allow clicking if the missile is loaded (ONBOARD or FIRED)
        if (status == MissileStatus.ONBOARD) {
            // Change to FIRED
            missileStatusMap.put(position, MissileStatus.FIRED);
            updateMissilePositionStyle(clickedPane, position);

            // Save the firing declaration to database
            saveFiringDeclaration(position, true);

        } else if (status == MissileStatus.FIRED) {
            // Allow changing back from FIRED to ONBOARD
            missileStatusMap.put(position, MissileStatus.ONBOARD);
            updateMissilePositionStyle(clickedPane, position);

            // Update the firing declaration in database
            saveFiringDeclaration(position, false);

        } else if (status == MissileStatus.EMPTY) {
            // Can't click on empty positions
            Window owner = clickedPane.getScene().getWindow();
            AlertUtils.showWarning(owner, "No Missile", "No missile loaded at position " + position);
        }
    }

    /**
     * Saves or updates a firing declaration in the dichiarazione_missile_gui table.
     *
     * @param position The position identifier
     * @param fired Whether the missile was fired (true) or not (false)
     */
    private void saveFiringDeclaration(String position, boolean fired) {
        Connection connection = null;
        PreparedStatement updateStmt = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();

            // Update the dichiarazione_missile_gui table
            String updateQuery = "UPDATE dichiarazione_missile_gui " +
                    "SET Missile_Sparato = ? " +
                    "WHERE ID_Missione = ? AND PosizioneVelivolo = ?";

            updateStmt = connection.prepareStatement(updateQuery);
            updateStmt.setString(1, fired ? "SI" : "NO");
            updateStmt.setInt(2, currentMissionId);
            updateStmt.setString(3, position);

            int rowsUpdated = updateStmt.executeUpdate();
            System.out.println("Updated " + rowsUpdated + " rows for position " + position + " - fired: " + (fired ? "SI" : "NO"));

            // Inform user
            Window owner = aircraftContainer.getScene().getWindow();
            String message = "Position " + position + " marked as " + (fired ? "FIRED" : "ONBOARD");
            AlertUtils.showInformation(owner, "Status Updated", message);

        } catch (SQLException e) {
            System.err.println("Error saving firing declaration: " + e.getMessage());
            e.printStackTrace();
            Window owner = aircraftContainer.getScene().getWindow();
            AlertUtils.showError(owner, "Database Error", "Failed to save firing declaration: " + e.getMessage());
        } finally {
            DBUtil.closeResources(connection, updateStmt, resultSet);
        }
    }

    /**
     * Handles the "Save Data" button click.
     * This is simplified to just save missile firing declarations.
     *
     * @param event The ActionEvent object
     */
    @FXML
    protected void onSaveDataClick(ActionEvent event) {
        Window owner = gloadMaxField.getScene().getWindow();

        if (currentMissionId == null) {
            AlertUtils.showError(owner, "No Mission Selected", "Please load a mission first");
            return;
        }

        // No flight data to save as they are read-only from vista_gui_missione
        // All firing declarations are saved directly when clicking on positions

        AlertUtils.showInformation(owner, "Data Saved",
                "All missile firing declarations have been saved.");
    }

    /**
     * Handles the "Clear Form" button click.
     * Clears form fields and resets missile status.
     *
     * @param event The ActionEvent object
     */
    @FXML
    protected void onClearFormClick(ActionEvent event) {
        // Clear form fields
        gloadMaxField.clear();
        gloadMinField.clear();
        quotaMediaField.clear();

        // Reset missile status
        for (String position : missileStatusMap.keySet()) {
            missileStatusMap.put(position, MissileStatus.EMPTY);
        }

        // Update UI
        updateMissilePositionStyles();

        // Reset current mission ID
        currentMissionId = null;
        currentAircraft = null;
        currentFlightNumber = null;
    }
}