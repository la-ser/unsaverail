package org.laser.ardagone;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClickableSigns implements CommandExecutor, Listener {

    private final ArdaGone plugin;

    private final MoneyManager moneyManager;
    private final Tasks tasks;
    private final GUIManager guiManager;
    private final FileConfiguration dialoguesConfig;
    private final NamespacedKey dialogueSectionKey;
    private final NamespacedKey guiSectionKey;
    private final NamespacedKey villagerTagKey;

    public ClickableSigns(ArdaGone plugin, Tasks tasks) {
        this.plugin = plugin;
        this.moneyManager = new MoneyManager(plugin);
        this.tasks = tasks;
        this.guiManager = new GUIManager(plugin, tasks, moneyManager);
        this.dialoguesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "dialogues.yml"));
        this.villagerTagKey = new NamespacedKey(plugin, "villagerTag");
        this.dialogueSectionKey = new NamespacedKey(plugin, "dialogueSection");
        this.guiSectionKey = new NamespacedKey(plugin, "guiSection");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /setsign <signNumber>");
            return true;
        }

        try {
            Player player = (Player) sender;
            setSign(player, args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid sign.");
        }

        return true;
    }

    private void setSign(Player player, String signType) {
        Location location = player.getLocation().getBlock().getLocation();

        File signsFile = new File(plugin.getDataFolder(), "signs.yml");
        FileConfiguration signsConfig = YamlConfiguration.loadConfiguration(signsFile);

        signsConfig.set("signs." + signType + ".x", location.getBlockX());
        signsConfig.set("signs." + signType + ".y", location.getBlockY());
        signsConfig.set("signs." + signType + ".z", location.getBlockZ());

        try {
            signsConfig.save(signsFile);
        } catch (IOException e) {
            player.sendMessage("An error occurred while saving the configuration.");
            e.printStackTrace();
        }

        player.sendMessage("Sign created!");
    }

    public boolean isSign(Block block) {
        return block.getState() instanceof Sign;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isSign(block)) {
            Location location = block.getLocation();
            File signsFile = new File(plugin.getDataFolder(), "signs.yml");
            FileConfiguration signsConfig = YamlConfiguration.loadConfiguration(signsFile);
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            if (signsConfig.contains("signs")) {
                for (String signType : signsConfig.getConfigurationSection("signs").getKeys(false)) {
                    int signX = signsConfig.getInt("signs." + signType + ".x");
                    int signY = signsConfig.getInt("signs." + signType + ".y");
                    int signZ = signsConfig.getInt("signs." + signType + ".z");

                    if (x == signX && y == signY && z == signZ) {
                        signsConfig.set("signs." + signType, null);
                        try {
                            signsConfig.save(signsFile);
                        } catch (IOException e) {
                            event.getPlayer().sendMessage("An error occurred while saving the configuration.");
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        if (event.getClickedBlock() != null && isSign(event.getClickedBlock())) {
            Player player = event.getPlayer();
            Location location = event.getClickedBlock().getLocation();

            File signsFile = new File(plugin.getDataFolder(), "signs.yml");
            FileConfiguration signsConfig = YamlConfiguration.loadConfiguration(signsFile);

            for (String signType : signsConfig.getConfigurationSection("signs").getKeys(false)) {
                int signX = signsConfig.getInt("signs." + signType + ".x");
                int signY = signsConfig.getInt("signs." + signType + ".y");
                int signZ = signsConfig.getInt("signs." + signType + ".z");

                if (location.getBlockX() == signX && location.getBlockY() == signY && location.getBlockZ() == signZ) {
                    ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                    ConfigurationSection GUISection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());

                    if (dialogueSection != null && dialogueSection.contains(signType)) {
                        List<String> pages = dialogueSection.getStringList(signType);
                        if (!pages.isEmpty()) {
                            tasks.openDialogueBook(player, pages, signType);
                            return;
                        }
                    } else if (GUISection != null && GUISection.contains(signType)) {
                        String argument = GUISection.getString(signType);
                        if (argument != null) {
                            guiManager.openSavedGUI(player, argument);
                            return;
                        }
                    } else {
                        player.sendMessage("gui/dialogue hasn't been set yet!");
                        return;
                    }
                    break;
                }
            }
        }
    }
}
