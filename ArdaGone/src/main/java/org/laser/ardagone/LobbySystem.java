package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;

public class LobbySystem implements CommandExecutor, Listener {
    private final ArdaGone plugin;
    private SpawnPoints spawnPoints;
    private final Set<Player> playersInLobby;
    private boolean countdownStarted;
    private int countdownSeconds;

    public LobbySystem(ArdaGone plugin, SpawnPoints spawnPoints) {
        this.plugin = plugin;
        this.spawnPoints = spawnPoints;
        this.playersInLobby = new HashSet<>();
        this.countdownStarted = false;
        this.countdownSeconds = 60;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("join").setExecutor(this);
        plugin.getCommand("leave").setExecutor(this);
        plugin.getCommand("gui").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("join")) {
            if (args.length == 0) {
                joinLobby(player);
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    joinLobby(target);
                    player.sendMessage("Added " + target.getName() + " to the lobby.");
                } else {
                    player.sendMessage("Player not found.");
                }
            } else {
                player.sendMessage("Usage: /join [player]");
            }
        } else if (command.getName().equalsIgnoreCase("leave")) {
            if (args.length == 0) {
                leaveLobby(player);
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    leaveLobby(target);
                    player.sendMessage("Removed " + target.getName() + " from the lobby.");
                } else {
                    player.sendMessage("Player not found.");
                }
            } else {
                player.sendMessage("Usage: /leave [player]");
            }
        } else if (command.getName().equalsIgnoreCase("gui")) {
            openLobbyGUI(player);
        }

        return true;
    }

    // Command to join the lobby
    public void joinLobby(Player player) {
        playersInLobby.add(player);
        player.sendMessage("You joined the lobby.");

        // Check if there are at least two players to start the countdown
        if (playersInLobby.size() >= 2 && !countdownStarted) {
            startCountdown();
        }
    }

    // Command to leave the lobby
    public void leaveLobby(Player player) {
        playersInLobby.remove(player);
        player.sendMessage("You left the lobby.");

        // Stop the countdown if there are less than two players
        if (playersInLobby.size() < 2 && countdownStarted) {
            stopCountdown();
        }
    }

    // Method to start the countdown
    private void startCountdown() {
        countdownStarted = true;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSeconds <= 0) {
                if (!countdownStarted) return; // Check if countdown has already finished
                Bukkit.broadcastMessage("Countdown finished!");
                //spawnPoints.randomSpawnAll(playersInLobby); //ERR
                stopCountdown();
            } else {
                if (countdownSeconds == 60 || countdownSeconds <= 15) {
                    Bukkit.broadcastMessage("Countdown: " + countdownSeconds + " seconds remaining.");
                }
                countdownSeconds--;
            }
        }, 0, 20); // 20 ticks = 1 second
    }


    // Method to stop the countdown
    private void stopCountdown() {
        countdownStarted = false;
        //countdownSeconds = 60; // Reset countdown time
    }

    // LOBBY GUI
    private final String lobbyGUITitle = "§6§lArdaGone";
    public void openLobbyGUI(Player player) {
        Inventory lobbyGUI = Bukkit.createInventory(null, 9*5, lobbyGUITitle);

        int[] slots = {0,1,2,3,4,5,6,7,17,26,35,36,37,38,39,40,41,42,43};
        ItemStack placeholderItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta placeholderItemMeta = placeholderItem.getItemMeta();
        placeholderItemMeta.setDisplayName(" ");
        placeholderItem.setItemMeta(placeholderItemMeta);
        setPlaceholderItemsInSlots(lobbyGUI, placeholderItem, slots);

        ItemStack bellItem = new ItemStack(Material.BELL);
        ItemMeta bellItemMeta = bellItem.getItemMeta();
        bellItemMeta.setDisplayName("§r§eNotifications");
        bellItem.setItemMeta(bellItemMeta);
        lobbyGUI.setItem(8, bellItem);

        ItemStack upgradeItem = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta upgradeItemMeta = upgradeItem.getItemMeta();
        addAllItemFlags(upgradeItemMeta);
        upgradeItemMeta.setDisplayName("§r§3Upgrade");
        upgradeItem.setItemMeta(upgradeItemMeta);
        lobbyGUI.setItem(9, upgradeItem);

        ItemStack x1 = new ItemStack(Material.BARRIER);
        ItemMeta x1Meta = x1.getItemMeta();
        addAllItemFlags(x1Meta);
        x1Meta.setDisplayName("§r§4§lX");
        x1.setItemMeta(x1Meta);
        lobbyGUI.setItem(18, x1);

        ItemStack x2 = new ItemStack(Material.BARRIER);
        ItemMeta x2Meta = x2.getItemMeta();
        addAllItemFlags(x2Meta);
        x2Meta.setDisplayName("§r§4X§l");
        x2.setItemMeta(x2Meta);
        lobbyGUI.setItem(27, x1);

        ItemStack arrowItem = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta arrowItemMeta = arrowItem.getItemMeta();
        arrowItemMeta.setDisplayName("§r§7next page");
        arrowItem.setItemMeta(arrowItemMeta);
        lobbyGUI.setItem(44, arrowItem);

        player.openInventory(lobbyGUI);
    }

    public void setPlaceholderItemsInSlots(Inventory inventory, ItemStack item, int[] slots) {
        for (int slot : slots) {
            inventory.setItem(slot, item);
        }
    }

    public void addAllItemFlags(ItemMeta meta) {
        for (ItemFlag flag : ItemFlag.values()) {
            meta.addItemFlags(flag);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(lobbyGUITitle)) {
            event.setCancelled(true);
        }
    }
}
