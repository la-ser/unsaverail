package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatusManager implements CommandExecutor, Listener {

    private final ArdaGone plugin;

    private File selectedCharsFile;
    private FileConfiguration selectedCharsConfig;

    public StatusManager(ArdaGone plugin) {
        this.plugin = plugin;

        statusFile = new File(plugin.getDataFolder(), "status.yml");
        if (!statusFile.exists()) {
            plugin.saveResource("status.yml", false);
        }
        statusConfig = YamlConfiguration.loadConfiguration(statusFile);
    }

    private FileConfiguration statusConfig;
    private File statusFile;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setStatus") && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                String status = ChatColor.translateAlternateColorCodes('&', args[1]);
                setStatus(target, status);
                sender.sendMessage("Status for " + target.getName() + " set to: " + status);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found.");
            }
            return true;
        }
        return false;
    }

    public void setStatus(Player player, String status) {
        statusConfig.set(player.getUniqueId().toString(), status);
        saveStatusConfig();
    }

    public String getStatus(Player player) {
        return statusConfig.getString(player.getUniqueId().toString());
    }

    public boolean hasAnyStatus(Player player) {
        return statusConfig.contains(player.getUniqueId().toString());
    }

    public boolean isStatus(Player player, String status) {
        String playerStatus = getStatus(player);
        return playerStatus != null && playerStatus.equals(status);
    }

    public void removeStatus(Player player) {
        statusConfig.set(player.getUniqueId().toString(), null);
        saveStatusConfig();
    }

    public List<Player> getPlayersWithStatus(String targetStatus) {
        List<Player> playersWithStatus = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerStatus = getStatus(player);
            if (playerStatus != null && playerStatus.equals(targetStatus)) {
                playersWithStatus.add(player);
            }
        }

        return playersWithStatus;
    }

    /*public void sendMessageToObamaPlayers() {
        String targetStatus = "obama";
        List<Player> obamaPlayers = getPlayersWithStatus(targetStatus);

        for (Player player : obamaPlayers) {
            player.sendMessage("Hello, Obama player!");
        }
    }*/

    private void saveStatusConfig() {
        try {
            statusConfig.save(statusFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save status.yml!");
        }
    }
}