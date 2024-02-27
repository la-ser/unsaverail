package org.laser.ardagone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PartySystem implements CommandExecutor, Listener {

    private FileConfiguration partiesConfig;
    private File partiesFile;
    private final ArdaGone plugin;
    private final Map<Player, Long> clickCooldowns = new HashMap<>();
    private final Map<Player, Party> playerParties = new HashMap<>();
    private final Map<Player, Player> pendingInvitations = new HashMap<>();
    private final Map<Player, Long> inviteCooldown = new HashMap<>();
    private final long inviteCooldownTime = 10000L;
    private final long cooldownTime = 200L;

    public PartySystem(ArdaGone plugin) {
        this.plugin = plugin;
        partiesFile = new File(plugin.getDataFolder(), "parties.yml");
        if (!partiesFile.exists()) {
            plugin.saveResource("parties.yml", false);
        }
        partiesConfig = YamlConfiguration.loadConfiguration(partiesFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("players")) {
            openPlayerListGUI(player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("getParty")) {
            Party party;
            if (args.length != 1) {
                party = getPlayerPartyFromConfig(player.getName());
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                party = getPlayerPartyFromConfig(target.getName());
            }

            if (party != null) {
                player.sendMessage("PARTY FOUND");
            } else {
                player.sendMessage("PARTY NOT FOUND");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("invite")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /invite <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            sendInvite(player, target);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("accept")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /accept <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            acceptInvite(target, player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("clearparty")) {
            Party playerParty = getPlayerPartyFromConfig(player.getName());
            if (playerParty == null) {
                player.sendMessage(ChatColor.RED + "» You are not in any party");
                return true;
            }
            removeParty(player);
            player.sendMessage(ChatColor.GREEN + "» You cleared the party");
            return true;
        }

        return false;
    }

    private final String playerGUITitle = "Online Players";

    public void openPlayerListGUI(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.sort(onlinePlayers, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

        int numPlayers = onlinePlayers.size();
        int numRows = (numPlayers / 9) + 1;

        if (numRows > 6) {
            numRows = 6;
        }

        Inventory gui = Bukkit.createInventory(player, numRows * 9, playerGUITitle);

        for (Player onlinePlayer : onlinePlayers) {
            ItemStack playerHead = createPlayerHead(onlinePlayer.getName());
            gui.addItem(playerHead);
        }

        player.openInventory(gui);
    }

    private final String inviteGUITitle = "Invite ";
    public void openPlayerInviteGUI(Player player, Player target) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, inviteGUITitle + ChatColor.DARK_GRAY +  ChatColor.BOLD + target.getName());
            inv.setItem(0, createPlayerHead(target.getName()));

            ItemStack placeholderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta placeholderItemMeta = placeholderItem.getItemMeta();
            placeholderItemMeta.setDisplayName(" ");
            placeholderItem.setItemMeta(placeholderItemMeta);
            inv.setItem(1, placeholderItem);
            inv.setItem(3, placeholderItem);

            ItemStack inviteItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta inviteItemMeta = inviteItem.getItemMeta();
            inviteItemMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "SEND INVITE");
            inviteItem.setItemMeta(inviteItemMeta);
            inv.setItem(2, inviteItem);

            ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta cancelItemMeta = cancelItem.getItemMeta();
            cancelItemMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL");
            cancelItem.setItemMeta(cancelItemMeta);
            inv.setItem(4, cancelItem);

            player.openInventory(inv);
        }, 1L);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) return;

        if (event.getView().getTitle().equals(playerGUITitle)) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.1f, 1f);
            ItemStack clickedItem = event.getCurrentItem();

            if (event.getClick() == ClickType.LEFT && event.getRawSlot() < event.getView().getTopInventory().getSize() &&
                    clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {

                ItemMeta itemMeta = clickedItem.getItemMeta();
                if (itemMeta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) itemMeta;
                    String clickedPlayerName = ChatColor.stripColor(skullMeta.getDisplayName());
                    Player getClickedPlayer = Bukkit.getPlayer(clickedPlayerName);

                    if (!player.getName().equals(clickedPlayerName)) {
                        /*sendInvite(clickingPlayer, getClickedPlayer);*/
                        Party playerParty = getPlayerPartyFromConfig(player.getName());
                        Party invitedParty = getPlayerPartyFromConfig(getClickedPlayer.getName());
                        if (playerParty == null && invitedParty == null) {
                            openPlayerInviteGUI(player, getClickedPlayer);
                        } else {
                            player.sendMessage(ChatColor.RED + "» You/"+getClickedPlayer.getName() + " already have a party!");
                        }
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatColor.RED + "» You clicked on your own head!");
                    }
                }
            }
            event.setCancelled(true);
        }  else if (event.getView().getTitle().startsWith(inviteGUITitle)) {
            int rawSlot = event.getRawSlot();
            if (rawSlot != 2 && rawSlot != 4) {
                event.setCancelled(true);
            }

            String inventoryTitle = event.getView().getTitle();

            if (rawSlot == 2) {
                String targetPlayerName = ChatColor.stripColor(inventoryTitle.substring(inviteGUITitle.length()));
                Player targetPlayer = plugin.getServer().getPlayerExact(targetPlayerName);

                if (targetPlayer != null) {
                    player.sendMessage(ChatColor.GREEN + "Invite sent to " + targetPlayer.getName());
                    sendInvite(player, targetPlayer);
                }
                player.closeInventory();
            } else if (rawSlot == 4) { // Clicked on "CANCEL"
                player.sendMessage(ChatColor.RED + "Invite cancelled");
                player.closeInventory();
            }
        }
    }

    private ItemStack createPlayerHead(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        meta.setOwner(playerName);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GRAY + playerName);

        playerHead.setItemMeta(meta);

        return playerHead;
    }

    private boolean hasCooldown(Player player) {
        if (clickCooldowns.containsKey(player)) {
            long lastClickTime = clickCooldowns.get(player);
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastClickTime) < cooldownTime;
        }
        return false;
    }

    private void setCooldown(Player player) {
        clickCooldowns.put(player, System.currentTimeMillis());
    }

    public void setParty(Player leader, Player member) {
        if (playerParties.containsKey(leader) || playerParties.containsKey(member)) {
            leader.sendMessage(ChatColor.RED + "» One of the players is already in a party.");
            return;
        }

        if (leader.equals(member)) {
            leader.sendMessage(ChatColor.RED + "» You cannot create a party with yourself.");
            return;
        }

        Party party = new Party(leader);
        party.addMember(member);

        String partyName = leader.getName();
        party.saveToConfig(partiesConfig, partyName);
        savePartiesConfig();

        leader.sendMessage(ChatColor.GREEN + "» You have created a party with " + member.getName() + ".");
        member.sendMessage(ChatColor.GREEN + "» You have joined a party with " + leader.getName() + ".");
    }

    public void getParty(Player player, Player target) {
        Party party = getPlayerPartyFromConfig(target.getName());

        if (target != player) {
            if (party != null) {
                player.sendMessage(ChatColor.GREEN + "» Party Leader: " + party.getLeader().getName());
                player.sendMessage(ChatColor.GREEN + "» Party Members:");
                for (Player member : party.getMembers()) {
                    player.sendMessage(ChatColor.GREEN + "- " + member.getName());
                }
            } else {
                player.sendMessage(ChatColor.RED + "» You and " + target.getName() + " are not in any parties.");
            }
        } else {
            party = getPlayerPartyFromConfig(player.getName());

            if (party != null) {
                player.sendMessage(ChatColor.GREEN + "» our Party Leader: " + party.getLeader().getName());
                player.sendMessage(ChatColor.GREEN + "» Your Party Members:");
                for (Player member : party.getMembers()) {
                    player.sendMessage(ChatColor.GREEN + "- " + member.getName());
                }
            } else {
                player.sendMessage(ChatColor.RED + "» You are not in any parties.");
            }
        }
    }

    public Party getPlayerPartyFromConfig(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        assert player != null;

        for (String partyName : partiesConfig.getConfigurationSection("parties").getKeys(false)) {
            String leaderName = partiesConfig.getString("parties." + partyName + ".leader");
            List<String> memberNames = partiesConfig.getStringList("parties." + partyName + ".members");

            if (leaderName.equals(playerName) || memberNames.contains(playerName)) {
                Player leader = Bukkit.getPlayer(leaderName);

                if (leader != null && leader.isOnline()) {
                    Party party = new Party(leader);

                    for (String memberName : memberNames) {
                        Player member = Bukkit.getPlayer(memberName);

                        if (member != null && member.isOnline()) {
                            party.addMember(member);
                        }
                    }
                    savePartiesConfig();
                    return party;
                }
            }
        }

        return null;
    }

    public static class Party {
        private String leaderName;
        private List<String> memberNames;

        public Party(Player leader) {
            this.leaderName = leader.getName();
            this.memberNames = new ArrayList<>();
        }

        public void addMember(Player member) {
            memberNames.add(member.getName());
        }

        public boolean isFull() {
            return memberNames.size() >= 2;
        }

        public Player getLeader() {
            return Bukkit.getPlayer(leaderName);
        }

        public List<Player> getMembers() {
            List<Player> members = new ArrayList<>();
            for (String memberName : memberNames) {
                Player member = Bukkit.getPlayer(memberName);
                if (member != null && member.isOnline()) {
                    members.add(member);
                }
            }
            return members;
        }

        public void assignLeader(Player newLeader) {
            leaderName = newLeader.getName();
        }

        public void saveToConfig(FileConfiguration config, String partyName) {
            config.set("parties." + partyName + ".leader", leaderName);
            config.set("parties." + partyName + ".members", memberNames);
        }

        public Party loadFromConfig(FileConfiguration config, String partyName) {
            String leaderName = config.getString("parties." + partyName + ".leader");
            List<String> memberNames = config.getStringList("parties." + partyName + ".members");
            Player leader = Bukkit.getPlayer(leaderName);

            if (leader != null && leader.isOnline()) {
                Party party = new Party(leader);
                party.memberNames = memberNames;
                return party;
            }

            return null;
        }
    }


    public void saveParty(Party party) {
        String leaderName = party.getLeader().getName();
        List<String> memberNames = new ArrayList<>();

        for (Player member : party.getMembers()) {
            memberNames.add(member.getName());
        }

        partiesConfig.set("parties." + leaderName + ".leader", leaderName);
        partiesConfig.set("parties." + leaderName + ".members", memberNames);

        savePartiesConfig();
    }

    public void savePartiesConfig() {
        try {
            partiesConfig.save(partiesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save parties.yml!");
        }
    }

    public void removeParty(Party party) {
        String targetLeaderName = party.getLeader().getName();
        List<String> targetMemberNames = new ArrayList<>(party.getMembers().stream().map(Player::getName).collect(Collectors.toList()));

        ConfigurationSection partiesSection = partiesConfig.getConfigurationSection("parties");
        if (partiesSection != null) {
            for (String partyName : partiesSection.getKeys(false)) {
                String leaderName = partiesConfig.getString("parties." + partyName + ".leader");
                List<String> memberNames = partiesConfig.getStringList("parties." + partyName + ".members");

                if (leaderName.equals(targetLeaderName) && memberNames.containsAll(targetMemberNames)) {
                    partiesConfig.set("parties." + partyName, null);
                    break;
                }
            }
        }

        savePartiesConfig();
    }

    public void sendInvite(Player inviter, Player invited) {
        if (inviter.equals(invited)) {
            inviter.sendMessage(ChatColor.RED + "» You cannot invite yourself to a party.");
            return;
        }

        Party inviterParty = getPlayerPartyFromConfig(inviter.getName());
        Party invitedParty = getPlayerPartyFromConfig(invited.getName());

        if (inviterParty != null || invitedParty != null) {
            inviter.sendMessage(ChatColor.RED + "» You or " + invited.getName() + " is already in a party.");
            return;
        }

        if (hasInviteCooldown(inviter, invited)) {
            inviter.sendMessage(ChatColor.RED + "» " + invited.getName() + " already has a pending invitation.");
            return;
        }

        invitedParty = playerParties.computeIfAbsent(invited, key -> new Party(inviter));
        pendingInvitations.put(invited, inviter);
        invited.sendMessage(ChatColor.GREEN + "» You have received an invitation from " + inviter.getName() + "." + ChatColor.GOLD + "\nType /accept " + inviter.getName() + " to join.");
        inviter.sendMessage(ChatColor.GREEN + "» Invitation sent to " + invited.getName() + ".");

        setInviteCooldown(inviter, invited);
    }

    public boolean hasInviteCooldown(Player inviter, Player invited) {
        if (inviteCooldown.containsKey(inviter) && inviteCooldown.containsKey(invited)) {
            long currentTime = System.currentTimeMillis();
            long inviterCooldown = inviteCooldown.get(inviter);
            long invitedCooldown = inviteCooldown.get(invited);

            return currentTime - inviterCooldown < inviteCooldownTime || currentTime - invitedCooldown < inviteCooldownTime;
        }
        return false;
    }

    public void setInviteCooldown(Player inviter, Player invited) {
        long currentTime = System.currentTimeMillis();
        inviteCooldown.put(inviter, currentTime);
        inviteCooldown.put(invited, currentTime);
    }

    public void acceptInvite(Player inviter, Player invited) {
        if (inviter.equals(invited)) {
            invited.sendMessage(ChatColor.RED + "» You cannot accept your own invitation.");
            return;
        }

        Party inviterParty = playerParties.get(inviter);
        Party invitedParty = playerParties.get(invited);

        if (inviterParty != null && inviterParty.isFull()) {
            invited.sendMessage(ChatColor.RED + "» You cannot join this party because " + inviter.getName() + " is already in a full party.");
            return;
        }

        if (pendingInvitations.containsKey(invited) && pendingInvitations.get(invited).equals(inviter)) {
            if (invitedParty == null) {
                invitedParty = new Party(inviter);
            }
            invitedParty.addMember(invited);

            if (invitedParty.getLeader() == null) {
                invitedParty.assignLeader(inviter);
            }

            playerParties.put(inviter, invitedParty);

            invitedParty.saveToConfig(partiesConfig, inviter.getName());
            savePartiesConfig();

            pendingInvitations.remove(invited);
            invited.sendMessage(ChatColor.GREEN + "» You joined the party of " + inviter.getName());
            inviter.sendMessage(ChatColor.GREEN + "» " + invited.getName() + " joined your party");
        } else {
            invited.sendMessage(ChatColor.RED + "» You don't have a pending invitation from " + inviter.getName() + ".");
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();
        removeParty(quittingPlayer);
    }

    public void removeParty(Player player) {
        Party party = getPlayerPartyFromConfig(player.getName());

        if (party != null) {
            removeParty(party);

            playerParties.remove(player);
        }
    }
}
