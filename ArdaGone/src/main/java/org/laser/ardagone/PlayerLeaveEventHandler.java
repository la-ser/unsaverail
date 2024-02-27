package org.laser.ardagone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerLeaveEventHandler implements Listener {

    private final ArdaGone plugin;
    private final StatusManager statusManager;

    public PlayerLeaveEventHandler(ArdaGone plugin) {
        this.plugin = plugin;
        this.statusManager = new StatusManager(plugin);
    }

    @EventHandler
    private void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.setDisplayName(null);
        Scoreboard scoreboard = player.getScoreboard();

        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        ArdaGone.players.remove(player.getName());

        statusManager.removeStatus(player);
    }
}
