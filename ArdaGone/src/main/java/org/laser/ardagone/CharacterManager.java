package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CharacterManager implements CommandExecutor, Listener, TabCompleter {

    private final ArdaGone plugin;

    private File selectedCharsFile;
    private FileConfiguration selectedCharsConfig;
    private File charactersFile;
    private FileConfiguration charactersConfig;

    public CharacterManager(ArdaGone plugin) {
        this.plugin = plugin;

        selectedCharsFile = new File(plugin.getDataFolder(), "selectedchars.yml");
        selectedCharsConfig = YamlConfiguration.loadConfiguration(selectedCharsFile);

        charactersFile = new File(plugin.getDataFolder(), "characters.yml");
        charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("createcharacter") || cmd.getAliases().contains("createcharacter")) {


            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /createcharacter <id> <name>");
                return true;
            }

            String id = args[0];
            String name = args[1];

            openCharacterCreationGUI(player, id, name);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("listcharacters") || cmd.getAliases().contains("listcharacters")) {
            openCharacterListGUI(player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("checkchars") || cmd.getAliases().contains("checkchars")) {
            openUnlockedCharactersGUI(player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("character") || cmd.getAliases().contains("character")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /character <player> <characterID> true/false");
                return true;
            }

            String targetPlayerName = args[0];
            String characterId = args[1];
            String unlockStatus = args[2].toLowerCase();

            if (!unlockStatus.equals("true") && !unlockStatus.equals("false")) {
                sender.sendMessage("Invalid value for unlock status. Use 'true' or 'false'.");
                return true;
            }

            boolean unlocked = Boolean.parseBoolean(unlockStatus);

            setCharacterUnlockStatus(targetPlayerName, characterId, unlocked);

            String status = unlocked ? "unlocked" : "locked";
            sender.sendMessage("Character " + characterId + " is now " + status + " for player " + targetPlayerName);

            return true;
        } else if (cmd.getName().equalsIgnoreCase("selectcharacter") || cmd.getAliases().contains("selectcharacter")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /selectcharacter <characterID>");
                return true;
            }

            setSelectedCharacter(player, args[0]);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("getchar") || cmd.getAliases().contains("getchar")) {
            player.sendMessage(getSelectedCharacter(player));
            return true;
        } else if (cmd.getName().equalsIgnoreCase("setcharitems") || cmd.getAliases().contains("setcharitems")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /setcharitems <characterID>");
                return true;
            }

            String characterId = args[0];

            saveCharacterItems(player, characterId);

            sender.sendMessage("Character items saved for character " + characterId);
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("character") || command.getAliases().contains("character")) {

            if (args.length == 1) {
                for (Player onlinePlayer : ((Player) sender).getServer().getOnlinePlayers()) {
                    completions.add(onlinePlayer.getName());
                }
            } else if (args.length == 2) {
                Set<String> characterIDs = getCharacterIDsFromConfig();
                completions.addAll(characterIDs);
            } else if (args.length == 3) {
                completions.add("true");
                completions.add("false");
            }

            String partial = args[args.length - 1].toLowerCase();
            completions.removeIf(option -> !option.toLowerCase().startsWith(partial));

        }
        return completions;
    }

    private Set<String> getCharacterIDsFromConfig() {
        ConfigurationSection charactersSection = charactersConfig.getConfigurationSection("characters");
        if (charactersSection != null) {
            return charactersSection.getKeys(false);
        }
        return null;
    }

    public String getSelectedCharacterID(Player player) {
        File selectedCharsFile = new File(plugin.getDataFolder(), "selectedchars.yml");

        if (!selectedCharsFile.exists()) {
            return null;
        }

        FileConfiguration selectedCharsConfig = YamlConfiguration.loadConfiguration(selectedCharsFile);
        String playerUUIDString = player.getUniqueId().toString();

        if (selectedCharsConfig.contains(playerUUIDString)) {
            return selectedCharsConfig.getString(playerUUIDString);
        }

        return null;
    }


    private void openCharacterCreationGUI(Player player, String id, String name) {
        Inventory gui = Bukkit.createInventory(player, 9, "Character Creation");

        ItemStack middleSlotItem = new ItemStack(Material.DIAMOND_SWORD);
        gui.setItem(4, middleSlotItem);

        player.openInventory(gui);

        player.setMetadata("CharacterID", new FixedMetadataValue(plugin, id));
        player.setMetadata("CharacterName", new FixedMetadataValue(plugin, name));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Character Creation")) {
            Player player = (Player) event.getWhoClicked();
            int clickedSlot = event.getRawSlot();

            String id = player.getMetadata("CharacterID").get(0).asString();
            String name = player.getMetadata("CharacterName").get(0).asString();
            ItemStack iconItem = event.getCurrentItem();

            saveCharacter(id, name, iconItem);

            player.sendMessage("Character created!");

            event.setCancelled(true);
            player.closeInventory();
        } else if (event.getView().getTitle().equals("Character List")) {
            event.setCancelled(true);
        }else if (event.getView().getTitle().equals(unlockedChars_Titel)) {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.1f, 1f);
            Inventory clickedInventory = event.getClickedInventory();

            if (clickedInventory != null) {
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedInventory.equals(player.getInventory())) {
                    event.setCancelled(true);
                    return;
                }

                if (clickedItem != null) {
                    int slot = event.getSlot() + 1;
                    String characterId = String.valueOf(slot);

                    File charactersFile = new File(plugin.getDataFolder(), "characters.yml");
                    FileConfiguration charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);

                    if (charactersConfig.contains("characters." + characterId + ".name")) {
                        String characterName = charactersConfig.getString("characters." + characterId + ".name");

                        Set<String> unlockedCharacterIds = getUnlockedCharacterIds(player);

                        if (unlockedCharacterIds.contains(characterId)) {
                            player.sendMessage(ChatColor.AQUA + "» You selected: " + characterName);
                            setSelectedCharacter(player, characterId);
                            player.closeInventory();
                            openUnlockedCharactersGUI(player);
                        }
                    }
                }
            }
            event.setCancelled(true);
        }

    }

    private Set<String> getUnlockedCharacterIds(Player player) {
        File unlockedCharsFile = new File(plugin.getDataFolder(), "unlockedchars.yml");
        FileConfiguration unlockedCharsConfig = YamlConfiguration.loadConfiguration(unlockedCharsFile);

        Set<String> unlockedCharacterIds = new HashSet<>();
        if (unlockedCharsConfig.contains(player.getUniqueId().toString())) {
            unlockedCharacterIds = unlockedCharsConfig.getConfigurationSection(player.getUniqueId().toString())
                    .getKeys(false);
        }
        return unlockedCharacterIds;
    }

    private final String unlockedChars_Titel = "Unlocked Characters";

    private void saveCharacter(String id, String name, ItemStack iconItem) {
        File charactersFile = new File(plugin.getDataFolder(), "characters.yml");
        FileConfiguration charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);

        charactersConfig.set("characters." + id + ".name", name);
        charactersConfig.set("characters." + id + ".iconItem", iconItem);

        try {
            charactersConfig.save(charactersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openCharacterListGUI(Player player) {
        File charactersFile = new File(plugin.getDataFolder(), "characters.yml");
        FileConfiguration charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);

        if (!charactersConfig.contains("characters")) {
            player.sendMessage("No characters found.");
            return;
        }

        ConfigurationSection characterSection = charactersConfig.getConfigurationSection("characters");
        Set<String> characterIds = characterSection.getKeys(false).stream()
                .map(Integer::parseInt)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        int guiSize = (int) Math.ceil(characterIds.size() / 9.0) * 9;
        Inventory characterListGUI = Bukkit.createInventory(player, guiSize, "Character List");

        for (String characterId : characterIds) {
            String name = characterSection.getString(characterId + ".name");
            ItemStack iconItem = characterSection.getItemStack(characterId + ".iconItem");
            characterListGUI.addItem(iconItem);
        }

        player.openInventory(characterListGUI);
    }

    public void openUnlockedCharactersGUI(Player player) {
        File unlockedCharsFile = new File(plugin.getDataFolder(), "unlockedchars.yml");
        FileConfiguration unlockedCharsConfig = YamlConfiguration.loadConfiguration(unlockedCharsFile);

        Set<String> unlockedCharacterIds = new HashSet<>();
        if (unlockedCharsConfig.contains(player.getUniqueId().toString())) {
            unlockedCharacterIds = unlockedCharsConfig.getConfigurationSection(player.getUniqueId().toString())
                    .getKeys(false);
        }

        File charactersFile = new File(plugin.getDataFolder(), "characters.yml");
        FileConfiguration charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);

        int guiSize = 36;
        Inventory unlockedCharacterGUI = Bukkit.createInventory(player, guiSize, unlockedChars_Titel);

        int slotCounter = 0;

        for (int i = 1; i <= guiSize; i++) {
            String characterId = String.valueOf(i);

            boolean isUnlocked = unlockedCharacterIds.contains(characterId);

            if (charactersConfig.contains("characters." + characterId)) {
                String name = charactersConfig.getString("characters." + characterId + ".name");
                ItemStack iconItem = charactersConfig.getItemStack("characters." + characterId + ".iconItem");

                if (iconItem != null) {
                    if (isUnlocked) {
                        String selectedCharacter = getSelectedCharacter(player);
                        if (selectedCharacter != null && selectedCharacter.equals(characterId)) {
                            if (iconItem != null && iconItem.getType() != Material.AIR) {
                                final ItemMeta itemMeta = iconItem.getItemMeta();
                                itemMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                iconItem.setItemMeta(itemMeta);
                            }
                        }
                        unlockedCharacterGUI.addItem(iconItem);
                    } else {
                        ItemStack lockedItem = ArdaGone.getHeadFromURL(ChatColor.DARK_GRAY + "???", "http://textures.minecraft.net/texture/da99b05b9a1db4d29b5e673d77ae54a77eab66818586035c8a2005aeb810602a");
                        unlockedCharacterGUI.addItem(lockedItem);
                    }
                }
            } else {
                ItemStack comingSoonItem = ArdaGone.getHeadFromURL(ChatColor.RED + "Coming Soon", "http://textures.minecraft.net/texture/1f575bb54a2e9133aaa1310a14642f78a014fcc9360774171663d34db236ccc4");
                unlockedCharacterGUI.addItem(comingSoonItem);
            }

            slotCounter++;
        }

        player.openInventory(unlockedCharacterGUI);
    }

    private void setCharacterUnlockStatus(String targetPlayerName, String characterId, boolean unlocked) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer != null) {
            UUID targetPlayerUUID = targetPlayer.getUniqueId();
            File unlockedCharsFile = new File(plugin.getDataFolder(), "unlockedchars.yml");
            FileConfiguration unlockedCharsConfig = YamlConfiguration.loadConfiguration(unlockedCharsFile);

            if (!unlockedCharsConfig.contains(targetPlayerUUID.toString())) {
                unlockedCharsConfig.createSection(targetPlayerUUID.toString());
            }

            ConfigurationSection playerSection = unlockedCharsConfig.getConfigurationSection(targetPlayerUUID.toString());

            if (unlocked) {
                playerSection.set(characterId, true);
            } else {
                playerSection.set(characterId, null);
            }

            try {
                unlockedCharsConfig.save(unlockedCharsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSelectedCharacter(Player player, String characterId) {
        selectedCharsConfig.set(player.getUniqueId().toString(), characterId);
        saveSelectedCharactersConfig();
    }

    public String getSelectedCharacter(Player player) {
        selectedCharsConfig = YamlConfiguration.loadConfiguration(selectedCharsFile);
        String playerUUID = player.getUniqueId().toString();
        String selectedCharacter = selectedCharsConfig.getString(playerUUID);
        return selectedCharacter != null ? selectedCharacter : "";
    }

    public ItemStack getCharacterIconItem(String characterId) {
        File charactersFile = new File(plugin.getDataFolder(), "characters.yml");
        FileConfiguration charactersConfig = YamlConfiguration.loadConfiguration(charactersFile);

        if (charactersConfig.contains("characters." + characterId)) {
            return charactersConfig.getItemStack("characters." + characterId + ".iconItem");
        }

        return null;
    }

    private void saveSelectedCharactersConfig() {
        try {
            selectedCharsConfig.save(selectedCharsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCharacterItems(Player player, String characterId) {
        File characterItemsFile = new File(plugin.getDataFolder(), "characteritems.yml");
        FileConfiguration characterItemsConfig = YamlConfiguration.loadConfiguration(characterItemsFile);

        Inventory playerInventory = player.getInventory();
        characterItemsConfig.set("characterItems." + characterId, playerInventory.getContents());

        try {
            characterItemsConfig.save(characterItemsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPlayerInventoryFromCharacterItems(Player player, String characterId) {
        player.playSound(player, Sound.BLOCK_BELL_USE, 2.0f, 0.4f);

        File characterItemsFile = new File(plugin.getDataFolder(), "characteritems.yml");
        FileConfiguration characterItemsConfig = YamlConfiguration.loadConfiguration(characterItemsFile);

        if (characterItemsConfig.contains("characterItems." + characterId)) {
            List<ItemStack> characterItems = (List<ItemStack>) characterItemsConfig.getList("characterItems." + characterId);

            if (characterItems != null) {
                player.getInventory().setContents(characterItems.toArray(new ItemStack[0]));
            } else {
                player.sendMessage("Character items for character " + characterId + " not found.");
            }
        } else {
            player.sendMessage("Character items for character " + characterId + " not found.");
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            if (event.getHitEntity() != null) {
                event.getEntity().remove();
            }

            if (event.getHitBlock() != null && event.getHitBlock().getType().isSolid()){
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
                    @Override
                    public void run(){
                        event.getEntity().remove();
                    }
                }, 10L);
            }
        }
    }


}