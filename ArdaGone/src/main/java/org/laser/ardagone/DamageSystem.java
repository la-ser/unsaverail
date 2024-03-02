package org.laser.ardagone;

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
            Player player = (Player) event.getDamager();
            Player targetPlayer = (Player) event.getEntity();
            int playerCharID = characters.getCharacter(player);
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (playerCharID == 0) {
                player.sendMessage("You have attacked as Henry!");
                targetPlayer.sendMessage("you have been attacked by henry");
            } else if (playerCharID == 1) {
                player.sendMessage("You have attacked as Robin!");
            } else if (playerCharID == 2) {
                player.sendMessage("You have attacked as Thorn!");
            }
        }
    }
}
