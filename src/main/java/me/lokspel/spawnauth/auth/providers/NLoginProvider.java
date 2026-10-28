package me.lokspel.spawnauth.auth.providers;

import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.Identity;
import com.nickuc.login.api.event.bukkit.auth.LoginEvent;
import com.nickuc.login.api.event.bukkit.auth.RegisterEvent;
import com.nickuc.login.api.event.bukkit.command.UnregisterEvent;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.auth.AuthProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class NLoginProvider implements AuthProvider, Listener {

    private AuthManager authManager;

    @Override
    public String getName() {
        return "nLogin";
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (player == null) return false;
        try {
            return nLoginAPI.getApi().isAuthenticated(player.getName());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(Player player) {
        if (player == null) return false;
        try {
            return nLoginAPI.getApi().getAccount(Identity.ofKnownName(player.getName())).isPresent();
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
    public void onUnregister(UnregisterEvent event) {
        authManager.handleLoggedOut(event.getPlayer());
    }
}
