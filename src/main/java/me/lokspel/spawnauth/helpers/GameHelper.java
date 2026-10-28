package me.lokspel.spawnauth.helpers;

import com.tcoded.folialib.FoliaLib;
import me.lokspel.spawnauth.config.SpawnAuthConfig;
import me.lokspel.spawnauth.config.SpawnMode;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class GameHelper {

    private final SpawnAuthConfig config;
    private final FoliaLib foliaLib;
    private final Logger logger;
    private Location cachedFixedLocation;

    public GameHelper(SpawnAuthConfig config, FoliaLib foliaLib, Logger logger) {
        this.config = config;
        this.foliaLib = foliaLib;
        this.logger = logger;
    }

    public Location getAuthSpawnLocation(SpawnMode mode) {
        if (mode == SpawnMode.DISABLED) {
            return null;
        }

        if (mode == SpawnMode.FIXED) {
            if (cachedFixedLocation == null || cachedFixedLocation.getWorld() == null) {
                World fixedWorld = Bukkit.getWorld(config.getFixedWorldName());
                if (fixedWorld == null) {
                    return null;
                }
                cachedFixedLocation = config.createFixedLocation(fixedWorld);
            }
            return cachedFixedLocation != null ? cachedFixedLocation.clone() : null;
        }

        World overworld = Bukkit.getWorld(config.getOverworldName());
        if (overworld == null) {
            overworld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }

        if (overworld == null) {
            return null;
        }

        return calculateServerSpawnLocation(overworld);
    }

    private Location calculateServerSpawnLocation(World world) {
        Location spawn = world.getSpawnLocation();

        int radius = 0;
        try {
            Integer rule = world.getGameRuleValue(GameRule.SPAWN_RADIUS);
            if (rule != null) {
                radius = Math.max(0, rule);
            }
        } catch (Throwable ignored) {
        }

        if (radius <= 0) {
            return spawn.clone().add(0.5, 0, 0.5);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = spawn.getBlockX() + random.nextInt(-radius, radius + 1);
        int z = spawn.getBlockZ() + random.nextInt(-radius, radius + 1);

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int y;
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            y = world.getHighestBlockYAt(x, z) + 1;
        } else {
            y = spawn.getBlockY();
        }

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public boolean isInAuthWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        String worldName = location.getWorld().getName();
        return worldName.equals(config.getOverworldName()) || worldName.equals(config.getFixedWorldName());
    }

    public CompletableFuture<Boolean> teleport(Player player, Location destination) {
        if (player == null || destination == null || destination.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }

        return foliaLib.getImpl().teleportAsync(player, destination);
    }

    public void applyLimboEffects(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.setCollidable(false);
        player.setPlayerWeather(WeatherType.CLEAR);
    }

    public void resetLimboEffects(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.setCollidable(true);
        player.resetPlayerWeather();
    }

    public void updateLimboEffects(Player player, boolean isUnauthenticated) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean inLimboWorld = isInAuthWorld(player.getLocation());
        if (isUnauthenticated && inLimboWorld) {
            applyLimboEffects(player);
        } else {
            resetLimboEffects(player);
        }
    }

    public void setCachedFixedLocation(Location location) {
        this.cachedFixedLocation = location != null ? location.clone() : null;
    }
}
