package me.lokspel.spawnauth.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class VoidWorldGenerator extends ChunkGenerator {

    private final int spawnX;
    private final int platformY;
    private final int spawnZ;
    private final int platformRadius;

    public VoidWorldGenerator(int spawnX, int platformY, int spawnZ, int platformRadius) {
        this.spawnX = spawnX;
        this.platformY = platformY;
        this.spawnZ = spawnZ;
        this.platformRadius = Math.max(0, platformRadius);
    }

    @Override
    public void generateSurface(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {
        if (platformRadius <= 0) {
            return;
        }

        if (platformY < chunkData.getMinHeight() || platformY >= chunkData.getMaxHeight()) {
            return;
        }

        int minX = Math.max(spawnX - platformRadius, chunkX << 4);
        int maxX = Math.min(spawnX + platformRadius, (chunkX << 4) + 15);
        int minZ = Math.max(spawnZ - platformRadius, chunkZ << 4);
        int maxZ = Math.min(spawnZ + platformRadius, (chunkZ << 4) + 15);

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                chunkData.setBlock(x & 15, platformY, z & 15, Material.BARRIER);
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
