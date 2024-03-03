package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;

public class LobbyHouse implements Listener {
    private final Plugin plugin;
    private final FileConfiguration config;

    public LobbyHouse(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        generateHouse(player);
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
            Location structureBlockLocation = new Location(world, houseLocation.getX(), houseLocation.getY() + 1, houseLocation.getZ());
            structureBlockLocation.getBlock().setType(Material.STRUCTURE_BLOCK);
            BlockState blockState = structureBlockLocation.getBlock().getState();
            if (blockState instanceof Structure) {
                Structure structureBlock = (Structure) blockState;
                structureBlock.setStructureName("factory_1");
                structureBlock.setRelativePosition(new BlockVector(0, 0, 0));
                structureBlock.update();
            }

            houseLocation.getBlock().setType(Material.AIR);
            houseLocation.getBlock().setType(Material.REDSTONE_BLOCK);

            Location playerHouseLocation = houseLocation.clone().add(new Vector(11, 2, 11.5));
            playerHouseLocation.setYaw(130);
            playerHouseLocation.setPitch(0);
            player.teleport(playerHouseLocation);

            // Save house location to config
            String path = "houses." + world.getName() + "." + houseLocation.getBlockX() + "." + houseLocation.getBlockY() + "." + houseLocation.getBlockZ();
            config.set(path, player.getUniqueId().toString());
            saveConfig();
        }
    }

    private void deleteHouse(Player player) {
        if (config.isConfigurationSection("houses")) {
            for (String worldName : config.getConfigurationSection("houses").getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world != null && config.isConfigurationSection("houses." + worldName)) {
                    for (String xStr : config.getConfigurationSection("houses." + worldName).getKeys(false)) {
                        int x = Integer.parseInt(xStr);
                        if (config.isConfigurationSection("houses." + worldName + "." + xStr)) {
                            for (String yStr : config.getConfigurationSection("houses." + worldName + "." + xStr).getKeys(false)) {
                                int y = Integer.parseInt(yStr);
                                if (config.isConfigurationSection("houses." + worldName + "." + xStr + "." + yStr)) {
                                    for (String zStr : config.getConfigurationSection("houses." + worldName + "." + xStr + "." + yStr).getKeys(false)) {
                                        int z = Integer.parseInt(zStr);
                                        String playerId = config.getString("houses." + worldName + "." + xStr + "." + yStr + "." + zStr);
                                        if (playerId.equals(player.getUniqueId().toString())) {
                                            Location houseLocation = new Location(world, x, y, z);
                                            Location structureBlockLocation = new Location(world, houseLocation.getX(), houseLocation.getY() + 1, houseLocation.getZ());
                                            structureBlockLocation.getBlock().setType(Material.STRUCTURE_BLOCK);
                                            BlockState blockState = structureBlockLocation.getBlock().getState();
                                            if (blockState instanceof Structure) {
                                                Structure structureBlock = (Structure) blockState;
                                                structureBlock.setStructureName("factory_0");
                                                structureBlock.setRelativePosition(new BlockVector(0, 0, 0));
                                                structureBlock.update();
                                            }

                                            houseLocation.getBlock().setType(Material.AIR);
                                            houseLocation.getBlock().setType(Material.REDSTONE_BLOCK);
                                            config.set("houses." + worldName + "." + xStr + "." + yStr + "." + zStr, null);
                                            saveConfig();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Location findFreeLocation(World world) {
        for (int x = 50; x < 100; x += 25) {
            for (int z = 50; z < 100; z += 25) {
                Location location = new Location(world, x, 56, z);
                if (!config.contains("houses." + world.getName() + "." + x + "." + 56 + "." + z)) {
                    return location;
                }
            }
        }
        return null;
    }

    private void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "houses.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
