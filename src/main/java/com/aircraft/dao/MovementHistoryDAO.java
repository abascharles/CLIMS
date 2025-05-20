package com.aircraft.dao;

import com.aircraft.model.MovementHistory;
import com.aircraft.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for MovementHistory operations.
 * Updated to work with the latest database schema that uses part numbers.
 */
public class MovementHistoryDAO {

    /**
     * Retrieves all movement history records for a specific part number.
     *
     * @param partNumber The part number to search for
     * @return List of MovementHistory objects
     */
    public List<MovementHistory> getByPartNumber(String partNumber) {
        List<MovementHistory> historyList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();

            // Debug - Print query for diagnostics
            System.out.println("Querying view for part number: " + partNumber);

            String query = "SELECT PartNumber, Nomenclatura, DataInstallazione, DataRimozione, " +
                    "PosizioneVelivolo, MatricolaVelivolo, TipoComponente " +
                    "FROM views_material_handling " +
                    "WHERE PartNumber = ? " +
                    "ORDER BY DataInstallazione DESC";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, partNumber);

            rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                MovementHistory history = new MovementHistory();
                history.setPartNumber(rs.getString("PartNumber"));
                history.setItemName(rs.getString("Nomenclatura"));
                history.setItemType(rs.getString("TipoComponente"));

                // Serial Number is no longer used, set to "N/A"
                history.setSerialNumber("N/A");

                // Handle date conversion from SQL Date to LocalDate
                java.sql.Date installDate = rs.getDate("DataInstallazione");
                if (installDate != null) {
                    history.setDate(installDate.toLocalDate());

                    // Debug - print the date
                    System.out.println("Found history: " + installDate + " for " + partNumber + " at position " + rs.getString("PosizioneVelivolo"));
                }

                // Determine action type based on dates - if DataRimozione is null, it's still installed
                java.sql.Date removalDate = rs.getDate("DataRimozione");
                String actionType = (removalDate == null) ? "Installation" : "Removal";
                history.setActionType(actionType);

                history.setLocation(rs.getString("PosizioneVelivolo"));
                history.setAircraftId(rs.getString("MatricolaVelivolo"));

                historyList.add(history);
            }

            System.out.println("Total records found for " + partNumber + ": " + count);

        } catch (SQLException e) {
            System.err.println("Error in getByPartNumber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return historyList;
    }

    /**
     * Retrieves launcher movement history records for a specific part number.
     * Uses the views_material_handling view with TipoComponente = 'Lanciatore'.
     *
     * @param partNumber The part number to search for
     * @return List of MovementHistory objects for launchers
     */
    public List<MovementHistory> getLauncherHistoryByPartNumber(String partNumber) {
        List<MovementHistory> historyList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String query = "SELECT PartNumber, Nomenclatura, DataInstallazione, DataRimozione, " +
                    "PosizioneVelivolo, MatricolaVelivolo, TipoComponente " +
                    "FROM views_material_handling " +
                    "WHERE PartNumber = ? AND TipoComponente = 'Lanciatore' " +
                    "ORDER BY DataInstallazione DESC";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, partNumber);

            rs = stmt.executeQuery();
            while (rs.next()) {
                MovementHistory history = new MovementHistory();
                history.setPartNumber(rs.getString("PartNumber"));
                history.setItemName(rs.getString("Nomenclatura"));
                history.setItemType(rs.getString("TipoComponente"));

                // Set serial number to "N/A" as it's no longer used
                history.setSerialNumber("N/A");

                // Handle date conversion from SQL Date to LocalDate
                java.sql.Date installDate = rs.getDate("DataInstallazione");
                if (installDate != null) {
                    history.setDate(installDate.toLocalDate());
                }

                // Determine action type based on dates
                java.sql.Date removalDate = rs.getDate("DataRimozione");
                String actionType = (removalDate == null) ? "Installation" : "Removal";
                history.setActionType(actionType);

                history.setLocation(rs.getString("PosizioneVelivolo"));
                history.setAircraftId(rs.getString("MatricolaVelivolo"));

                historyList.add(history);
            }
        } catch (SQLException e) {
            System.err.println("Error in getLauncherHistoryByPartNumber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return historyList;
    }

    /**
     * Retrieves missile/load movement history records for a specific part number.
     * Uses the views_material_handling view with TipoComponente = 'Carico'.
     *
     * @param partNumber The part number to search for
     * @return List of MovementHistory objects for missiles/loads
     */
    public List<MovementHistory> getLoadHistoryByPartNumber(String partNumber) {
        List<MovementHistory> historyList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String query = "SELECT PartNumber, Nomenclatura, DataInstallazione, DataRimozione, " +
                    "PosizioneVelivolo, MatricolaVelivolo, TipoComponente " +
                    "FROM views_material_handling " +
                    "WHERE PartNumber = ? AND TipoComponente = 'Carico' " +
                    "ORDER BY DataInstallazione DESC";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, partNumber);

            rs = stmt.executeQuery();
            while (rs.next()) {
                MovementHistory history = new MovementHistory();
                history.setPartNumber(rs.getString("PartNumber"));
                history.setItemName(rs.getString("Nomenclatura"));
                history.setItemType("Missile"); // Use "Missile" instead of "Carico" for UI consistency

                // Serial number set to "N/A" as it's no longer used
                history.setSerialNumber("N/A");

                // Handle date conversion from SQL Date to LocalDate
                java.sql.Date installDate = rs.getDate("DataInstallazione");
                if (installDate != null) {
                    history.setDate(installDate.toLocalDate());
                }

                // Determine action type based on dates
                java.sql.Date removalDate = rs.getDate("DataRimozione");
                String actionType = (removalDate == null) ? "Installation" : "Removal";
                history.setActionType(actionType);

                history.setLocation(rs.getString("PosizioneVelivolo"));
                history.setAircraftId(rs.getString("MatricolaVelivolo"));

                historyList.add(history);
            }
        } catch (SQLException e) {
            System.err.println("Error in getLoadHistoryByPartNumber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return historyList;
    }

    /**
     * Determines if a part number exists in the system and what type it is.
     * Uses the views_material_handling view.
     *
     * @param partNumber The part number to check
     * @return The item type ("Launcher", "Missile") or null if not found
     */
    public String getItemTypeByPartNumber(String partNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String query = "SELECT DISTINCT TipoComponente FROM views_material_handling " +
                    "WHERE PartNumber = ? LIMIT 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, partNumber);

            rs = stmt.executeQuery();
            if (rs.next()) {
                String dbType = rs.getString("TipoComponente");
                // Convert to the types used in the UI
                if ("Lanciatore".equals(dbType) || "Launcher".equals(dbType)) {
                    return "Launcher";
                } else if ("Carico".equals(dbType)) {
                    return "Missile";
                }
                return dbType; // Return the type as is if it doesn't match known types
            }
        } catch (SQLException e) {
            System.err.println("Error in getItemTypeByPartNumber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return null;
    }

    /**
     * Gets the item name (nomenclatura) for a part number.
     * Uses the views_material_handling view.
     *
     * @param partNumber The part number to look up
     * @return The item name or null if not found
     */
    public String getItemNameByPartNumber(String partNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            String query = "SELECT DISTINCT Nomenclatura FROM views_material_handling " +
                    "WHERE PartNumber = ? LIMIT 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, partNumber);

            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("Nomenclatura");
            }
        } catch (SQLException e) {
            System.err.println("Error in getItemNameByPartNumber: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return null;
    }
}