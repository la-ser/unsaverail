package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TheFog implements CommandExecutor {

    private final ArdaGone plugin;
    private final Map<Player, ZoneData> zones = new HashMap<>();

    public TheFog(ArdaGone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("zone")) {
            handleZoneCommand(player, args);
        } else if (label.equalsIgnoreCase("modifyzone")) {
            handleModifyZoneCommand(player, args);
        } else {
            sender.sendMessage("Unknown command: " + label);
        }

        return true;
    }

    private void handleZoneCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage("Usage: /zone <radius>");
            return;
        }

        try {
            int radius = Integer.parseInt(args[0]);
            createZone(player, radius);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid radius. Please provide a valid integer.");
        }
    }

    private void handleModifyZoneCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Usage: /modifyzone <radius> <time>");
            return;
        }

        try {
            int radius = Integer.parseInt(args[0]);
            int time = Integer.parseInt(args[1]);
            modifyZone(player, radius, time);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid arguments. Please provide valid integers.");
        }
    }

    private void modifyZone(Player player, int newRadius, int time) {
        if (!zones.containsKey(player)) {
            player.sendMessage("You don't have an existing zone. Create one using /zone command first.");
            return;
        }

        ZoneData zoneData = zones.get(player);
        int oldRadius = zoneData.radius;
        int steps = Math.max(1, time * 20 / 2); // Number of steps for smooth transition

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= steps) {
                    // Update zone data with new radius
                    zoneData.radius = newRadius;
                    cancel();
                    return;
                }

                // Calculate intermediate radius
                int currentRadius = oldRadius + (newRadius - oldRadius) * step / steps;
                updateZone(player, currentRadius);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every tick for smoother transition
    }

    private void createZone(Player player, int radius) {
        World world = player.getWorld();
        Location center = new Location(world, 128.5, 68, -114.5); // Set center coordinates

        // Define particle color
        Particle.DustOptions particleData = new Particle.DustOptions(org.bukkit.Color.RED, 1);

        // Spawn particles in a circle
        for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 36) {
            double x = center.getX() + radius * Math.cos(theta);
            double z = center.getZ() + radius * Math.sin(theta);
            Location particleLocation = new Location(world, x, center.getY(), z);
            world.spawnParticle(Particle.REDSTONE, particleLocation, 1, particleData);
        }

        // Schedule a task to continuously update the particle circle
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!zones.containsKey(player)) {
                    cancel(); // If the player's zone is removed, stop updating the particles
                    return;
                }

                // Clear previous particles
                for (Location loc : calculateCircle(center, zones.get(player).radius)) {
                    world.spawnParticle(Particle.REDSTONE, loc, 1, particleData);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Update every second

        // Store zone data
        zones.put(player, new ZoneData(radius));
    }

    private void updateZone(Player player, int radius) {
        // Update zone data
        zones.get(player).radius = radius;

        // Get center location
        World world = player.getWorld();
        Location center = new Location(world, 128.5, 68, -114.5); // Set center coordinates

        // Spawn particles for the updated zone
        spawnZoneParticles(center, radius);
    }

    // Spawn particles for the zone with the given center and radius
    private void spawnZoneParticles(Location center, int radius) {
        World world = center.getWorld();

        // Define particle color
        Particle.DustOptions particleData = new Particle.DustOptions(org.bukkit.Color.RED, 1);

        // Spawn particles in a circle without clearing previous particles
        for (Location loc : calculateCircle(center, radius)) {
            world.spawnParticle(Particle.REDSTONE, loc, 1, particleData);
        }
    }

    // Calculate the locations of the circle's particles
    private List<Location> calculateCircle(Location center, int radius) {
        List<Location> circle = new ArrayList<>();
        World world = center.getWorld();

        for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 36) {
            double x = center.getX() + radius * Math.cos(theta);
            double z = center.getZ() + radius * Math.sin(theta);
            Location particleLocation = new Location(world, x, 68, z); // Set y-coordinate to 56
            circle.add(particleLocation);
        }

        return circle;
    }

    private static class ZoneData {
        int radius;

        ZoneData(int radius) {
            this.radius = radius;
        }
    }
}
