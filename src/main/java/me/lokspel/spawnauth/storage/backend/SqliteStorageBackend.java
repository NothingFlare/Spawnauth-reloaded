package me.lokspel.spawnauth.storage.backend;

import me.lokspel.spawnauth.storage.model.SavedLocation;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteStorageBackend implements StorageBackend {

    private final File dbFile;
    private final String tableName;
    private final Logger logger;
    private Connection connection;

    private static final String COLUMNS = "player_name, world, x, y, z, yaw, pitch";

    public SqliteStorageBackend(File dataFolder, String tablePrefix, Logger logger) {
        this.dbFile = new File(dataFolder, "SpawnAuth.db");
        this.tableName = tablePrefix + "_locations";
        this.logger = logger;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL;");
                statement.execute("PRAGMA synchronous = NORMAL;");
                statement.execute("PRAGMA busy_timeout = 5000;");
                statement.execute("PRAGMA temp_store = MEMORY;");
            }
        }
        return connection;
    }

    @Override
    public synchronized void init() throws SQLException {
        Connection conn = getConnection();
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_name VARCHAR(64) PRIMARY KEY, " +
                "world VARCHAR(128) NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL DEFAULT 0, " +
                "pitch FLOAT NOT NULL DEFAULT 0" +
                ");";

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(createTableSql);
        }
    }

    @Override
    public synchronized void upsert(SavedLocation location) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_name) DO UPDATE SET " +
                "world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, " +
                "yaw = excluded.yaw, pitch = excluded.pitch;";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, location.playerName());
            ps.setString(2, location.worldName());
            ps.setDouble(3, location.x());
            ps.setDouble(4, location.y());
            ps.setDouble(5, location.z());
            ps.setFloat(6, location.yaw());
            ps.setFloat(7, location.pitch());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized SavedLocation get(String playerName) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM " + tableName + " WHERE player_name = ?;";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SavedLocation(
                            rs.getString("player_name"),
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        }
        return null;
    }

    @Override
    public synchronized void delete(String playerName) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE player_name = ?;";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized Collection<SavedLocation> getAll() throws SQLException {
        List<SavedLocation> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM " + tableName + ";";

        try (Statement statement = getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new SavedLocation(
                        rs.getString("player_name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                ));
            }
        }
        return list;
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing SQLite connection", e);
            }
            connection = null;
        }
    }
}
