package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageSystem implements Listener {
    private final ArdaGone plugin;
    private final Characters characters;

    public DamageSystem(ArdaGone plugin, Characters characters) {
        this.plugin = plugin;
        this.characters = characters;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (event.getEntity().isInvulnerable()) return;

            Player player = (Player) event.getDamager();
            Player targetPlayer = (Player) event.getEntity();
            int playerCharID = characters.getCharacter(player);
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon.getItemMeta().getCustomModelData() != 1) return;

            if (playerCharID == 0) {
                player.sendMessage("You have attacked as Henry!");
                targetPlayer.sendMessage("You have been attacked by Henry!");
                double targetPlayerDamageHealth = targetPlayer.getHealth() - PConfig.HenryDamageAmount;
                if (targetPlayerDamageHealth < 1) {
                    targetPlayerDamageHealth = 0;
                }
                double finalTargetPlayerDamageHealth = targetPlayerDamageHealth;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    targetPlayer.setHealth(finalTargetPlayerDamageHealth);
                }, 2);
            } else if (playerCharID == 1) {
                player.sendMessage("You have attacked as Robin!");
                targetPlayer.sendMessage("You have been attacked by Robin!");
                double targetPlayerDamageHealth = targetPlayer.getHealth() - PConfig.RobinDamageAmount;
                if (targetPlayerDamageHealth < 1) {
                    targetPlayerDamageHealth = 0;
                }
                double finalTargetPlayerDamageHealth = targetPlayerDamageHealth;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    targetPlayer.setHealth(finalTargetPlayerDamageHealth);
                }, 2);
            } else if (playerCharID == 2) {
                player.sendMessage("You have attacked as Thorn!");
                targetPlayer.sendMessage("You have been attacked by Thorn!");
                double targetPlayerDamageHealth = targetPlayer.getHealth() - PConfig.ThornDamageAmount;
                if (targetPlayerDamageHealth < 1) {
                    targetPlayerDamageHealth = 0;
                }
                double finalTargetPlayerDamageHealth = targetPlayerDamageHealth;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    targetPlayer.setHealth(finalTargetPlayerDamageHealth);
                }, 2);
            }
        }
    }
}
