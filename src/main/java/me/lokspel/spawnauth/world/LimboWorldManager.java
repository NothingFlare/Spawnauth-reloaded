package me.lokspel.spawnauth.world;

import com.tcoded.folialib.FoliaLib;
import me.lokspel.spawnauth.config.SpawnAuthConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.logging.Logger;

public class LimboWorldManager {

    private final SpawnAuthConfig config;
    private final FoliaLib foliaLib;
    private final Logger logger;

    public LimboWorldManager(SpawnAuthConfig config, FoliaLib foliaLib, Logger logger) {
        this.config = config;
        this.foliaLib = foliaLib;
        this.logger = logger;
    }

    public World loadOrCreateLimboWorld() {
        String worldName = config.getFixedWorldName();
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            configureWorld(existing);
            return existing;
        }

        if (!config.isFixedCreate()) {
            logger.warning("Limbo world '" + worldName + "' was not found and fixed.create is false.");
            return null;
        }

        if (foliaLib.isFolia()) {
            logger.severe("Automatic dynamic world creation is not supported by Folia.");
            logger.severe("Please pre-create the limbo world '" + worldName + "' on Folia.");
            return null;
        }

        int spawnX = (int) config.getFixedSpawnX();
        int spawnY = (int) config.getFixedSpawnY();
        int spawnZ = (int) config.getFixedSpawnZ();
        int radius = config.getFixedPlatformRadius();

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidWorldGenerator(spawnX, spawnY - 1, spawnZ, radius));
        creator.generateStructures(false);

        World created = creator.createWorld();
        if (created == null) {
            logger.severe("Failed to create limbo world '" + worldName + "'!");
            return null;
        }

        Location spawnLocation = config.createFixedLocation(created);
        if (spawnLocation != null) {
            created.setSpawnLocation(spawnLocation);
        }

        configureWorld(created);
        logger.info("Successfully created and configured limbo world '" + worldName + "'.");
        return created;
    }

    public static void configureWorld(World world) {
        if (world == null) {
            return;
        }

        setSafeGameRule(world, GameRule.DO_DAYLIGHT_CYCLE, false);
        setSafeGameRule(world, GameRule.DO_MOB_SPAWNING, false);
        setSafeGameRule(world, GameRule.DO_WEATHER_CYCLE, false);
        setSafeGameRule(world, GameRule.DO_INSOMNIA, false);
        setSafeGameRule(world, GameRule.FALL_DAMAGE, false);
        setSafeGameRule(world, GameRule.KEEP_INVENTORY, true);

        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setKeepSpawnInMemory(false);
    }

    private static <T> void setSafeGameRule(World world, GameRule<T> rule, T value) {
        try {
            if (rule != null) {
                world.setGameRule(rule, value);
            }
        } catch (Throwable ignored) {
        }
    }
}
