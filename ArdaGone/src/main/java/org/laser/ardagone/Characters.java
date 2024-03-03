package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Characters implements Listener {

    private final ArdaGone plugin;
    private final File configfFile;
    private FileConfiguration config;
    private final HashMap<Player, Long> dashCooldowns;

    public Characters(ArdaGone plugin) {
        this.plugin = plugin;
        this.configfFile = new File(plugin.getDataFolder(), "char.yml");
        this.config = YamlConfiguration.loadConfiguration(configfFile);
        this.dashCooldowns = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setPlayerHealth(Player player, int health) {
        if (health <= 1) return;
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(health);
            player.setHealth(health);
        }
        player.setHealthScale(20);
        player.setHealthScaled(true);
    }

    public void selectCharacter(Player player, int charID) {
        ConfigurationSection characterSection = config.getConfigurationSection("char");
        if (characterSection == null) {
            characterSection = config.createSection("char");
        }

        if (charID == 0) {
            player.sendMessage("You have selected 'Henry'!");
            characterSection.set(player.getUniqueId().toString(), 0);
        } else if (charID == 1) {
            player.sendMessage("You have selected 'Robin'!");
            characterSection.set(player.getUniqueId().toString(), 1);
        } else if (charID == 2) {
            player.sendMessage("You have selected 'Thorn'!");
            characterSection.set(player.getUniqueId().toString(), 2);
        } else if (charID == 3) {
            player.sendMessage("You have selected 'Batty'!");
            characterSection.set(player.getUniqueId().toString(), 3);
        }
        else {
            player.sendMessage("Err selecting character!");
            return;
        }

        equipCharacter(player, charID);
        saveConfig();
    }

    public int getCharacter(Player player) {
        ConfigurationSection characterSection = config.getConfigurationSection("char");
        if (characterSection == null) {
            return -1;
        }

        String uuidString = player.getUniqueId().toString();
        if (characterSection.contains(uuidString)) {
            return characterSection.getInt(uuidString);
        } else {
            return -1;
        }
    }

    private void equipCharacter(Player player, int charID) {
        player.getInventory().clear();

        if (charID == 0) {

            setPlayerHealth(player, 1000);
            ItemStack henrySword = new ItemStack(Material.STONE_SWORD);
            ItemMeta henrySwordMeta = henrySword.getItemMeta();
            henrySwordMeta.setDisplayName("§cSword");
            henrySwordMeta.setUnbreakable(true);
            henrySwordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            henrySwordMeta.setCustomModelData(1);
            henrySwordMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
/*
            henrySwordMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE.name(), 230, AttributeModifier.Operation.ADD_NUMBER));
*/
            henrySword.setItemMeta(henrySwordMeta);
            player.getInventory().setItem(0, henrySword);

        } else if (charID == 1) {

            setPlayerHealth(player, 650);
            ItemStack robinBow = new ItemStack(Material.BOW);
            ItemMeta robinBowMeta = robinBow.getItemMeta();
            robinBowMeta.setDisplayName("§eBow");
            robinBowMeta.setUnbreakable(true);
            robinBowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            robinBowMeta.setCustomModelData(1);
            robinBow.setItemMeta(robinBowMeta);
            player.getInventory().setItem(0, robinBow);

        } else if (charID == 2) {

            setPlayerHealth(player, 850);
            ItemStack thornSword = new ItemStack(Material.WOODEN_SWORD);
            ItemMeta thornSwordMeta = thornSword.getItemMeta();
            thornSwordMeta.setDisplayName("§cThornsword");
            thornSwordMeta.setUnbreakable(true);
            thornSwordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            thornSwordMeta.setCustomModelData(1);
            thornSwordMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
/*
            thornSwordMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE.name(), 130, AttributeModifier.Operation.ADD_NUMBER));
*/
            thornSword.setItemMeta(thornSwordMeta);
            player.getInventory().setItem(0, thornSword);
        } else if (charID == 3) {

            setPlayerHealth(player, 850);
            ItemStack bettyBat = new ItemStack(Material.STICK);
            ItemMeta bettyBatMeta = bettyBat.getItemMeta();
            bettyBatMeta.setDisplayName("§7Batty's Bat");
            bettyBatMeta.setUnbreakable(true);
            bettyBatMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            bettyBatMeta.setCustomModelData(1);
            bettyBatMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
/*
            thornSwordMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE.name(), 130, AttributeModifier.Operation.ADD_NUMBER));
*/
            bettyBat.setItemMeta(bettyBatMeta);
            player.getInventory().setItem(0, bettyBat);
        }
    }

    private void saveConfig() {
        try {
            config.save(configfFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Err saving the config: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        int playerCharID = getCharacter(player);

        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (mainHandItem == null || mainHandItem.getItemMeta() == null || !mainHandItem.getItemMeta().hasCustomModelData()) return;

            if (playerCharID == 0) {
                if (!CooldownManager.isOnCooldown(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 5, 2), false);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1), false);

                    CooldownManager.setCooldown(player, 7500);
                } else {
                    player.sendMessage("Ability is on cooldown! " + CooldownManager.getRemainingCooldown(player) / 1000 + " seconds remaining.");
                }
            }

            if (playerCharID == 1) {
                Location playerLocation = player.getLocation();
                World world = player.getWorld();

                if (player.getLocation().subtract(0, 1, 0).getBlock().getType() == Material.AIR) {
                    if (player.getLocation().subtract(0, 2, 0).getBlock().getType() == Material.AIR) {
                        return;
                    }
                    playerLocation = playerLocation.subtract(new Vector(0,1,0));
                }

                if (!CooldownManager.isOnCooldown(player) && !player.isSneaking()) {
                    player.setVelocity(player.getVelocity().setY(1.35));
                    Location finalPlayerLocation = playerLocation;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (int i = 1; i <= 7; i++) {
                                Block block = world.getBlockAt(finalPlayerLocation.getBlockX(), finalPlayerLocation.getBlockY() + i -1, finalPlayerLocation.getBlockZ());
                                block.setType(Material.OAK_LOG);
                            }

                            new BukkitRunnable() {
                                int count = 7;

                                @Override
                                public void run() {
                                    if (count >= 0) {
                                        Block block = world.getBlockAt(finalPlayerLocation.getBlockX(), finalPlayerLocation.getBlockY() + count, finalPlayerLocation.getBlockZ());
                                        block.setType(Material.AIR);
                                        count--;
                                    } else {
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(plugin, 80, 3);
                        }
                    }.runTaskLater(plugin, 13);

                    CooldownManager.setCooldown(player, 7500);
                } else {
                    player.sendMessage("Ability is on cooldown! " + CooldownManager.getRemainingCooldown(player) / 1000 + " seconds remaining.");
                }
            } else if (playerCharID == 2) {
                if (!CooldownManager.isOnCooldown(player)) {
                    player.sendMessage("Thats too thorny!");
                    CooldownManager.setCooldown(player, 7500);
                } else {
                    player.sendMessage("Ability is on cooldown! " + CooldownManager.getRemainingCooldown(player) / 1000 + " seconds remaining.");
                }
            } else if (playerCharID == 3) {
                if (!CooldownManager.isOnCooldown(player)) {
                    player.sendMessage("Some bats would be spawning now.");

                    CooldownManager.setCooldown(player, 7500);
                } else {
                    player.sendMessage("Ability is on cooldown! " + CooldownManager.getRemainingCooldown(player) / 1000 + " seconds remaining.");
                }
            }
        }
    }

    // ABILITIES
    public static class CooldownManager {
        private static final HashMap<Player, Long> abilityCooldowns = new HashMap<>();

        public static boolean isOnCooldown(Player player) {
            if (abilityCooldowns.containsKey(player)) {
                long currentTime = System.currentTimeMillis();
                long cooldownExpiration = abilityCooldowns.get(player);
                return currentTime < cooldownExpiration;
            }
            return false;
        }

        public static void setCooldown(Player player, long cooldownMillis) {
            long currentTime = System.currentTimeMillis();
            long cooldownExpiration = currentTime + cooldownMillis;
            abilityCooldowns.put(player, cooldownExpiration);
        }

        public static long getRemainingCooldown(Player player) {
            if (abilityCooldowns.containsKey(player)) {
                long currentTime = System.currentTimeMillis();
                long cooldownExpiration = abilityCooldowns.get(player);
                if (currentTime < cooldownExpiration) {
                    return cooldownExpiration - currentTime;
                }
            }
            return 0;
        }
    }
}
