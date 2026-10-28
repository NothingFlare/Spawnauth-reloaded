package me.lokspel.spawnauth.auth;

import me.lokspel.spawnauth.SpawnAuth;
import org.bukkit.entity.Player;

public interface AuthProvider {

    String getName();

    boolean isAuthenticated(Player player);

    boolean isRegistered(Player player);

    void registerListeners(SpawnAuth plugin, AuthManager authManager);
}
