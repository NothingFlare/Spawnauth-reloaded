package me.lokspel.spawnauth.config;

import me.lokspel.spawnauth.storage.DatabaseType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class SpawnAuthConfig {

    private final String overworldName;
    private final SpawnMode loginSpawnMode;
    private final SpawnMode registerSpawnMode;
    private final String fixedWorldName;
    private final boolean fixedCreate;
    private final double fixedSpawnX;
    private final double fixedSpawnY;
    private final double fixedSpawnZ;
    private final float fixedSpawnYaw;
    private final float fixedSpawnPitch;
    private final int fixedPlatformRadius;
    private final DatabaseType databaseType;
    private final String tablePrefix;
    private final boolean databaseCache;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final String mysqlUrlParameters;
    private final int poolMaxSize;
    private final int poolMinIdle;
    private final long poolConnectionTimeoutMs;

    public SpawnAuthConfig(FileConfiguration config) {
        this.overworldName = config.getString("worlds.overworld", "world");
        this.loginSpawnMode = SpawnMode.fromString(config.getString("login.spawn-mode", "vanilla"));
        this.registerSpawnMode = SpawnMode.fromString(config.getString("register.spawn-mode", "vanilla"));

        this.fixedWorldName = config.getString("fixed.world", "limbo");
        this.fixedCreate = config.getBoolean("fixed.create", false);
        this.fixedSpawnX = config.getDouble("fixed.spawn.x", 7.0);
        this.fixedSpawnY = config.getDouble("fixed.spawn.y", 70.0);
        this.fixedSpawnZ = config.getDouble("fixed.spawn.z", 7.0);
        this.fixedSpawnYaw = (float) config.getDouble("fixed.spawn.yaw", 0.0);
        this.fixedSpawnPitch = (float) config.getDouble("fixed.spawn.pitch", 0.0);
        this.fixedPlatformRadius = Math.max(0, config.getInt("fixed.platform-radius", 1));

        this.databaseType = DatabaseType.fromString(config.getString("database.type", "sqlite"));
        this.tablePrefix = config.getString("database.table-prefix", "spawnauth");
        this.databaseCache = config.getBoolean("database.cache", true);

        this.mysqlHost = config.getString("database.mysql.host", "localhost");
        this.mysqlPort = config.getInt("database.mysql.port", 3306);
        this.mysqlDatabase = config.getString("database.mysql.database", "spawnauth");
        this.mysqlUser = config.getString("database.mysql.user", "root");
        this.mysqlPassword = config.getString("database.mysql.password", "");
        this.mysqlUrlParameters = config.getString("database.mysql.url-parameters", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        this.poolMaxSize = Math.max(1, config.getInt("database.pool.max-pool-size", 10));
        this.poolMinIdle = Math.max(1, config.getInt("database.pool.min-idle", 2));
        this.poolConnectionTimeoutMs = Math.max(1000L, config.getLong("database.pool.connection-timeout-ms", 30000L));
    }

    public boolean usesFixedSpawn() {
        return loginSpawnMode == SpawnMode.FIXED || registerSpawnMode == SpawnMode.FIXED;
    }

    public Location createFixedLocation(World world) {
        if (world == null) {
            return null;
        }
        return new Location(world, fixedSpawnX + 0.5, fixedSpawnY, fixedSpawnZ + 0.5, fixedSpawnYaw, fixedSpawnPitch);
    }

    public String getOverworldName() {
        return overworldName;
    }

    public SpawnMode getLoginSpawnMode() {
        return loginSpawnMode;
    }

    public SpawnMode getRegisterSpawnMode() {
        return registerSpawnMode;
    }

    public String getFixedWorldName() {
        return fixedWorldName;
    }

    public boolean isFixedCreate() {
        return fixedCreate;
    }

    public double getFixedSpawnX() {
        return fixedSpawnX;
    }

    public double getFixedSpawnY() {
        return fixedSpawnY;
    }

    public double getFixedSpawnZ() {
        return fixedSpawnZ;
    }

    public float getFixedSpawnYaw() {
        return fixedSpawnYaw;
    }

    public float getFixedSpawnPitch() {
        return fixedSpawnPitch;
    }

    public int getFixedPlatformRadius() {
        return fixedPlatformRadius;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public boolean isDatabaseCache() {
        return databaseCache;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUser() {
        return mysqlUser;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getMysqlUrlParameters() {
        return mysqlUrlParameters;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public long getPoolConnectionTimeoutMs() {
        return poolConnectionTimeoutMs;
    }
}
