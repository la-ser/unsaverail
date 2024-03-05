package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class LobbyHouse implements Listener {
    private final ArdaGone plugin;
    private CharacterManager characterManager;
    private FileConfiguration config;

    public LobbyHouse(ArdaGone plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        reloadConfig();
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
        removeArmorStand(player);
        deleteHouse(player);
    }

    private void generateHouse(Player player) {
        World world = player.getWorld();
        Location houseLocation = findFreeLocation(world);
        if (houseLocation != null) {
            Location structureBlockLocation = new Location(world, houseLocation.getX(), houseLocation.getY() + 1, houseLocation.getZ());
            structureBlockLocation.getBlock().setType(Material.STRUCTURE_BLOCK);
            BlockState blockState = structureBlockLocation.getBlock().getState();
            if (blockState instanceof Structure) {
                Structure structureBlock = (Structure) blockState;
                structureBlock.setStructureName("factory_1");
                structureBlock.setRelativePosition(new BlockVector(0, 0, 0));
                structureBlock.update();
            }

            houseLocation.getBlock().setType(Material.AIR); // nerviges blockupdate
            houseLocation.getBlock().setType(Material.REDSTONE_BLOCK);

            Location playerHouseLocation = houseLocation.clone().add(11, 2, 11.5);
            playerHouseLocation.setYaw(130);
            playerHouseLocation.setPitch(0);
            player.teleport(playerHouseLocation);

            // Save player UUID and armor stand UUID in configuration
            String playerPath = "houses." + world.getName() + "." + houseLocation.getBlockX() + "." + houseLocation.getBlockY() + "." + houseLocation.getBlockZ();
            config.set(playerPath + ".user", player.getUniqueId().toString());
            saveConfig();

            // Summon armor stand with player head
            ArmorStand armorStand = summonArmorStand(player, houseLocation);
            if (armorStand != null) {
                String armorStandPath = playerPath + ".armorstand";
                config.set(armorStandPath, armorStand.getUniqueId().toString());
                saveConfig();
            }
        }
    }

    private void deleteHouse(Player player) {
        UUID playerUUID = player.getUniqueId();
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
                                        String playerId = config.getString("houses." + worldName + "." + xStr + "." + yStr + "." + zStr + ".user");
                                        if (playerId != null && playerId.equals(playerUUID.toString())) {
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
                                            String path = "houses." + worldName + "." + xStr + "." + yStr + "." + zStr;
                                            config.set(path, null);
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

    private ArmorStand summonArmorStand(Player player, Location location) {
        Location armorstandSpawnLocation = location.clone().add(new Vector(3.5, 2, 9.5));
        armorstandSpawnLocation.setYaw(-90);
        armorstandSpawnLocation.setPitch(0);

        ArmorStand armorStand = location.getWorld().spawn(armorstandSpawnLocation, ArmorStand.class);
        armorStand.setCustomName("§r§e" + player.getName() + "'s House");
        armorStand.setCustomNameVisible(true);
        /*ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.setOwningPlayer(player);
        playerHead.setItemMeta(playerHeadMeta);
        armorStand.setHelmet(playerHead);*/
        ItemStack characterItem = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta characterItemMeta = characterItem.getItemMeta();
        characterItemMeta.setCustomModelData(200);
        characterItem.setItemMeta(characterItemMeta);
        armorStand.setHelmet(characterItem);
        armorStand.setBasePlate(false);
        armorStand.setVisible(false);
        armorStand.setSmall(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
        armorStand.addScoreboardTag("character_selector");

        // Save armor stand UUID to config
        String path = "houses." + location.getWorld().getName() + "." + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ() + ".armorstand";
        config.set(path, armorStand.getUniqueId().toString());
        saveConfig();
        return armorStand;
    }

    private void removeArmorStand(Player player) {
        Location houseLocation = getPlayerHouseLocation(player);
        if (houseLocation != null) {
            String path = "houses." + houseLocation.getWorld().getName() + "." + houseLocation.getBlockX() + "." + houseLocation.getBlockY() + "." + houseLocation.getBlockZ() + ".armorstand";
            String armorStandUUID = config.getString(path);

            UUID armorstandUUID = UUID.fromString(armorStandUUID);
            killEntityByUUID(armorstandUUID);
        }
    }

    private void killEntityByUUID(UUID entityUUID) {
        World world = Bukkit.getWorlds().get(0);
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(entityUUID)) {
                entity.remove();
                break;
            }
        }
    }

    private Location getPlayerHouseLocation(Player player) {
        World world = player.getWorld();
        for (String xStr : config.getConfigurationSection("houses." + world.getName()).getKeys(false)) {
            int x = Integer.parseInt(xStr);
            for (String yStr : config.getConfigurationSection("houses." + world.getName() + "." + xStr).getKeys(false)) {
                int y = Integer.parseInt(yStr);
                for (String zStr : config.getConfigurationSection("houses." + world.getName() + "." + xStr + "." + yStr).getKeys(false)) {
                    int z = Integer.parseInt(zStr);
                    String userUUID = config.getString("houses." + world.getName() + "." + xStr + "." + yStr + "." + zStr + ".user");
                    if (userUUID != null && userUUID.equals(player.getUniqueId().toString())) {
                        return new Location(world, x, y, z);
                    }
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

    private void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "houses.yml");
        config = plugin.getConfig();
        if (!configFile.exists()) {
            plugin.saveResource("houses.yml", false);
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    @EventHandler
    public void onArmorStandClick(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            ArmorStand clickedArmorStand = (ArmorStand) event.getRightClicked();
            if (clickedArmorStand.getScoreboardTags().contains("character_selector")) {
                Player player = event.getPlayer();
                characterManager.openUnlockedCharactersGUI(player);
            }
        }
    }
}
