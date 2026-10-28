package me.lokspel.spawnauth.auth.providers;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.events.RegisterEvent;
import fr.xephi.authme.events.UnregisterByAdminEvent;
import fr.xephi.authme.events.UnregisterByPlayerEvent;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.auth.AuthProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuthMeProvider implements AuthProvider, Listener {

    private AuthManager authManager;

    @Override
    public String getName() {
        return "AuthMe";
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (player == null) return false;
        try {
            return AuthMeApi.getInstance().isAuthenticated(player);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(Player player) {
        if (player == null) return false;
        try {
            return AuthMeApi.getInstance().isRegistered(player.getName());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void registerListeners(SpawnAuth plugin, AuthManager authManager) {
        this.authManager = authManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogin(LoginEvent event) {
        authManager.handleAuthenticated(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegister(RegisterEvent event) {
        authManager.handleAuthenticated(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogout(LogoutEvent event) {
        authManager.handleLoggedOut(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnregisterByPlayer(UnregisterByPlayerEvent event) {
        authManager.handleLoggedOut(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnregisterByAdmin(UnregisterByAdminEvent event) {
        authManager.handleLoggedOut(event.getPlayer());
    }
}
