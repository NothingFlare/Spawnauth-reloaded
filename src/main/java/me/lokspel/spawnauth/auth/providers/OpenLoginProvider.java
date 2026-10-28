package me.lokspel.spawnauth.auth.providers;

import com.nickuc.openlogin.bukkit.OpenLoginBukkit;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.auth.AuthProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class OpenLoginProvider implements AuthProvider, Listener {

    private AuthManager authManager;
    private OpenLoginBukkit openLoginBukkit;

    @Override
    public String getName() {
        return "OpenLogin";
    }

    private OpenLoginBukkit getOpenLogin() {
        if (openLoginBukkit == null) {
            Plugin p = Bukkit.getPluginManager().getPlugin("OpeNLogin");
            if (p instanceof OpenLoginBukkit olb) {
                this.openLoginBukkit = olb;
            }
        }
        return openLoginBukkit;
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (player == null) return false;
        try {
            OpenLoginBukkit plugin = getOpenLogin();
            return plugin != null && plugin.getLoginManagement().isAuthenticated(player.getName());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(Player player) {
        if (player == null) return false;
        try {
            return OpenLoginBukkit.getApi().isRegistered(player.getName());
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
    public void onAsyncLogin(AsyncLoginEvent event) {
        authManager.handleAuthenticated(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncRegister(AsyncRegisterEvent event) {
        authManager.handleAuthenticated(event.getPlayer(), true);
    }
}
