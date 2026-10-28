package me.lokspel.spawnauth;

import com.tcoded.folialib.FoliaLib;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.commands.SpawnAuthCommand;
import me.lokspel.spawnauth.config.SpawnAuthConfig;
import me.lokspel.spawnauth.helpers.GameHelper;
import me.lokspel.spawnauth.listeners.PlayerConnectionListener;
import me.lokspel.spawnauth.storage.StorageService;
import me.lokspel.spawnauth.storage.model.SavedLocation;
import me.lokspel.spawnauth.world.LimboWorldManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnAuth extends JavaPlugin {

    private FoliaLib foliaLib;
    private SpawnAuthConfig config;
    private StorageService storageService;
    private GameHelper gameHelper;
    private AuthManager authManager;
    private LimboWorldManager limboWorldManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Failed to create plugin data directory!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        this.config = new SpawnAuthConfig(getConfig());

        this.foliaLib = new FoliaLib(this);

        try {
            this.storageService = new StorageService(getDataFolder(), config, getLogger());
            this.storageService.init();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gameHelper = new GameHelper(config, foliaLib, getLogger());
        this.limboWorldManager = new LimboWorldManager(config, foliaLib, getLogger());

        if (config.usesFixedSpawn()) {
            World limbo = limboWorldManager.loadOrCreateLimboWorld();
            if (limbo == null) {
                getLogger().warning("Fixed spawn mode is active, but the limbo world could not be found or created!");
            }
        }

        this.authManager = new AuthManager(this, config, storageService, gameHelper, foliaLib, getLogger());
        if (!authManager.init()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(authManager, gameHelper, storageService, foliaLib),
                this
        );

        PluginCommand command = getCommand("spawnauth");
        if (command != null) {
            SpawnAuthCommand cmdExecutor = new SpawnAuthCommand(this);
            command.setExecutor(cmdExecutor);
            command.setTabCompleter(cmdExecutor);
        }

        // bStats metrics
        try {
            Metrics metrics = new Metrics(this, 33136);
            metrics.addCustomChart(new SimplePie("auth_provider", () ->
                    authManager.getActiveProvider() != null ? authManager.getActiveProvider().getName() : "None"));
            metrics.addCustomChart(new SimplePie("database_type", () ->
                    config.getDatabaseType().name()));
        } catch (Throwable ignored) {
        }

        getLogger().info("SpawnAuth v" + getDescription().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Safely restore any online unauthenticated players to their original locations
        if (authManager != null && storageService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (!authManager.isAuthenticated(player)) {
                        SavedLocation saved = storageService.getDirectFromCache(player.getName());
                        if (saved != null) {
                            Location target = saved.toBukkitLocation();
                            if (target != null) {
                                player.teleport(target);
                            }
                        }
                        if (gameHelper != null) {
                            gameHelper.resetLimboEffects(player);
                        }
                    }
                } catch (Throwable t) {
                    getLogger().warning("Error restoring location for player " + player.getName() + " on disable: " + t.getMessage());
                }
            }
        }

        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }

        if (storageService != null) {
            storageService.close();
        }

        getLogger().info("SpawnAuth has been disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        this.config = new SpawnAuthConfig(getConfig());
        this.gameHelper.setCachedFixedLocation(null);

        if (config.usesFixedSpawn()) {
            limboWorldManager.loadOrCreateLimboWorld();
        }

        getLogger().info("Configuration reloaded successfully.");
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public SpawnAuthConfig getSpawnAuthConfig() {
        return config;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public GameHelper getGameHelper() {
        return gameHelper;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }
}
