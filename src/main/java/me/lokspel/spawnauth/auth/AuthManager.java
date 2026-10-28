package me.lokspel.spawnauth.auth;

import com.tcoded.folialib.FoliaLib;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.providers.AuthMeProvider;
import me.lokspel.spawnauth.auth.providers.LoginSecurityProvider;
import me.lokspel.spawnauth.auth.providers.LoginToProvider;
import me.lokspel.spawnauth.auth.providers.NLoginProvider;
import me.lokspel.spawnauth.auth.providers.OpenLoginProvider;
import me.lokspel.spawnauth.config.SpawnAuthConfig;
import me.lokspel.spawnauth.config.SpawnMode;
import me.lokspel.spawnauth.helpers.GameHelper;
import me.lokspel.spawnauth.storage.StorageService;
import me.lokspel.spawnauth.storage.model.SavedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;

public class AuthManager {

    private final SpawnAuth plugin;
    private final SpawnAuthConfig config;
    private final StorageService storageService;
    private final GameHelper gameHelper;
    private final FoliaLib foliaLib;
    private final Logger logger;
    private AuthProvider activeProvider;

    public AuthManager(SpawnAuth plugin, SpawnAuthConfig config, StorageService storageService,
                       GameHelper gameHelper, FoliaLib foliaLib, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.storageService = storageService;
        this.gameHelper = gameHelper;
        this.foliaLib = foliaLib;
        this.logger = logger;
    }

    public boolean init() {
        PluginManager pm = plugin.getServer().getPluginManager();

        if (isPluginAvailable(pm, "nLogin")) {
            activeProvider = new NLoginProvider();
        } else if (isPluginAvailable(pm, "OpeNLogin")) {
            activeProvider = new OpenLoginProvider();
        } else if (isPluginAvailable(pm, "LoginSecurity")) {
            activeProvider = new LoginSecurityProvider();
        } else if (isPluginAvailable(pm, "AuthMe")) {
            activeProvider = new AuthMeProvider();
        } else if (isPluginAvailable(pm, "LoginTo")) {
            activeProvider = new LoginToProvider();
        }

        if (activeProvider == null) {
            logger.severe("No supported authentication plugin found!");
            logger.severe("Please install one of: AuthMe, nLogin, OpeNLogin, LoginSecurity, LoginTo");
            return false;
        }

        logger.info("Using " + activeProvider.getName() + " as authentication provider.");
        activeProvider.registerListeners(plugin, this);
        return true;
    }

    private boolean isPluginAvailable(PluginManager pm, String name) {
        var p = pm.getPlugin(name);
        return p != null && p.isEnabled();
    }

    public boolean isAuthenticated(Player player) {
        return activeProvider != null && activeProvider.isAuthenticated(player);
    }

    public boolean isRegistered(Player player) {
        return activeProvider != null && activeProvider.isRegistered(player);
    }

    public SpawnMode getSpawnMode(Player player) {
        if (isRegistered(player)) {
            return config.getLoginSpawnMode();
        }
        return config.getRegisterSpawnMode();
    }

    public void handleAuthenticated(Player player, boolean isRegister) {
        if (player == null) {
            return;
        }

        SpawnMode mode = isRegister ? config.getRegisterSpawnMode() : config.getLoginSpawnMode();

        foliaLib.getScheduler().runAtEntity(player, task -> {
            if (!player.isOnline()) {
                return;
            }

            if (mode != SpawnMode.DISABLED) {
                storageService.takeLocation(player.getName()).thenAccept(saved -> {
                    if (saved != null) {
                        Location target = saved.toBukkitLocation();
                        if (target != null) {
                            foliaLib.getScheduler().runAtEntity(player, teleportTask -> {
                                if (player.isOnline()) {
                                    gameHelper.teleport(player, target);
                                    gameHelper.resetLimboEffects(player);
                                }
                            });
                            return;
                        }
                    }

                    foliaLib.getScheduler().runAtEntity(player, resetTask -> {
                        if (player.isOnline()) {
                            gameHelper.resetLimboEffects(player);
                        }
                    });
                });
            } else {
                gameHelper.resetLimboEffects(player);
            }
        });
    }

    public void handleLoggedOut(Player player) {
        if (player == null) {
            return;
        }

        foliaLib.getScheduler().runAtEntity(player, task -> {
            if (!player.isOnline()) {
                return;
            }

            SpawnMode mode = getSpawnMode(player);
            if (mode == SpawnMode.DISABLED) {
                return;
            }

            Location currentLoc = player.getLocation();
            storageService.saveLocation(SavedLocation.fromBukkitLocation(player.getName(), currentLoc));

            Location authSpawn = gameHelper.getAuthSpawnLocation(mode);
            if (authSpawn != null) {
                gameHelper.teleport(player, authSpawn).thenRun(() -> {
                    if (player.isOnline()) {
                        gameHelper.applyLimboEffects(player);
                    }
                });
            }
        });
    }

    public AuthProvider getActiveProvider() {
        return activeProvider;
    }
}
