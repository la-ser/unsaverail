package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;

public class MoneyManager {

    private final ArdaGone plugin;
    private final File moneyFile;
    private final FileConfiguration moneyConfig;

    public MoneyManager(ArdaGone plugin) {
        this.plugin = plugin;
        this.moneyFile = new File(plugin.getDataFolder(), "money.yml");
        this.moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
    }

    public long getMoney(String playerName) {
        return moneyConfig.getLong(playerName, 0);
    }

    public void setMoney(String playerName, long amount) {
        moneyConfig.set(playerName, amount);
        saveMoneyFile();

        Player player = Bukkit.getPlayer(playerName);

        Scoreboard scoreboard = player.getScoreboard();
        String teamName = playerName + "'s team";
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        /*team.setPrefix(ChatColor.GRAY + "Player » ");*/
        team.setSuffix(ChatColor.DARK_GRAY + " | " + ChatColor.GOLD + amount + " 🔔");
        team.setColor(ChatColor.GRAY);

        team.addPlayer(player);

        Objective objective = scoreboard.getObjective(DisplaySlot.PLAYER_LIST);
        if (objective != null) {
            objective.getScore(player.getName()).setScore(0);
        } else {
            plugin.getLogger().severe("Objective is null!");
        }
    }

    public void addMoney(String playerName, long amount) {
        long currentMoney = getMoney(playerName);
        setMoney(playerName, currentMoney + amount);
    }

    private void saveMoneyFile() {
        try {
            moneyConfig.save(moneyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
