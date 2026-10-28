package me.lokspel.spawnauth.listeners;

import com.tcoded.folialib.FoliaLib;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.config.SpawnMode;
import me.lokspel.spawnauth.helpers.GameHelper;
import me.lokspel.spawnauth.storage.StorageService;
import me.lokspel.spawnauth.storage.model.SavedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerConnectionListener implements Listener {

    private final AuthManager authManager;
    private final GameHelper gameHelper;
    private final StorageService storageService;
    private final FoliaLib foliaLib;

    public PlayerConnectionListener(AuthManager authManager, GameHelper gameHelper,
                                    StorageService storageService, FoliaLib foliaLib) {
        this.authManager = authManager;
        this.gameHelper = gameHelper;
        this.storageService = storageService;
        this.foliaLib = foliaLib;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        Location joinLocation = player.getLocation();

        if (player.isDead()) {
            storageService.removeLocation(playerName);
            try {
                player.spigot().respawn();
            } catch (Throwable ignored) {
            }
            foliaLib.getScheduler().runAtEntity(player, task ->
                    handlePostJoin(player, playerName, player.getLocation(), true));
            return;
        }

        handlePostJoin(player, playerName, joinLocation, false);
    }

    private void handlePostJoin(Player player, String playerName, Location joinLocation, boolean fromRespawn) {
        if (!player.isOnline()) {
            return;
        }

        if (authManager.isAuthenticated(player)) {
            storageService.takeLocation(playerName).thenAccept(saved -> {
                if (saved != null) {
                    Location target = saved.toBukkitLocation();
                    if (target != null && gameHelper.isInAuthWorld(player.getLocation())) {
                        foliaLib.getScheduler().runAtEntity(player, t -> {
                            if (player.isOnline()) {
                                gameHelper.teleport(player, target);
                                gameHelper.resetLimboEffects(player);
                            }
                        });
                        return;
                    }
                }
                foliaLib.getScheduler().runAtEntity(player, t -> {
                    if (player.isOnline()) {
                        gameHelper.resetLimboEffects(player);
                    }
                });
            });
            return;
        }

        SpawnMode mode = authManager.getSpawnMode(player);
        if (mode == SpawnMode.DISABLED) {
            return;
        }

        storageService.getLocation(playerName).thenAccept(existing ->
                foliaLib.getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline() || authManager.isAuthenticated(player)) {
                        return;
                    }

                    if (existing == null && !fromRespawn) {
                        storageService.saveLocation(SavedLocation.fromBukkitLocation(playerName, joinLocation));
                    }

                    Location authSpawn = gameHelper.getAuthSpawnLocation(mode);
                    if (authSpawn != null) {
                        gameHelper.teleport(player, authSpawn).thenRun(() -> {
                            if (player.isOnline() && !authManager.isAuthenticated(player)) {
                                gameHelper.applyLimboEffects(player);
                            }
                        });
                    } else {
                        gameHelper.applyLimboEffects(player);
                    }
                })
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!authManager.isAuthenticated(player)) {
            SpawnMode mode = authManager.getSpawnMode(player);
            if (mode != SpawnMode.DISABLED) {
                storageService.saveLocation(SavedLocation.fromBukkitLocation(player.getName(), event.getRespawnLocation()));
                Location authSpawn = gameHelper.getAuthSpawnLocation(mode);
                if (authSpawn != null) {
                    event.setRespawnLocation(authSpawn);
                    foliaLib.getScheduler().runAtEntity(player, t -> {
                        if (player.isOnline()) {
                            gameHelper.applyLimboEffects(player);
                        }
                    });
                    return;
                }
            }
        }

        foliaLib.getScheduler().runAtEntity(player, t -> {
            if (player.isOnline()) {
                gameHelper.updateLimboEffects(player, !authManager.isAuthenticated(player));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameHelper.resetLimboEffects(player);
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
    }
}
