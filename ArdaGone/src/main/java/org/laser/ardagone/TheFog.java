package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class TheFog implements CommandExecutor {

    private final ArdaGone plugin;

    private final Map<Player, CircleProperties> circleMap = new HashMap<>();

    public TheFog(ArdaGone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1 || args.length > 3) {
            sender.sendMessage("Usage: /fornite <radius> [durationInSeconds]");
            return true;
        }

        if (args[0].equalsIgnoreCase("false")) {
            removeCircle(player);
            sender.sendMessage("Circle removed.");
            return true;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid radius. Please specify a number.");
            return true;
        }

        int duration = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int steps = args.length == 3 ? Integer.parseInt(args[2]) : 100; // Default number of steps for the transition

        CircleProperties circle = circleMap.get(player);
        if (circle == null) {
            // Display circle without duration
            displayCircle(player, radius, duration, steps);
            sender.sendMessage("Circle with radius " + radius + " created.");
        } else {
            // Update existing circle
            circle.updateCircle(radius, duration, steps);
            sender.sendMessage("Circle updated: Radius " + radius + ", Duration " + duration + " seconds.");
        }

        return true;
    }

    private void displayCircle(Player player, int radius, int duration, int steps) {
        Location center = player.getLocation();

        CircleProperties circle = circleMap.get(player);
        if (circle == null) {
            circle = new CircleProperties(radius, center, plugin, duration, steps);
            circleMap.put(player, circle);
        } else {
            circle.updateCircle(radius, duration, steps);
        }

        circle.startCircle();
    }

    private void removeCircle(Player player) {
        CircleProperties circle = circleMap.remove(player);
        if (circle != null) {
            circle.stopCircle();
        }
    }

    private static class CircleProperties {
        private final ArdaGone plugin;
        private final Player player;
        private int radius;
        private Location center;
        private int duration;
        private int steps;
        private int stepCounter;
        private double deltaRadius;
        private double deltaPosX;
        private double deltaPosZ;
        private double initialRadius;
        private double initialPosX;
        private double initialPosZ;
        private Location targetLocation;
        private BukkitTask task;

        public CircleProperties(int radius, Location center, ArdaGone plugin, int duration, int steps) {
            this.plugin = plugin;
            this.radius = radius;
            this.initialRadius = radius;
            this.center = center;
            this.initialPosX = center.getX();
            this.initialPosZ = center.getZ();
            this.player = center.getWorld().getPlayers().get(0); // Just for initializing player
            this.duration = duration;
            this.steps = steps;
            this.stepCounter = 0;
            this.deltaRadius = (double) (radius - initialRadius) / steps;
            this.targetLocation = center.clone(); // Initialize targetLocation with center
        }

        public void updateCircle(int newRadius, int newDuration, int newSteps) {
            if (task != null) {
                task.cancel();
            }
            this.radius = newRadius;
            this.initialRadius = radius;
            this.duration = newDuration;
            this.steps = newSteps;
            this.stepCounter = 0;
            this.deltaRadius = (double) (newRadius - initialRadius) / steps;
            this.targetLocation = center.clone(); // Update targetLocation with center
            startCircle();
        }

        public void startCircle() {
            this.deltaPosX = (targetLocation.getX() - initialPosX) / steps;
            this.deltaPosZ = (targetLocation.getZ() - initialPosZ) / steps;

            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (stepCounter >= steps) {
                    center = targetLocation.clone();
                    stopCircle();
                    return;
                }

                for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 180) {
                    double x = center.getX() + radius * Math.cos(theta);
                    double z = center.getZ() + radius * Math.sin(theta);
                    Location borderLocation = new Location(center.getWorld(), x, center.getY(), z);
                    player.getWorld().spawnParticle(Particle.SPELL_MOB, borderLocation, 1);
                }

                stepCounter++;
                radius += deltaRadius;
                center.add(deltaPosX, 0, deltaPosZ);
            }, 0, duration * 20L / steps);
        }

        public void stopCircle() {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
