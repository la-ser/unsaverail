package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArdaGone extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("test").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (command.getName().equals("test")) {
            openCharacterSelectionGUI(player);
        }
        return true;
    }

    // Method to open the character selection GUI for a player
    private void openCharacterSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "Character Selection");

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
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.equals("Character Selection")) {
            event.setCancelled(true); // Prevent players from moving items around in the GUI

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                // Handle selection based on the clicked item
                // For example, you could give the player permissions, set their character, etc.
                player.sendMessage("You selected: " + clickedItem.getType().toString());
            }
        }
    }
}
