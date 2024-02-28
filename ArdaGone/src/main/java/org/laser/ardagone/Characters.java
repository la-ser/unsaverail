package org.laser.ardagone;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class Characters {

    private final ArdaGone plugin;
    private final File configfFile;
    private FileConfiguration config;

    public Characters(ArdaGone plugin) {
        this.plugin = plugin;
        this.configfFile = new File(plugin.getDataFolder(), "characters.yml");
        this.config = YamlConfiguration.loadConfiguration(configfFile);
    }

    public void selectCharacter(Player player, int charID) {
        ConfigurationSection characterSection = config.getConfigurationSection("char");
        if (characterSection == null) {
            characterSection = config.createSection("char");
        };

        if (charID == 0) {
            player.sendMessage("You have selected 'Henry'!");
            characterSection.set(player.getUniqueId().toString(), 0);
        } else if (charID == 1) {
            player.sendMessage("You have selected 'Robin'!");
            characterSection.set(player.getUniqueId().toString(), 1);
        } else {
            player.sendMessage("Err selecting character!");
        }

        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configfFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Err saving the config: " + e.getMessage());
        }
    }
}
