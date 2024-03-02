package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class LobbySystem implements CommandExecutor {
    private final ArdaGone plugin;
    private SpawnPoints spawnPoints;
    private final Set<Player> playersInLobby;
    private boolean countdownStarted;
    private int countdownSeconds;

    public LobbySystem(ArdaGone plugin, SpawnPoints spawnPoints) {
        this.plugin = plugin;
        this.spawnPoints = spawnPoints;
        this.playersInLobby = new HashSet<>();
        this.countdownStarted = false;
        this.countdownSeconds = 60; // Change this to the desired countdown time in seconds

        // Register commands
        plugin.getCommand("join").setExecutor(this);
        plugin.getCommand("leave").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("join")) {
            if (args.length == 0) {
                joinLobby(player);
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    joinLobby(target);
                    player.sendMessage("Added " + target.getName() + " to the lobby.");
                } else {
                    player.sendMessage("Player not found.");
                }
            } else {
                player.sendMessage("Usage: /join [player]");
            }
        } else if (command.getName().equalsIgnoreCase("leave")) {
            if (args.length == 0) {
                leaveLobby(player);
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    leaveLobby(target);
                    player.sendMessage("Removed " + target.getName() + " from the lobby.");
                } else {
                    player.sendMessage("Player not found.");
                }
            } else {
                player.sendMessage("Usage: /leave [player]");
            }
        }

        return true;
    }

    // Command to join the lobby
    public void joinLobby(Player player) {
        playersInLobby.add(player);
        player.sendMessage("You joined the lobby.");

        // Check if there are at least two players to start the countdown
        if (playersInLobby.size() >= 2 && !countdownStarted) {
            startCountdown();
        }
    }

    // Command to leave the lobby
    public void leaveLobby(Player player) {
        playersInLobby.remove(player);
        player.sendMessage("You left the lobby.");

        // Stop the countdown if there are less than two players
        if (playersInLobby.size() < 2 && countdownStarted) {
            stopCountdown();
        }
    }

    // Method to start the countdown
    private void startCountdown() {
        countdownStarted = true;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSeconds <= 0) {
                if (!countdownStarted) return; // Check if countdown has already finished
                Bukkit.broadcastMessage("Countdown finished!");
                //spawnPoints.randomSpawnAll(playersInLobby); //ERR
                stopCountdown();
            } else {
                if (countdownSeconds == 60 || countdownSeconds <= 15) {
                    Bukkit.broadcastMessage("Countdown: " + countdownSeconds + " seconds remaining.");
                }
                countdownSeconds--;
            }
        }, 0, 20); // 20 ticks = 1 second
    }


    // Method to stop the countdown
    private void stopCountdown() {
        countdownStarted = false;
        //countdownSeconds = 60; // Reset countdown time
    }
}
