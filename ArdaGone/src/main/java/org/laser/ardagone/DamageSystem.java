package org.laser.ardagone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageSystem {
    private final ArdaGone plugin;
    private final Characters characters;

    public DamageSystem(ArdaGone plugin, Characters characters) {
        this.plugin = plugin;
        this.characters = characters;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            int playerCharID = characters.getCharacter(player);
            ItemStack weapon = player.getInventory().getItemInMainHand();

            /*if (weapon.getType().equals(Material.WOODEN_SWORD) && weapon.getItemMeta().getCustomModelData() == 1) {
                player.sendMessage("You have attacked as Henry!");
            }*/
            if (playerCharID == 0) {
                player.sendMessage("You have attacked as Henry!");
            } else if (playerCharID == 1) {
                player.sendMessage("You have attacked as Robin!");
            }
        }
    }
}
