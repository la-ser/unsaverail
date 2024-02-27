package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import static org.bukkit.Bukkit.getServer;

public class PlayerJoinEventHandler implements Listener {

    private final ArdaGone plugin;

    private final MoneyManager moneyManager;

    public PlayerJoinEventHandler(ArdaGone plugin) {
        this.plugin = plugin;
        this.moneyManager = new MoneyManager(plugin);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.LIGHT_PURPLE + "\n» Welcome, " + player.getName() + "!\n ");
        event.setJoinMessage("");

        resetPlayer(player);

        for (int slot = 9; slot <= 35; slot++) {
            ItemStack itemStack = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(" ");
            itemStack.setItemMeta(itemMeta);
            player.getInventory().setItem(slot, itemStack);
        }
    }

    public void resetPlayer(Player player) {
        World defaultWorld = getServer().getWorld("world");
        Location spawnLocation = defaultWorld.getSpawnLocation();
        double x = spawnLocation.getX() + 0.5;
        double y = spawnLocation.getY();
        double z = spawnLocation.getZ() + 0.5;
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        Location WORLDSPAWN = new Location(defaultWorld, x, y, z, yaw, pitch);
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        player.teleport(WORLDSPAWN);
        player.setRotation(0, 0);
        player.setFoodLevel(20);
        player.setHealthScale(20);
        player.setHealthScaled(true);
        player.setHealth(maxHealth);
        player.setFireTicks(0);

        double starterHealth = 20;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(starterHealth);
        player.setHealth(starterHealth);
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setExp(0);
        player.setLevel(0);

        player.teleport(WORLDSPAWN);
        player.setGameMode(GameMode.ADVENTURE);
        player.setRotation(90, 0);

        moneyManager.addMoney(player.getName(), 0);
    }
}
