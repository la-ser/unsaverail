package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TheFog extends JavaPlugin {

    private final ArdaGone plugin;
    private Map<Integer, Double> circles = new HashMap<>(); // Map to store circle IDs and their corresponding radius

    public TheFog(ArdaGone plugin) {
        this.plugin = plugin;
    }

    // warum no usages?
    @Override
    public void onEnable() {

        Bukkit.getScheduler().runTaskTimer(this, this::updateZone, 0, 20);

    }


    private void updateZone() {

        World world = Bukkit.getWorlds().get(0);

        Iterator<Map.Entry<Integer, Double>> iterator = circles.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, Double> entry = iterator.next();
            double radius = entry.getValue();
            radius -= 0.5; // decrement


            if (radius <= 0) {
                iterator.remove(); // das nicht weitere Effekte entstehen hoffe ich
            } else {
                entry.setValue(radius);
            }
        }

        int newCircleId = circles.size() + 1;
        circles.put(newCircleId, 5.0);
        drawCircle(world, newCircleId, new Location(world, 0, 100, 0), 5, 20);

    }

    private void drawCircle(World world, int circleId, Location center, double radius, int points) {
        double increment = (2 * Math.PI) / points;
        for (int i = 0; i < points; i++) {
            double angle = i * increment;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(Particle.CLOUD, loc, 1);
        }
    }

    // clear all
    private void clearAllParticleEffects() {
        World world = Bukkit.getWorlds().get(0);

        for (int circleId : circles.keySet()) {

            for (int i = 0; i < 360; i += 10) { // Increase the step value to clear more particles

                double angle = Math.toRadians(i);
                double x = circles.get(circleId) * Math.cos(angle);
                double z = circles.get(circleId) * Math.sin(angle);
                Location loc = new Location(world, x, 100, z);
                world.spawnParticle(Particle.CLOUD, loc, 1); // verstehe ich nicht

            }
        }
    }


    /*

    private boolean isInZone(Location location) {
    for (double radius : circles.values()) {
        double distanceSquared = location.distanceSquared(new Location(location.getWorld(), 0, 100, 0)); // Assuming the center of the zone is at (0, 100, 0)
        if (distanceSquared <= radius * radius) {
            return true; // Spieler ist in der Zone
        }
    }
    return false; // Spieler ist außerhalb der Zone
}


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();
        if (isInZone(playerLocation)) {
            // Spieler ist in der Zone
        } else {
            // Spieler ist außerhalb der Zone
        }
    }

     */
}
