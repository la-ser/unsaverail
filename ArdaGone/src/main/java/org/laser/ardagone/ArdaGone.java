package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArdaGone extends JavaPlugin implements Listener {

    private Characters characters;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("test").setExecutor(this);

        characters = new Characters(this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("test")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openCharacterSelectionGUI(player);
            } else {
                sender.sendMessage("Only players can use this command.");
            }
            return true;
        }
        return false;
    }

    // Method to open the character selection GUI for a player
    private void openCharacterSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, InventoryType.DROPPER, "Character Selection");

        // Add buttons for each character
        ItemStack character1 = new ItemStack(Material.DIAMOND_SWORD);
        // Add any additional metadata or lore as needed
        gui.addItem(character1);

        ItemStack character2 = new ItemStack(Material.IRON_SWORD);
        // Add any additional metadata or lore as needed
        gui.addItem(character2);

        // Add more characters as needed

        player.openInventory(gui);
    }

    // Event handler for when a player clicks in the inventory
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals("Character Selection")) {
            event.setCancelled(true);
            int slotID = event.getSlot();
            characters.selectCharacter(player, slotID);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.NETHER_STAR) {
            Player player = event.getPlayer();
            openCharacterSelectionGUI(player);
        }
    }
}
