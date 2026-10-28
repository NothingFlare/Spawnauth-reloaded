package me.lokspel.spawnauth.commands;

import me.lokspel.spawnauth.SpawnAuth;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SpawnAuthCommand implements CommandExecutor, TabCompleter {

    private final SpawnAuth plugin;

    public SpawnAuthCommand(SpawnAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("spawnauth.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "[SpawnAuth] Configuration reloaded successfully.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "--- SpawnAuth v" + plugin.getDescription().getVersion() + " ---");
        sender.sendMessage(ChatColor.YELLOW + "/spawnauth reload " + ChatColor.GRAY + "- Reload plugin configuration");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
