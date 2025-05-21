package com.aircraft.dao;

import com.aircraft.model.Mission;
import com.aircraft.model.WeaponStatus;
import com.aircraft.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Mission-related database operations.
 * Provides methods for CRUD operations on missions.
 */
public class MissionDAO {

    /**
     * Inserts a new mission into the database.
     *
     * @param mission The Mission object to insert
     * @return true if insertion was successful, false otherwise
     */
    public boolean insert(Mission mission) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection();

            // Updated SQL query to include launcher and missile fields
            String sql = "INSERT INTO missione (MatricolaVelivolo, DataMissione, NumeroVolo, OraPartenza, OraArrivo, " +
                    "PartNumberLanciatoreP1, PartNumberMissileP1, PartNumberLanciatoreP13, PartNumberMissileP13) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, mission.getMatricolaVelivolo());
            stmt.setDate(2, mission.getDataMissione());
            stmt.setInt(3, mission.getNumeroVolo());
            stmt.setTime(4, mission.getOraPartenza());
            stmt.setTime(5, mission.getOraArrivo());

            // Add the new parameters
            stmt.setString(6, mission.getPartNumberLanciatoreP1());
            stmt.setString(7, mission.getPartNumberMissileP1());
            stmt.setString(8, mission.getPartNumberLanciatoreP13());
            stmt.setString(9, mission.getPartNumberMissileP13());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Get the generated ID
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    mission.setId(generatedKeys.getInt(1));
                    success = true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting mission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, generatedKeys);
        }

        return success;
    }

    /**
     * Updates an existing mission in the database.
     *
     * @param mission The Mission object to update
     * @return true if update was successful, false otherwise
     */
    public boolean update(Mission mission) {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection();

            // Updated SQL query to include launcher and missile fields
            String sql = "UPDATE missione SET MatricolaVelivolo = ?, DataMissione = ?, NumeroVolo = ?, " +
                    "OraPartenza = ?, OraArrivo = ?, PartNumberLanciatoreP1 = ?, PartNumberMissileP1 = ?, " +
                    "PartNumberLanciatoreP13 = ?, PartNumberMissileP13 = ? WHERE ID = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, mission.getMatricolaVelivolo());
            stmt.setDate(2, mission.getDataMissione());
            stmt.setInt(3, mission.getNumeroVolo());
            stmt.setTime(4, mission.getOraPartenza());
            stmt.setTime(5, mission.getOraArrivo());

            // Add the new parameters
            stmt.setString(6, mission.getPartNumberLanciatoreP1());
            stmt.setString(7, mission.getPartNumberMissileP1());
            stmt.setString(8, mission.getPartNumberLanciatoreP13());
            stmt.setString(9, mission.getPartNumberMissileP13());

            // Set ID as the last parameter
            stmt.setInt(10, mission.getId());

            int rowsAffected = stmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating mission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, null);
        }

        return success;
    }

    /**
     * Deletes a mission from the database by its ID.
     * Also handles deletion of related records in historical_load and historical_launcher tables.
     *
     * @param id The ID of the mission to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean delete(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DBUtil.getConnection();

            // Begin transaction
            conn.setAutoCommit(false);

            // Delete from historical_load first (if table exists)
            try {
                String sqlHistLoad = "DELETE FROM historical_load WHERE mission_id = ?";
                stmt = conn.prepareStatement(sqlHistLoad);
                stmt.setInt(1, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                // Table might not exist yet, continue with deletion
                System.out.println("Note: historical_load table not found or other error: " + e.getMessage());
            }

            // Delete from historical_launcher (if table exists)
            try {
                String sqlHistLauncher = "DELETE FROM historical_launcher WHERE mission_id = ?";
                stmt = conn.prepareStatement(sqlHistLauncher);
                stmt.setInt(1, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                // Table might not exist yet, continue with deletion
                System.out.println("Note: historical_launcher table not found or other error: " + e.getMessage());
            }

            // Delete from missione_posizione_automatica
            try {
                String sqlMPA = "DELETE FROM missione_posizione_automatica WHERE ID_Missione = ?";
                stmt = conn.prepareStatement(sqlMPA);
                stmt.setInt(1, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                // Continue with deletion
                System.out.println("Error deleting from missione_posizione_automatica: " + e.getMessage());
            }

            // Finally delete the mission
            String sqlMission = "DELETE FROM missione WHERE ID = ?";
            stmt = conn.prepareStatement(sqlMission);
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            // Commit the transaction
            conn.commit();

            success = rowsAffected > 0;
        } catch (SQLException e) {
            // Rollback transaction in case of error
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
                ex.printStackTrace();
            }

            System.err.println("Error deleting mission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Reset auto-commit
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
                e.printStackTrace();
            }

            DBUtil.closeResources(conn, stmt, null);
        }

        return success;
    }

    /**
     * Retrieves a mission by its ID.
     *
     * @param id The ID of the mission to retrieve
     * @return The Mission object if found, null otherwise
     */
    public Mission getById(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Mission mission = null;

        try {
            conn = DBUtil.getConnection();

            // SQL query to find a mission by ID
            String sql = "SELECT * FROM missione WHERE ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                // Mission found, create and return Mission object
                mission = createMissionFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving mission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return mission;
    }

    /**
     * Retrieves a mission by flight number and aircraft.
     *
     * @param matricolaVelivolo The aircraft serial number
     * @param numeroVolo        The flight number
     * @return The Mission object if found, null otherwise
     */
    public Mission getByFlightNumber(String matricolaVelivolo, int numeroVolo) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Mission mission = null;

        try {
            conn = DBUtil.getConnection();

            // SQL query to find a mission by aircraft and flight number
            String sql = "SELECT * FROM missione WHERE MatricolaVelivolo = ? AND NumeroVolo = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matricolaVelivolo);
            stmt.setInt(2, numeroVolo);

            rs = stmt.executeQuery();

            if (rs.next()) {
                // Mission found, create and return Mission object
                mission = createMissionFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving mission: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return mission;
    }

    /**
     * Retrieves all missions from the database.
     *
     * @return A List of all Mission objects
     */
    public List<Mission> getAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Mission> missions = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            // SQL query to retrieve all missions
            String sql = "SELECT * FROM missione ORDER BY DataMissione DESC";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // For each row, create a Mission object and add to list
                Mission mission = createMissionFromResultSet(rs);
                missions.add(mission);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving missions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return missions;
    }

    /**
     * Retrieves all missions for a specific aircraft.
     *
     * @param matricolaVelivolo The aircraft serial number
     * @return A List of Mission objects for the specified aircraft
     */
    public List<Mission> getMissionsByAircraft(String matricolaVelivolo) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Mission> missions = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            // SQL query to retrieve missions for a specific aircraft
            String sql = "SELECT * FROM missione WHERE MatricolaVelivolo = ? ORDER BY DataMissione DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matricolaVelivolo);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // For each row, create a Mission object and add to list
                Mission mission = createMissionFromResultSet(rs);
                missions.add(mission);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving missions for aircraft: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return missions;
    }

    /**
     * Retrieves the latest missions, ordered by ID descending.
     *
     * @param limit The maximum number of missions to retrieve
     * @return A List of the most recent Mission objects
     */
    public List<Mission> getLatestMissions(int limit) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Mission> missions = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            // SQL query to retrieve the latest missions
            String sql = "SELECT * FROM missione ORDER BY ID DESC LIMIT ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // For each row, create a Mission object and add to list
                Mission mission = createMissionFromResultSet(rs);
                missions.add(mission);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving latest missions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return missions;
    }

    /**
     * Creates a Mission object from a ResultSet row.
     *
     * @param rs The ResultSet containing mission data
     * @return A new Mission object
     * @throws SQLException If there is an error accessing the ResultSet
     */
    private Mission createMissionFromResultSet(ResultSet rs) throws SQLException {
        Mission mission = new Mission();
        mission.setId(rs.getInt("ID"));
        mission.setMatricolaVelivolo(rs.getString("MatricolaVelivolo"));
        mission.setDataMissione(rs.getDate("DataMissione"));
        mission.setNumeroVolo(rs.getInt("NumeroVolo"));
        mission.setOraPartenza(rs.getTime("OraPartenza"));
        mission.setOraArrivo(rs.getTime("OraArrivo"));

        // Add the new fields
        mission.setPartNumberLanciatoreP1(rs.getString("PartNumberLanciatoreP1"));
        mission.setPartNumberMissileP1(rs.getString("PartNumberMissileP1"));
        mission.setPartNumberLanciatoreP13(rs.getString("PartNumberLanciatoreP13"));
        mission.setPartNumberMissileP13(rs.getString("PartNumberMissileP13"));

        return mission;
    }

    /**
     * Retrieves missions by date range.
     *
     * @param fromDate The start date
     * @param toDate   The end date
     * @return A List of Mission objects within the specified date range
     */
    public List<Mission> getMissionsByDateRange(Date fromDate, Date toDate) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Mission> missions = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            // SQL query to retrieve missions within a date range
            String sql = "SELECT * FROM missione WHERE DataMissione BETWEEN ? AND ? ORDER BY DataMissione DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setDate(1, fromDate);
            stmt.setDate(2, toDate);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // For each row, create a Mission object and add to list
                Mission mission = createMissionFromResultSet(rs);
                missions.add(mission);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving missions by date range: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return missions;
    }

    /**
     * Retrieves missions by aircraft and date range.
     *
     * @param matricolaVelivolo The aircraft serial number
     * @param fromDate          The start date
     * @param toDate            The end date
     * @return A List of Mission objects for the specified aircraft within the date range
     */
    public List<Mission> getMissionsByAircraftAndDateRange(String matricolaVelivolo, Date fromDate, Date toDate) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Mission> missions = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            // SQL query to retrieve missions for a specific aircraft within a date range
            String sql = "SELECT * FROM missione WHERE MatricolaVelivolo = ? AND DataMissione BETWEEN ? AND ? ORDER BY DataMissione DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matricolaVelivolo);
            stmt.setDate(2, fromDate);
            stmt.setDate(3, toDate);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // For each row, create a Mission object and add to list
                Mission mission = createMissionFromResultSet(rs);
                missions.add(mission);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving missions by aircraft and date range: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return missions;
    }


    public Mission getMissionById(int id) {
        return getById(id);
    }

    /**
     * Retrieves flight data for a specific mission.
     *
     * @param id The mission ID
     * @return Array containing [MissionID, MaxGLoad, MinGLoad, AvgAltitude, MaxSpeed, MissileStatus]
     */
    public Object[] getFlightDataForMission(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Object[] flightData = new Object[6]; // ID, MaxGLoad, MinGLoad, AvgAltitude, MaxSpeed, MissileStatus

        try {
            conn = DBUtil.getConnection();
            System.out.println("Fetching flight data for mission ID: " + id);

            // Get mission details first
            String sqlMission = "SELECT ID, MatricolaVelivolo, NumeroVolo FROM missione WHERE ID = ?";
            stmt = conn.prepareStatement(sqlMission);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                flightData[0] = rs.getInt("ID");
                String matricola = rs.getString("MatricolaVelivolo");
                int numeroVolo = rs.getInt("NumeroVolo");

                // Set default values
                flightData[1] = 0.0; // MaxGLoad
                flightData[2] = 0.0; // MinGLoad
                flightData[3] = 0.0; // AvgAltitude
                flightData[4] = 0.0; // MaxSpeed
                flightData[5] = ""; // MissileStatus

                rs.close();
                stmt.close();

                // Get flight data from dati_registrati - ensure we get the most recent record
                String sqlFlightData = "SELECT GloadMax, GloadMin, QuotaMedia, VelocitaMassima, StatoMissili " +
                        "FROM dati_registrati " +
                        "WHERE MatricolaVelivolo = ? AND NumeroVolo = ? " +
                        "ORDER BY ID DESC LIMIT 1";

                stmt = conn.prepareStatement(sqlFlightData);
                stmt.setString(1, matricola);
                stmt.setInt(2, numeroVolo);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    flightData[1] = rs.getDouble("GloadMax");
                    flightData[2] = rs.getDouble("GloadMin");
                    flightData[3] = rs.getDouble("QuotaMedia");
                    flightData[4] = rs.getDouble("VelocitaMassima");
                    flightData[5] = rs.getString("StatoMissili");

                    System.out.println("Flight data retrieved - Mission ID: " + flightData[0] +
                            ", MaxG: " + flightData[1] +
                            ", MinG: " + flightData[2] +
                            ", Altitude: " + flightData[3] +
                            ", Speed: " + flightData[4]);
                } else {
                    System.out.println("No flight data found in dati_registrati for: " +
                            matricola + ", Flight: " + numeroVolo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving flight data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return flightData;
    }

    public List<WeaponStatus> getWeaponsForMission(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<WeaponStatus> weapons = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            System.out.println("Fetching weapons for mission ID: " + id);

            // Get mission details first
            String sqlMission = "SELECT MatricolaVelivolo FROM missione WHERE ID = ?";
            stmt = conn.prepareStatement(sqlMission);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            String matricola = null;
            if (rs.next()) {
                matricola = rs.getString("MatricolaVelivolo");
                rs.close();
                stmt.close();
            } else {
                return weapons; // Return empty list if mission not found
            }

            // Join with dichiarazione_missile_gui to get firing status
            String sqlWeapons =
                    "SELECT sl.PosizioneVelivolo, sl.PartNumber AS LauncherPN, '' AS LauncherSN, " +
                            "sc.PartNumber AS MissilePN, sc.Nomenclatura AS MissileName, " +
                            "CASE WHEN dm.Missile_Sparato = 'FIRED' THEN 'FIRED' ELSE 'ONBOARD' END AS Status " +
                            "FROM storico_lanciatore sl " +
                            "LEFT JOIN storico_carico sc ON sl.PosizioneVelivolo = sc.PosizioneVelivolo " +
                            "AND sl.MatricolaVelivolo = sc.MatricolaVelivolo " +
                            "LEFT JOIN dichiarazione_missile_gui dm ON dm.PosizioneVelivolo = sl.PosizioneVelivolo " +
                            "AND dm.ID_Missione = ? " +
                            "WHERE sl.MatricolaVelivolo = ? " +
                            "ORDER BY sl.PosizioneVelivolo";

            stmt = conn.prepareStatement(sqlWeapons);
            stmt.setInt(1, id);
            stmt.setString(2, matricola);
            rs = stmt.executeQuery();

            while (rs.next()) {
                WeaponStatus weapon = new WeaponStatus();
                String position = rs.getString("PosizioneVelivolo");
                weapon.setPosition(position);
                weapon.setMissilePartNumber(rs.getString("MissilePN"));
                weapon.setMissileName(rs.getString("MissileName"));
                weapon.setStatus(rs.getString("Status"));
                weapon.setLauncherPartNumber(rs.getString("LauncherPN"));
                weapon.setLauncherSerialNumber(rs.getString("LauncherSN"));
                weapons.add(weapon);
            }

            System.out.println("Found " + weapons.size() + " weapons for mission ID: " + id);
        } catch (SQLException e) {
            System.err.println("Error retrieving weapons: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return weapons;
    }

    /**
     * Checks if a flight number already exists for the specified aircraft.
     *
     * @param matricolaVelivolo The aircraft serial number
     * @param numeroVolo        The flight number to check
     * @return true if the flight number already exists for the aircraft, false otherwise
     */
    public boolean flightNumberExists(String matricolaVelivolo, int numeroVolo) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean exists = false;

        try {
            conn = DBUtil.getConnection();

            // SQL query to check if a flight number exists for the specified aircraft
            String sql = "SELECT 1 FROM missione WHERE MatricolaVelivolo = ? AND NumeroVolo = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, matricolaVelivolo);
            stmt.setInt(2, numeroVolo);

            rs = stmt.executeQuery();
            exists = rs.next(); // If rs.next() returns true, a record was found
        } catch (SQLException e) {
            System.err.println("Error checking flight number existence: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtil.closeResources(conn, stmt, rs);
        }

        return exists;
    }

    // Helper method to set string parameters that might be null
    private void setStringOrNull(PreparedStatement stmt, int paramIndex, String value) throws SQLException {
        if (value != null && !value.isEmpty()) {
            stmt.setString(paramIndex, value);
            System.out.println("Param " + paramIndex + ": '" + value + "'");
        } else {
            stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
            System.out.println("Param " + paramIndex + ": NULL");
        }
    }

    /**
     * Inserts a new mission into the database and returns the generated ID.
     *
     * @param mission The Mission object to insert
     * @return The generated mission ID, or -1 if insertion fails
     */
    public int insertAndGetId(Mission mission) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        int generatedId = -1;

        try {
            conn = DBUtil.getConnection();

            // Set to true to ensure SQL errors will throw exceptions
            conn.setAutoCommit(true);

            System.out.println("MissionDAO: Starting insert operation for " + mission.getMatricolaVelivolo() +
                    ", flight " + mission.getNumeroVolo());

            // SQL query for inserting mission
            String sql = "INSERT INTO missione (MatricolaVelivolo, DataMissione, NumeroVolo, OraPartenza, OraArrivo, " +
                    "PartNumberLanciatoreP1, PartNumberMissileP1, PartNumberLanciatoreP13, PartNumberMissileP13) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            System.out.println("SQL: " + sql);
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // Set parameters with logging
            int paramIndex = 1;

            // Required parameters
            stmt.setString(paramIndex++, mission.getMatricolaVelivolo());
            stmt.setDate(paramIndex++, mission.getDataMissione());
            stmt.setInt(paramIndex++, mission.getNumeroVolo());

            // Handle nullable parameters
            if (mission.getOraPartenza() != null) {
                stmt.setTime(paramIndex++, mission.getOraPartenza());
                System.out.println("Param " + (paramIndex - 1) + ": " + mission.getOraPartenza());
            } else {
                stmt.setNull(paramIndex++, java.sql.Types.TIME);
                System.out.println("Param " + (paramIndex - 1) + ": NULL");
            }

            if (mission.getOraArrivo() != null) {
                stmt.setTime(paramIndex++, mission.getOraArrivo());
                System.out.println("Param " + (paramIndex - 1) + ": " + mission.getOraArrivo());
            } else {
                stmt.setNull(paramIndex++, java.sql.Types.TIME);
                System.out.println("Param " + (paramIndex - 1) + ": NULL");
            }

            // Set launcher and missile parameters - always use setNull for nulls
            setStringOrNull(stmt, paramIndex++, mission.getPartNumberLanciatoreP1());
            setStringOrNull(stmt, paramIndex++, mission.getPartNumberMissileP1());
            setStringOrNull(stmt, paramIndex++, mission.getPartNumberLanciatoreP13());
            setStringOrNull(stmt, paramIndex++, mission.getPartNumberMissileP13());

            // Execute with better error detection
            try {
                System.out.println("Executing SQL...");
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Insert affected " + rowsAffected + " rows");

                if (rowsAffected <= 0) {
                    System.err.println("WARNING: No rows affected by insert!");
                    return -1;
                }
            } catch (SQLException e) {
                System.err.println("SQL ERROR: " + e.getMessage());

                // Check for warnings
                SQLWarning warning = stmt.getWarnings();
                while (warning != null) {
                    System.err.println("SQL Warning: " + warning.getMessage());
                    warning = warning.getNextWarning();
                }

                throw e; // Re-throw to be handled by caller
            }

            // Get the generated ID
            generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                generatedId = generatedKeys.getInt(1);
                mission.setId(generatedId);
                System.out.println("Generated mission ID: " + generatedId);
                return generatedId;
            } else {
                System.err.println("No generated keys returned!");
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting mission: " + e.getMessage());
            e.printStackTrace();
            return -1;
        } finally {
            DBUtil.closeResources(conn, stmt, generatedKeys);
        }
    }
}

