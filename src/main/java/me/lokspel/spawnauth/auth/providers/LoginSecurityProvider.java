package me.lokspel.spawnauth.auth.providers;

import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.events.AuthActionEvent;
import com.lenis0012.bukkit.loginsecurity.session.AuthActionType;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import me.lokspel.spawnauth.SpawnAuth;
import me.lokspel.spawnauth.auth.AuthManager;
import me.lokspel.spawnauth.auth.AuthProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class LoginSecurityProvider implements AuthProvider, Listener {

    private AuthManager authManager;

    @Override
    public String getName() {
        return "LoginSecurity";
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (player == null) return false;
        try {
            PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
            return session != null && session.isLoggedIn();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isRegistered(Player player) {
        if (player == null) return false;
        try {
            PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
            return session != null && session.isRegistered();
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
    public void onAuthAction(AuthActionEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        AuthActionType type = event.getType();
        if (type == AuthActionType.LOGIN) {
            authManager.handleAuthenticated(player, false);
        } else if (type == AuthActionType.REGISTER) {
            authManager.handleAuthenticated(player, true);
        } else if (type == AuthActionType.LOGOUT || type == AuthActionType.REMOVE_PASSWORD) {
            authManager.handleLoggedOut(player);
        }
    }
}
