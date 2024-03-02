package org.laser.ardagone;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SpawnPoints implements CommandExecutor {

    private final JavaPlugin plugin;
    private final FileConfiguration spawnConfig;
    private final File spawnFile;

    public SpawnPoints(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawnConfig = plugin.getConfig();
        this.spawnFile = new File(plugin.getDataFolder(), "spawnpoints.yml");
        if (!spawnFile.exists()) {
            plugin.saveResource("spawnpoints.yml", false);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return false;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("savespawnpoint")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /savespawnpoint <spawnpoint>");
                return false;
            }

            int spawnPoint;
            try {
                spawnPoint = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid spawnpoint number.");
                return false;
            }

            spawnConfig.set("SpawnPoints." + spawnPoint + ".x", player.getLocation().getX());
            spawnConfig.set("SpawnPoints." + spawnPoint + ".y", player.getLocation().getY());
            spawnConfig.set("SpawnPoints." + spawnPoint + ".z", player.getLocation().getZ());

            try {
                spawnConfig.save(spawnFile);
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage("Error saving spawnpoint.");
                return false;
            }

            player.sendMessage("Spawnpoint " + spawnPoint + " saved.");
            return true;
        } else if (label.equalsIgnoreCase("randomspawn")) {
            randomSpawn(player);
            return true;
        }

        return false;
    }

    public void randomSpawn(Player player) {
        if (spawnConfig.getConfigurationSection("SpawnPoints") == null || spawnConfig.getConfigurationSection("SpawnPoints").getKeys(false).isEmpty()) {
            player.sendMessage("No spawnpoints saved.");
            return;
        }

        List<String> spawnList = new ArrayList<>(spawnConfig.getConfigurationSection("SpawnPoints").getKeys(false));
        Random random = new Random();
        String randomSpawn = spawnList.get(random.nextInt(spawnList.size()));
        double x = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".x");
        double y = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".y");
        double z = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".z");

        player.teleport(new Location(player.getWorld(), x, y, z));
        player.sendMessage("Teleported to a random spawnpoint.");
    }


    public void randomSpawnAll(Set<Player> playersInLobby) { // hier müssen wir einen fehler finden
        if (spawnConfig.getConfigurationSection("SpawnPoints") == null || spawnConfig.getConfigurationSection("SpawnPoints").getKeys(false).isEmpty()) {
            for (Player player : playersInLobby) {
                player.sendMessage("No spawnpoints saved.");
            }
            return;
        }

        List<String> spawnList = new ArrayList<>(spawnConfig.getConfigurationSection("SpawnPoints").getKeys(false));
        Random random = new Random();
        String randomSpawn = spawnList.get(random.nextInt(spawnList.size()));
        double x = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".x");
        double y = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".y");
        double z = spawnConfig.getDouble("SpawnPoints." + randomSpawn + ".z");

        for (Player player : playersInLobby) {
            player.teleport(new Location(player.getWorld(), x, y, z));
            player.sendMessage("Teleported to a random spawnpoint.");
        }
    }

}
