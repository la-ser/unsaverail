package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartCommand implements CommandExecutor, Listener {

    private final ArdaGone plugin;
    private final CharacterManager characterManager;
    private final PartySystem partySystem;
    private final inGame inGame;
    private final File partiesFile;
    private FileConfiguration partiesConfig;

    public StartCommand(ArdaGone plugin) {
        this.plugin = plugin;
        this.characterManager = new CharacterManager(plugin);
        this.partySystem = new PartySystem(plugin);
        this.inGame = new inGame(plugin);

        this.partiesFile = new File(plugin.getDataFolder(), "parties.yml");
        this.partiesConfig = YamlConfiguration.loadConfiguration(partiesFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("start")) {
            startGame(player);
            return true;
        }

        return true;
    }

    @EventHandler
    private void onPlayerHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            event.setCancelled(true);

            /*PotionEffect effect = new PotionEffect(PotionEffectType.SATURATION, 20, 1, true, false);
            player.addPotionEffect(effect);*/
        }
    }

    public void startGame(Player player) {
        if (isPlayerInAnyParty(player.getName())) {
            if (isPartyLeader(player.getName())) {
                player.sendMessage(ChatColor.RED + "» Teams mode doesn't exist yet!");
            } else {
                player.sendMessage(ChatColor.RED + "» You are not the party leader!");
            }
        } else {
            inGame.startFunctions();

            teleportPlayersToRandomSpawn(plugin.players);
            player.sendMessage(ChatColor.GREEN + "Game started!");

            for (String playerName : plugin.players) {
                Player playerTarget = Bukkit.getPlayer(playerName);

                if (isPlayerInAnyParty(playerTarget.getName())) return;

                String selectedCharacter = characterManager.getSelectedCharacter(playerTarget);
                characterManager.setPlayerInventoryFromCharacterItems(playerTarget, selectedCharacter);
                playerTarget.sendMessage("Your character: " + selectedCharacter);

                playerTarget.setFoodLevel(18);
                playerTarget.setFireTicks(0);
                playerTarget.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
        }
    }

    public void teleportPlayersToRandomSpawn(List<String> playerNames) {
        File customConfigFile = new File(plugin.getDataFolder(), "gamespawn.yml");
        FileConfiguration customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
        ConfigurationSection spawnpoints = customConfig.getConfigurationSection("spawnpoints");

        if (spawnpoints == null) {
            Bukkit.getLogger().warning("No spawnpoints found in the config.");
            return;
        }

        List<Location> spawnLocations = new ArrayList<>();

        for (String key : spawnpoints.getKeys(false)) {
            String worldName = spawnpoints.getString(key + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = spawnpoints.getDouble(key + ".x");
                double y = spawnpoints.getDouble(key + ".y");
                double z = spawnpoints.getDouble(key + ".z");
                float yaw = spawnpoints.contains(key + ".yaw") ? (float) spawnpoints.getDouble(key + ".yaw") : 0;
                Location location = new Location(world, x, y, z, yaw, 0);
                spawnLocations.add(location);
            }
        }

        Collections.shuffle(spawnLocations);

        int numPlayers = playerNames.size();
        int numSpawnLocations = spawnLocations.size();

        if (numPlayers > numSpawnLocations) {
            Bukkit.getLogger().warning("Not enough spawn locations for all players.");
            return;
        }

        for (int i = 0; i < numPlayers; i++) {
            String playerName = playerNames.get(i);
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) {
                Location spawnLocation = spawnLocations.get(i);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(spawnLocation);
                        player.setGameMode(GameMode.ADVENTURE);
                        player.sendMessage("You have been teleported to a random spawn location.");
                    }
                }.runTaskLater(plugin, i * 20L);
            }
        }
    }

    public boolean isPartyLeader(String playerName) {
        if (partiesConfig.contains("parties")) {
            for (String partyName : partiesConfig.getConfigurationSection("parties").getKeys(false)) {
                String leader = partiesConfig.getString("parties." + partyName + ".leader");

                if (leader.equalsIgnoreCase(playerName)) {
                    return true;
                }
            }
        }
        return false;
    }


    private final String startGUITitle = ChatColor.GREEN + "" + ChatColor.BOLD + "» Start Menu";
    private final String addHeadTitle = ChatColor.GREEN + "➕" + ChatColor.GRAY + " [add to party]";
    public void openStartGUI(Player player) {
        Inventory startGUI = Bukkit.createInventory(player, 27, startGUITitle);

        List<Integer> slotsMain = createSlotListMain();

        List<Integer> slotsBrown = createSlotListBrown();

        for (int slot : slotsMain) {
            ItemStack placeholderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1);
            ItemMeta meta = placeholderItem.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_GRAY + " ");
            placeholderItem.setItemMeta(meta);
            startGUI.setItem(slot, placeholderItem);
        }

        for (int slot : slotsBrown) {
            ItemStack placeholderItem = new ItemStack(Material.BROWN_STAINED_GLASS_PANE, 1);
            ItemMeta meta = placeholderItem.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_GRAY + "—");
            placeholderItem.setItemMeta(meta);
            startGUI.setItem(slot, placeholderItem);
        }

        ItemStack partyLeaderItem = new ItemStack(Material.GOLDEN_HELMET, 1);
        ItemMeta partyLeaderItemMeta = partyLeaderItem.getItemMeta();
        partyLeaderItemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Party Leader");
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        partyLeaderItemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        partyLeaderItem.setItemMeta(partyLeaderItemMeta);
        startGUI.setItem(2, partyLeaderItem);

        ItemStack partyMemberItem = new ItemStack(Material.LEATHER_HELMET, 1);
        ItemMeta partyMemberItemMeta = partyMemberItem.getItemMeta();
        partyMemberItemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GRAY + "Party Member");
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        partyMemberItemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        partyMemberItem.setItemMeta(partyMemberItemMeta);
        startGUI.setItem(3, partyMemberItem);

        ItemStack startItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
        ItemMeta startItemMeta = startItem.getItemMeta();
        startItemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "READY");
        startItem.setItemMeta(startItemMeta);
        startGUI.setItem(6, startItem);

        ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        ItemMeta cancelItemMeta = cancelItem.getItemMeta();
        cancelItemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "CANCEL");
        cancelItem.setItemMeta(cancelItemMeta);
        startGUI.setItem(15, cancelItem);

        Boolean playerIsInParty = isPlayerInAnyParty(player.getName());

        if (playerIsInParty) {
            String partyLeaderName = getPartyLeader(player.getName());
            Player partyLeader = Bukkit.getPlayer(partyLeaderName);
            String partyMemberName = getFirstPartyMember(player.getName());
            Player partyMember = Bukkit.getPlayer(partyMemberName);

            ItemStack partyLeaderHead = ArdaGone.getPlayerHead(partyLeader.getName(), ChatColor.GOLD + partyLeader.getName());
            ItemMeta partyLeaderHeadMeta = partyLeaderHead.getItemMeta();
            partyLeaderHead.setItemMeta(partyLeaderHeadMeta);
            startGUI.setItem(11, partyLeaderHead);

            ItemStack partyMemberHead = ArdaGone.getPlayerHead(partyMember.getName(), ChatColor.GOLD + partyMember.getName());
            ItemMeta partyMemberHeadMeta = partyMemberHead.getItemMeta();
            partyMemberHeadMeta.setDisplayName(ChatColor.GRAY + partyMember.getName());
            partyMemberHead.setItemMeta(partyMemberHeadMeta);
            startGUI.setItem(12, partyMemberHead);
        } else {
            ItemStack partyLeaderHead = ArdaGone.getPlayerHead(player.getName(), ChatColor.GOLD + player.getName());
            ItemMeta partyLeaderHeadMeta = partyLeaderHead.getItemMeta();
            partyLeaderHead.setItemMeta(partyLeaderHeadMeta);
            startGUI.setItem(11, partyLeaderHead);

            ItemStack partyMemberHead = ArdaGone.getHeadFromURL(addHeadTitle, "http://textures.minecraft.net/texture/b4a88483a7d1af1284f192468bd85e1cc6eb9b2ef2e1204c913ee447dbe28ca2");
            ItemMeta partyMemberHeadMeta = partyMemberHead.getItemMeta();
            partyMemberHead.setItemMeta(partyMemberHeadMeta);
            startGUI.setItem(12, partyMemberHead);
        }


        player.openInventory(startGUI);
    }

    public String getPartyLeader(String playerName) {
        if (partiesConfig.contains("parties")) {
            for (String partyName : partiesConfig.getConfigurationSection("parties").getKeys(false)) {
                String leader = partiesConfig.getString("parties." + partyName + ".leader");
                List<String> members = partiesConfig.getStringList("parties." + partyName + ".members");

                if (leader.equalsIgnoreCase(playerName) || members.contains(playerName)) {
                    return leader;
                }
            }
        }
        return null;
    }

    public String getFirstPartyMember(String playerName) {
        if (partiesConfig.contains("parties")) {
            for (String partyName : partiesConfig.getConfigurationSection("parties").getKeys(false)) {
                String leader = partiesConfig.getString("parties." + partyName + ".leader");
                List<String> members = partiesConfig.getStringList("parties." + partyName + ".members");

                if (leader.equalsIgnoreCase(playerName) || members.contains(playerName)) {
                    if (!members.isEmpty()) {
                        return members.get(0);
                    }
                }
            }
        }
        return null;
    }

    public List<Integer> createSlotListMain() {
        List<Integer> slots = new ArrayList<>();
        slots.add(0);
        slots.add(8);
        slots.add(9);
        slots.add(17);
        slots.add(18);
        slots.add(19);
        slots.add(20);
        slots.add(21);
        slots.add(22);
        slots.add(23);
        slots.add(24);
        slots.add(25);
        slots.add(26);
        return slots;
    }

    public List<Integer> createSlotListBrown() {
        List<Integer> slots = new ArrayList<>();
        slots.add(1);
        slots.add(4);
        slots.add(7);
        slots.add(10);
        slots.add(13);
        slots.add(16);
        return slots;
    }

    public boolean isPlayerInAnyParty(String playerName) {
        partiesConfig = YamlConfiguration.loadConfiguration(partiesFile);

        if (partiesConfig.contains("parties")) {
            for (String partyName : partiesConfig.getConfigurationSection("parties").getKeys(false)) {
                String leader = partiesConfig.getString("parties." + partyName + ".leader");
                List<String> members = partiesConfig.getStringList("parties." + partyName + ".members");

                if (leader.equalsIgnoreCase(playerName) || members.contains(playerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(startGUITitle)) {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.1f, 1f);
            if (event.getSlot() == 6 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                plugin.joinGame(player);
                player.closeInventory();
            } else if (event.getSlot() == 15 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                plugin.leaveGame(player);
                player.closeInventory();
            } else if (event.getSlot() == 12 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                if (event.getCurrentItem().getItemMeta().getDisplayName().equals(addHeadTitle)) {
                    partySystem.openPlayerListGUI(player);
                }
            }
            event.setCancelled(true);
        }
    }
}