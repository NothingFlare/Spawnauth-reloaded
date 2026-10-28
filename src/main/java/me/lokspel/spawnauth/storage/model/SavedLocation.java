package me.lokspel.spawnauth.storage.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record SavedLocation(
        String playerName,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public Location toBukkitLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static SavedLocation fromBukkitLocation(String playerName, Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new SavedLocation(
                playerName,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }
}
