package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootBoxes implements CommandExecutor, Listener {

    private final ArdaGone plugin;

    private final MoneyManager moneyManager;

    public LootBoxes(ArdaGone plugin) {
        this.plugin = plugin;
        this.moneyManager = new MoneyManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /givecrate <crateType>");
            return true;
        }

        try {
            int crateType = Integer.parseInt(args[0]);
            Player player = (Player) sender;
            placeCrate(player, crateType);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid crate type.");
        }

        return true;
    }

    private void placeCrate(Player player, int crateType) {
        Location location = player.getLocation().getBlock().getLocation();
        Block block = location.getBlock();

        if (block.getType() == Material.BARREL) {
            player.sendMessage("A barrel already exists here!");
            return;
        }

        block.setType(Material.BARREL);
        Barrel barrel = (Barrel) block.getState();
        Inventory crateInventory = barrel.getInventory();

        switch (crateType) {
            case 1:
/*
                crateInventory.addItem(new ItemStack(Material.DIAMOND, 3));
*/
                break;
            case 2:
/*
                crateInventory.addItem(new ItemStack(Material.GOLD_INGOT, 5));
*/
                break;
            default:
                player.sendMessage("Invalid crate type.");
                return;
        }

        File lootBoxFile = new File(plugin.getDataFolder(), "lootboxes.yml");
        FileConfiguration lootBoxConfig = YamlConfiguration.loadConfiguration(lootBoxFile);

        lootBoxConfig.set("crates." + crateType + ".x", location.getBlockX());
        lootBoxConfig.set("crates." + crateType + ".y", location.getBlockY());
        lootBoxConfig.set("crates." + crateType + ".z", location.getBlockZ());

        try {
            lootBoxConfig.save(lootBoxFile);
        } catch (IOException e) {
            player.sendMessage("An error occurred while saving the configuration.");
            e.printStackTrace();
        }

        player.sendMessage("Crate placed!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.BARREL) {
            Location location = block.getLocation();
            File lootBoxFile = new File(plugin.getDataFolder(), "lootboxes.yml");
            FileConfiguration lootBoxConfig = YamlConfiguration.loadConfiguration(lootBoxFile);
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            if (lootBoxConfig.contains("crates")) {
                for (String crateType : lootBoxConfig.getConfigurationSection("crates").getKeys(false)) {
                    int crateX = lootBoxConfig.getInt("crates." + crateType + ".x");
                    int crateY = lootBoxConfig.getInt("crates." + crateType + ".y");
                    int crateZ = lootBoxConfig.getInt("crates." + crateType + ".z");

                    if (x == crateX && y == crateY && z == crateZ) {
                        lootBoxConfig.set("crates." + crateType, null);
                        try {
                            lootBoxConfig.save(lootBoxFile);
                        } catch (IOException e) {
                            event.getPlayer().sendMessage("An error occurred while saving the configuration.");
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    private List<ItemStack> customItemList() {
        List<ItemStack> lootItems = new ArrayList<>();
        /*lootItems.add(new ItemStack(Material.DIAMOND));
        lootItems.add(new ItemStack(Material.GOLD_INGOT));
        lootItems.add(new ItemStack(Material.IRON_INGOT));

        Material carrotMaterial = Material.CARROT;
        ItemStack enchantedCarrot = new ItemStack(carrotMaterial);
        enchantedCarrot.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 1);
        lootItems.add(enchantedCarrot);*/

        for (int slot = 0; slot < 26; slot++) {
            ItemStack itemWithNBT = addCustomNBT(getItemInChestSlot(slot), "value", ""+slot);

            lootItems.add(itemWithNBT);
        }

        return lootItems;
    }

    public ItemStack getItemInChestSlot(int slotNumber) {
        Location chestLocation = new Location(Bukkit.getWorld("world"), -17, 103, -1);

        Block block = chestLocation.getBlock();

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory chestInventory = chest.getInventory();

            if (slotNumber >= 0 && slotNumber < chestInventory.getSize()) {
                ItemStack itemInSlot = chestInventory.getItem(slotNumber);
                return itemInSlot;
            }
        }

        return null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BARREL) {
            Player player = event.getPlayer();
            Location location = event.getClickedBlock().getLocation();

            File lootBoxFile = new File(plugin.getDataFolder(), "lootboxes.yml");
            FileConfiguration lootBoxConfig = YamlConfiguration.loadConfiguration(lootBoxFile);

            for (String crateType : lootBoxConfig.getConfigurationSection("crates").getKeys(false)) {
                int crateX = lootBoxConfig.getInt("crates." + crateType + ".x");
                int crateY = lootBoxConfig.getInt("crates." + crateType + ".y");
                int crateZ = lootBoxConfig.getInt("crates." + crateType + ".z");

                if (location.getBlockX() == crateX && location.getBlockY() == crateY && location.getBlockZ() == crateZ) {
                    Inventory customInventory = Bukkit.createInventory(null, LOOT_BOX_SIZE, "Loot Box");

                    List<ItemStack> shuffledLoot = shuffleItems(customItemList());

                    fillLootBox(customInventory, shuffledLoot);

                    player.openInventory(customInventory);
                    event.setCancelled(true);

                    startRotationAnimation(player, customInventory, shuffledLoot, 7);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null) {
            Player player = (Player) event.getWhoClicked();

            if (event.getView().getTitle().equals("Loot Box")) {
                event.setCancelled(true);
            }
        }
    }

    private void startRotationAnimation(Player player, Inventory inventory, List<ItemStack> possibleLoot, int animationDurationSeconds) {
        int totalFrames = (animationDurationSeconds * 20) / ROTATION_DELAY_TICKS;

        new BukkitRunnable() {
            int currentIndex = 0;
            Random random = new Random();
            int framesPassed = 0;

            @Override
            public void run() {
                for (int i = 0; i < LOOT_BOX_SIZE - 1; i++) {
                    ItemStack nextItem = inventory.getItem(i + 1);
                    inventory.setItem(i, nextItem);
                }

                int randomIndex = random.nextInt(possibleLoot.size());
                ItemStack randomItem = possibleLoot.get(randomIndex);

                inventory.setItem(LOOT_BOX_SIZE - 1, randomItem);

                currentIndex = (currentIndex + 1) % LOOT_BOX_SIZE;

                framesPassed++;

                if (framesPassed >= totalFrames) {
                    cancel();
                    ItemStack item = inventory.getItem(4);
                    String customValue = getCustomNBTValue(item, "value");
/*
                    player.getInventory().addItem(item);
*/

                    player.sendMessage("CV: " + customValue);

                    int itemAmount = item.getAmount();

                    String playerName = player.getName();

                    switch (Objects.requireNonNull(customValue)) {
                        case "0":
                            player.sendMessage("CV:0 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "1":
                            player.sendMessage("CV:1 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "2":
                            player.sendMessage("CV:2 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "3":
                            player.sendMessage("CV:3 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "4":
                            player.sendMessage("CV:4 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "5":
                            player.sendMessage("CV:5 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        case "6":
                            player.sendMessage("CV:6 " + customValue);
                            moneyManager.addMoney(playerName, itemAmount);
                            break;
                        default:
                            player.sendMessage("niete!");
                            break;
                    }

                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.1f, 2.0f);
                } else {
                    player.playSound(player, Sound.ITEM_SHIELD_BREAK, 0.1f, 2.0f);
                }
            }
        }.runTaskTimer(plugin, 0, ROTATION_DELAY_TICKS);
    }


    private List<ItemStack> shuffleItems(List<ItemStack> items) {
        List<ItemStack> shuffled = new ArrayList<>(items);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    private void fillLootBox(Inventory inventory, List<ItemStack> possibleLoot) {
        Random random = new Random();

        for (int i = 0; i < inventory.getSize(); i++) {
            int randomIndex = random.nextInt(possibleLoot.size());
            ItemStack randomItem = possibleLoot.get(randomIndex);

            inventory.setItem(i, randomItem);
        }
    }

    private static final int LOOT_BOX_SIZE = 9;
    private static final int ROTATION_DELAY_TICKS = 10;

    private ItemStack addCustomNBT(ItemStack itemStack, String key, String value) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        ItemStack newItem = new ItemStack(itemStack.getType(), itemStack.getAmount());

        ItemMeta newMeta = itemMeta.clone();
        newMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        newItem.setItemMeta(newMeta);

        return newItem;
    }

    private String getCustomNBTValue(ItemStack itemStack, String key) {
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, key), PersistentDataType.STRING)) {
                return itemMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
            }
        }

        // Return null if the item doesn't have the specified key
        return null;
    }
}
