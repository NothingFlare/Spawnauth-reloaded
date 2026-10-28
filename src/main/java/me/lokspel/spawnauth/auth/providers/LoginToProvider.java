package me.lokspel.spawnauth.auth.providers;

import com.github.yager400.loginto.api.LoginTo;
import com.github.yager400.loginto.api.events.PlayerAccountDeletionEvent;
import com.github.yager400.loginto.api.events.PlayerLoginEvent;
import com.github.yager400.loginto.api.events.PlayerRegistrationEvent;
import com.github.yager400.loginto.common.players.Sessions;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.auth.AuthProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginToProvider implements AuthProvider, Listener {

    private AuthManager authManager;
    private SpawnAuth plugin;

    @Override
    public String getName() {
        return "LoginTo";
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (player == null) return false;
        try {
            return Sessions.isPlayerLogged(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(Player player) {
        if (player == null) return false;
        try {
            var db = com.github.yager400.loginto.bukkit.LoginTo.getDatabase();
            return db != null && db.databaseContainsPlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void registerListeners(SpawnAuth plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        try {
            var eventManager = LoginTo.getEventManager();
            eventManager.register(PlayerRegistrationEvent.class, event -> {
                Player player = Bukkit.getPlayer(event.getPlayerUUID());
                if (player != null) {
                    authManager.handleAuthenticated(player, true);
                }
            });

            eventManager.register(PlayerLoginEvent.class, event -> {
                Player player = Bukkit.getPlayer(event.getPlayerUUID());
                if (player != null) {
                    authManager.handleAuthenticated(player, false);
                }
            });

            eventManager.register(PlayerAccountDeletionEvent.class, event -> {
                Player player = Bukkit.getPlayer(event.getPlayerUUID());
                if (player != null) {
                    authManager.handleLoggedOut(player);
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register LoginTo custom event listeners: " + t.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline() && isAuthenticated(player)) {
                authManager.handleAuthenticated(player, false);
            }
        });
    }
}
