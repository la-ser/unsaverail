package org.laser.ardagone;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;

public class CustomFishingRod implements Listener {

    private final ArdaGone plugin;

    public CustomFishingRod(ArdaGone plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.IN_GROUND) {
            Player player = event.getPlayer();
            Location hookLocation = event.getHook().getLocation();
            Location playerLocation = player.getLocation();

            Vector direction = hookLocation.toVector().subtract(playerLocation.toVector()).normalize();

            direction.multiply(1.2);
            direction.setY(0.3);

            player.setVelocity(direction);
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity caught = event.getCaught();

            if (caught instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) caught;

                Player player = event.getPlayer();
                Vector direction = player.getLocation().toVector().subtract(livingEntity.getLocation().toVector()).normalize();
                caught.setVelocity(direction.multiply(1));
            }
        }

    }


}
