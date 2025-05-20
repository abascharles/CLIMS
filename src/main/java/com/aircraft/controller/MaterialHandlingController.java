package com.aircraft.controller;

import com.aircraft.dao.MovementHistoryDAO;
import com.aircraft.model.MovementHistory;
import com.aircraft.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Window;

import java.util.List;

/**
 * Controller for the Material Handling screen.
 * Handles viewing launcher and missile embarkation/disembarkation history.
 */
public class MaterialHandlingController {

    @FXML
    private TextField partNumberSearchField;

    @FXML
    private Button searchButton;

    @FXML
    private Label itemTypeLabel;

    @FXML
    private TableView<MovementHistory> historyTable;

    @FXML
    private TableColumn<MovementHistory, String> partNumberColumn;

    @FXML
    private TableColumn<MovementHistory, String> itemNameColumn;

    @FXML
    private TableColumn<MovementHistory, String> itemTypeColumn;

    @FXML
    private TableColumn<MovementHistory, String> serialNumberColumn;

    @FXML
    private TableColumn<MovementHistory, String> dateColumn;

    @FXML
    private TableColumn<MovementHistory, String> actionTypeColumn;

    @FXML
    private TableColumn<MovementHistory, String> locationColumn;

    @FXML
    private TableColumn<MovementHistory, String> aircraftIdColumn;

    private final MovementHistoryDAO movementHistoryDAO = new MovementHistoryDAO();
    private ObservableList<MovementHistory> historyList = FXCollections.observableArrayList();

    /**
     * Initializes the controller after its root element has been processed.
     * Sets up event handlers and initializes UI components.
     */
    @FXML
    public void initialize() {
        // Set up table columns
        partNumberColumn.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        itemTypeColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        serialNumberColumn.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        actionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        aircraftIdColumn.setCellValueFactory(new PropertyValueFactory<>("aircraftId"));

        // Clear the table initially
        historyTable.setItems(historyList);

        // Set up enter key press on the part number field to trigger search
        partNumberSearchField.setOnAction(this::onSearchButtonClick);

        // Debug message
        System.out.println("MaterialHandlingController initialized");
    }

    /**
     * Handles the "Search" button click.
     * Retrieves and displays movement history for the specified part number.
     *
     * @param event The ActionEvent object
     */
    @FXML
    protected void onSearchButtonClick(ActionEvent event) {
        Window owner = searchButton.getScene().getWindow();

        // Get part number from search field
        String partNumber = partNumberSearchField.getText().trim();

        // Validate input
        if (partNumber.isEmpty()) {
            AlertUtils.showError(owner, "Validation Error", "Please enter a Part Number");
            return;
        }

        System.out.println("Searching for part number: " + partNumber);

        // Get item type and name
        String itemType = movementHistoryDAO.getItemTypeByPartNumber(partNumber);
        String itemName = movementHistoryDAO.getItemNameByPartNumber(partNumber);

        System.out.println("Item type: " + itemType + ", Item name: " + itemName);

        if (itemType != null) {
            itemTypeLabel.setText("Item Type: " + itemType + (itemName != null ? " - " + itemName : ""));

            // Retrieve movement history
            List<MovementHistory> history;

            // Use the direct method to get all history for this part number
            history = movementHistoryDAO.getByPartNumber(partNumber);

            System.out.println("Found " + history.size() + " movements for part number: " + partNumber);

            if (history.isEmpty()) {
                AlertUtils.showInformation(owner, "No Records Found",
                        "No movement history found for Part Number: " + partNumber);
                historyList.clear();
                historyTable.setItems(historyList);
            } else {
                // Update table with history data
                historyList.clear();
                historyList.addAll(history);
                historyTable.setItems(historyList);

                // Force table refresh
                historyTable.refresh();
            }
        } else {
            itemTypeLabel.setText("Unknown Part Number");
            historyList.clear();
            AlertUtils.showInformation(owner, "Unknown Part Number",
                    "No information found for Part Number: " + partNumber);
        }
    }
}