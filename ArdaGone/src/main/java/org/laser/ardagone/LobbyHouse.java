package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LobbyHouse implements Listener {
    private final Plugin plugin;
    private final Map<Player, Location> playerHouses;
    private final FileConfiguration config;

    public LobbyHouse(Plugin plugin) {
        this.plugin = plugin;
        this.playerHouses = new HashMap<>();
        this.config = plugin.getConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        generateHouse(player);
        // Teleport player to their house
        Location houseLocation = playerHouses.get(player);
        if (houseLocation != null) {
            player.teleport(houseLocation);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        deleteHouse(player);
    }

    private void generateHouse(Player player) {
        World world = player.getWorld();
        Location houseLocation = findFreeLocation(world);
        if (houseLocation != null) {
            // Generate the structure block at the house location
            houseLocation.getBlock().setType(Material.STRUCTURE_BLOCK);
            Location redstoneLocation = new Location(world, houseLocation.getX(), houseLocation.getY() - 1, houseLocation.getZ());
            redstoneLocation.getBlock().setType(Material.REDSTONE_BLOCK);
            playerHouses.put(player, houseLocation);

            // Save house location to config
            config.set("houses." + player.getUniqueId().toString(), houseLocation);
            try {
                config.save(new File(plugin.getDataFolder(), "houses.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteHouse(Player player) {
        Location houseLocation = playerHouses.remove(player);
        if (houseLocation != null) {
            // Remove the structure block at the house location
            houseLocation.getBlock().setType(Material.AIR);

            // Place a gold block at the house location
            Location goldLocation = new Location(houseLocation.getWorld(), houseLocation.getX(), houseLocation.getY() - 1, houseLocation.getZ());
            goldLocation.getBlock().setType(Material.GOLD_BLOCK);

            // Remove player's house from config
            config.set("houses." + player.getUniqueId().toString(), null);
            try {
                config.save(new File(plugin.getDataFolder(), "houses.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Location findFreeLocation(World world) {
        // Start at coordinates (50, 56, 50)
        Location location = new Location(world, 50, 56, 50);
        Block block = location.getBlock();

        // Check if there's already a house at the current location
        while (block.getType() == Material.REDSTONE_BLOCK || block.getType() == Material.STRUCTURE_BLOCK) {
            // Move 25 blocks in the positive z direction
            location.add(0, 0, 25);
            block = location.getBlock();
        }

        // Return the location if it's free
        return location;
    }
}
