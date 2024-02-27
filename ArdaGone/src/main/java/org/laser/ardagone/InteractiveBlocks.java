package org.laser.ardagone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class InteractiveBlocks implements CommandExecutor, Listener {

    private final ArdaGone plugin;

    private final MoneyManager moneyManager;
    private final CharacterManager characterManager;
    private final Tasks tasks;
    private final GUIManager guiManager;
    private final StartCommand startCommand;
    private final FileConfiguration dialoguesConfig;
    private final File dialoguesFile;
    private final NamespacedKey dialogueSectionKey;
    private final NamespacedKey guiSectionKey;
    private final NamespacedKey villagerTagKey;

    public InteractiveBlocks(ArdaGone plugin, Tasks tasks) {
        this.plugin = plugin;
        this.moneyManager = new MoneyManager(plugin);
        this.tasks = tasks;
        this.guiManager = new GUIManager(plugin, tasks, moneyManager);
        this.characterManager = new CharacterManager(plugin);
        this.startCommand = new StartCommand(plugin);

        this.dialoguesFile = new File(plugin.getDataFolder(), "dialogues.yml");
        this.dialoguesConfig = YamlConfiguration.loadConfiguration(dialoguesFile);
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

        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("setinteractiveblock") || cmd.getAliases().contains("setinteractiveblock")) {

            if (args.length != 2) {
                sender.sendMessage("Usage: /setinteractiveblock <blockID> <interactionID>");
                return true;
            }

            try {
                setInteractiveBlock(player, args[0], args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid block.");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("removeinteractiveblock") || cmd.getAliases().contains("removeinteractiveblock")) {
            if (args.length != 1) {
                sender.sendMessage("Usage: /removeinteractiveblock <blockID>");
                return true;
            }

            try {
                String blockID = args[0];
                removeInteractiveBlock(player, blockID);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid block.");
            }
            return true;
        }

        return true;
    }

    private void setInteractiveBlock(Player player, String blockID, String interactionID) {
        /*Location location = player.getLocation().getBlock().getLocation();*/
        Block block = getBlockPlayerIsLookingAt(player, 7);
        Location location = block.getLocation();

        File iBlockFile = new File(plugin.getDataFolder(), "interactionblocks.yml");
        FileConfiguration iBlockConfig = YamlConfiguration.loadConfiguration(iBlockFile);

        iBlockConfig.set("interactionBlocks." + blockID + ".x", location.getBlockX());
        iBlockConfig.set("interactionBlocks." + blockID + ".y", location.getBlockY());
        iBlockConfig.set("interactionBlocks." + blockID + ".z", location.getBlockZ());

        try {
            iBlockConfig.save(iBlockFile);
        } catch (IOException e) {
            player.sendMessage("An error occurred while saving the configuration.");
            e.printStackTrace();
        }

        dialoguesConfig.set("guisection." + blockID, interactionID);

        try {
            dialoguesConfig.save(dialoguesFile);
        } catch (IOException e) {
            player.sendMessage("An error occurred while saving the configuration.");
            e.printStackTrace();
        }

        player.sendMessage("Block created!");
    }

    private void removeInteractiveBlock(Player player, String blockID) {
        File iBlockFile = new File(plugin.getDataFolder(), "interactionblocks.yml");
        FileConfiguration iBlockConfig = YamlConfiguration.loadConfiguration(iBlockFile);

        if (iBlockConfig.contains("interactionBlocks." + blockID)) {
            iBlockConfig.set("interactionBlocks." + blockID, null);

            try {
                iBlockConfig.save(iBlockFile);
            } catch (IOException e) {
                player.sendMessage("An error occurred while saving the configuration.");
                e.printStackTrace();
            }

            player.sendMessage("Block removed!");
        } else {
            player.sendMessage("Block not found!");
        }

        if (dialoguesConfig.contains("guisection." + blockID)) {
            dialoguesConfig.set("guisection." + blockID, null);

            try {
                dialoguesConfig.save(dialoguesFile);
            } catch (IOException e) {
                player.sendMessage("An error occurred while saving the configuration.");
                e.printStackTrace();
            }
        }
    }

    public Block getBlockPlayerIsLookingAt(Player player, int range) {
        Location playerLocation = player.getEyeLocation();
        Vector direction = playerLocation.getDirection().normalize();

        for (int i = 1; i <= range; i++) {
            Location targetLocation = playerLocation.clone().add(direction.clone().multiply(i));
            Block targetBlock = targetLocation.getBlock();

            if (!targetBlock.isEmpty()) {
                return targetBlock;
            }
        }

        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != null && block.getType() != Material.AIR) {
            Location location = block.getLocation();
            File iBlockFile = new File(plugin.getDataFolder(), "interactionblocks.yml");
            FileConfiguration iBlockConfig = YamlConfiguration.loadConfiguration(iBlockFile);
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            if (iBlockConfig.contains("interactionBlocks")) {
                for (String iBlockType : iBlockConfig.getConfigurationSection("interactionBlocks").getKeys(false)) {
                    int iBLockX = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".x");
                    int iBLockY = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".y");
                    int iBLockZ = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".z");

                    if (x == iBLockX && y == iBLockY && z == iBLockZ) {
                        iBlockConfig.set("interactionBlocks." + iBlockType, null);
                        try {
                            iBlockConfig.save(iBlockFile);
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

        Block block = event.getClickedBlock();

        if (block != null && block.getType() != null && block.getType() != Material.AIR) {
            Player player = event.getPlayer();
            Location location = event.getClickedBlock().getLocation();

            File iBlockFile = new File(plugin.getDataFolder(), "interactionblocks.yml");
            FileConfiguration iBlockConfig = YamlConfiguration.loadConfiguration(iBlockFile);

            for (String iBlockType : iBlockConfig.getConfigurationSection("interactionBlocks").getKeys(false)) {
                String value = dialoguesConfig.getString("guisection." + iBlockType);
                int iBLockX = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".x");
                int iBLockY = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".y");
                int iBLockZ = iBlockConfig.getInt("interactionBlocks." + iBlockType + ".z");

                if (location.getBlockX() == iBLockX && location.getBlockY() == iBLockY && location.getBlockZ() == iBLockZ) {
                    ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                    ConfigurationSection GUISection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());

                    if (value.equalsIgnoreCase("checkchars")){
                        characterManager.openUnlockedCharactersGUI(player);
                        return;
                    } else if (value.equalsIgnoreCase("startmenu")){
                        startCommand.openStartGUI(player);
                        return;
                    } else if (dialogueSection != null && dialogueSection.contains(iBlockType)) {
                        List<String> pages = dialogueSection.getStringList(iBlockType);
                        if (!pages.isEmpty()) {
                            tasks.openDialogueBook(player, pages, iBlockType);
                            return;
                        }
                    } else if (GUISection != null) {
                        String argument = GUISection.getString(iBlockType);
                        if (argument != null) {
                            guiManager.openSavedGUI(player, argument);
                            return;
                        }
                    }

                    player.sendMessage("gui/dialogue hasn't been set yet!");
                    return;
                }
            }
        }
    }
}
