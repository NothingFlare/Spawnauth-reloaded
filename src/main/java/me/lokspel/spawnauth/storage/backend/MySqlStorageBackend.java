package me.lokspel.spawnauth.storage.backend;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lokspel.spawnauth.config.SpawnAuthConfig;
import me.lokspel.spawnauth.storage.model.SavedLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySqlStorageBackend implements StorageBackend {

    private final HikariDataSource dataSource;
    private final String tableName;
    private final Logger logger;

    private static final String COLUMNS = "player_name, world, x, y, z, yaw, pitch";

    public MySqlStorageBackend(SpawnAuthConfig config, Logger logger) {
        this.logger = logger;
        this.tableName = config.getTablePrefix() + "_locations";

        String jdbcUrl = "jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort()
                + "/" + config.getMysqlDatabase() + "?" + config.getMysqlUrlParameters();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getMysqlUser());
        hikariConfig.setPassword(config.getMysqlPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolMaxSize());
        hikariConfig.setMinimumIdle(config.getPoolMinIdle());
        hikariConfig.setConnectionTimeout(config.getPoolConnectionTimeoutMs());
        hikariConfig.setPoolName("SpawnAuth-Hikari");

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    public void init() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_name VARCHAR(64) PRIMARY KEY, " +
                "world VARCHAR(128) NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL DEFAULT 0, " +
                "pitch FLOAT NOT NULL DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public void upsert(SavedLocation location) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), " +
                "yaw = VALUES(yaw), pitch = VALUES(pitch);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
    public SavedLocation get(String playerName) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM " + tableName + " WHERE player_name = ?;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
    public void delete(String playerName) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE player_name = ?;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        }
    }

    @Override
    public Collection<SavedLocation> getAll() throws SQLException {
        List<SavedLocation> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM " + tableName + ";";

        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement();
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
    public void close() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error closing MySQL connection pool", e);
        }
    }
}
