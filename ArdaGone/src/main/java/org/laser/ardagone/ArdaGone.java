package org.laser.ardagone;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public final class ArdaGone extends JavaPlugin implements CommandExecutor, Listener {

    public static List<String> players = new ArrayList<>();
    private StartCommand startCommand;
    private PlayerJoinEventHandler playerJoinEventHandler;
    private PlayerLeaveEventHandler playerLeaveEventHandler;
    private CustomFishingRod customFishingRod;
    private LootBoxes lootBoxes;
    private MoneyManager moneyManager;
    private Tasks tasks;
    private GUIManager guiManager;
    private PartySystem partySystem;
    private PublicVars publicVars;
    private ClickableSigns clickableSigns;
    private InteractiveBlocks interactiveBlocks;
    private CharacterManager characterManager;
    private inGame inGame;
    private StatusManager statusManager;
    private static ArdaGone instance;
    public boolean gameStarted = false;

    private FileConfiguration partiesConfig;
    private File partiesFile;

    private FileConfiguration statusConfig;
    private File statusFile;

    @Override
    public void onEnable() {
        configureGameRules("world");
        configureGameRules("game");

        World world = Bukkit.getWorld("world");

        if (world != null) {
            world.setPVP(false);
        } else {
            getLogger().warning("The specified world was not found.");
        }

        partiesFile = new File(this.getDataFolder(), "parties.yml");
        if (!partiesFile.exists()) {
            this.saveResource("parties.yml", false);
        }
        partiesConfig = YamlConfiguration.loadConfiguration(partiesFile);

        statusFile = new File(this.getDataFolder(), "status.yml");
        if (!statusFile.exists()) {
            this.saveResource("status.yml", false);
        }
        statusConfig = YamlConfiguration.loadConfiguration(statusFile);

        getLogger().info("UnknownGame initialized!");
        instance = this;

        getServer().getPluginManager().registerEvents(this, this);

        startCommand = new StartCommand(this);
        getServer().getPluginManager().registerEvents(startCommand, this);
        getCommand("start").setExecutor(startCommand);

        getCommand("setgamespawnpoint").setExecutor(this);

        playerJoinEventHandler = new PlayerJoinEventHandler(this);
        getServer().getPluginManager().registerEvents(playerJoinEventHandler, this);

        playerLeaveEventHandler = new PlayerLeaveEventHandler(this);
        getServer().getPluginManager().registerEvents(playerLeaveEventHandler, this);

        customFishingRod = new CustomFishingRod(this);
        getServer().getPluginManager().registerEvents(customFishingRod, this);

        lootBoxes = new LootBoxes(this);
        getServer().getPluginManager().registerEvents(lootBoxes, this);
        getCommand("givecrate").setExecutor(lootBoxes);

        moneyManager = new MoneyManager(this);

        tasks = new Tasks(this, tasks);
        getCommand("summonvillager").setExecutor(tasks);
        getCommand("setdialogue").setExecutor(tasks);
        getCommand("setgui").setExecutor(tasks);
        getCommand("removedialogue").setExecutor(tasks);
        getCommand("getdialoguebook").setExecutor(tasks);
        getCommand("getdialogues").setExecutor(tasks);
        getServer().getPluginManager().registerEvents(tasks, this);

        guiManager = new GUIManager(this, tasks, moneyManager);
        getCommand("createnewgui").setExecutor(guiManager);
        getCommand("deletegui").setExecutor(guiManager);
        getCommand("opengui").setExecutor(guiManager);
        getCommand("edititem").setExecutor(guiManager);
        getCommand("gui").setExecutor(guiManager);
        getCommand("setguiname").setExecutor(guiManager);
        getCommand("setprice").setExecutor(guiManager);
        getServer().getPluginManager().registerEvents(guiManager, this);

        clickableSigns = new ClickableSigns(this, tasks);
        getCommand("setsign").setExecutor(clickableSigns);
        getServer().getPluginManager().registerEvents(clickableSigns, this);

        interactiveBlocks = new InteractiveBlocks(this, tasks);
        getCommand("setinteractiveblock").setExecutor(interactiveBlocks);
        getCommand("removeinteractiveblock").setExecutor(interactiveBlocks);
        getServer().getPluginManager().registerEvents(interactiveBlocks, this);

        characterManager = new CharacterManager(this);
        getCommand("createcharacter").setExecutor(characterManager);
        getCommand("listcharacters").setExecutor(characterManager);
        getCommand("checkchars").setExecutor(characterManager);
        getCommand("character").setExecutor(characterManager);
        getCommand("selectcharacter").setExecutor(characterManager);
        getCommand("getchar").setExecutor(characterManager);
        getCommand("setcharitems").setExecutor(characterManager);
        getCommand("character").setTabCompleter(characterManager);
        getServer().getPluginManager().registerEvents(characterManager, this);

        partySystem = new PartySystem(this);
        getCommand("players").setExecutor(partySystem);
        getCommand("getParty").setExecutor(partySystem);
        getCommand("invite").setExecutor(partySystem);
        getCommand("accept").setExecutor(partySystem);
        getCommand("clearparty").setExecutor(partySystem);
        getServer().getPluginManager().registerEvents(partySystem, this);

        inGame = new inGame(this);
        getCommand("test").setExecutor(inGame);
        getServer().getPluginManager().registerEvents(inGame, this);

        statusManager = new StatusManager(this);
        getCommand("setStatus").setExecutor(statusManager);
        getServer().getPluginManager().registerEvents(statusManager, this);
    }

    private void configureGameRules(String worldName) {
        World world = getServer().getWorld(worldName);

        if (world != null) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_TILE_DROPS, false);
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.DO_FIRE_TICK, false);
            world.setGameRule(GameRule.SPAWN_RADIUS, -1);
        } else {
            getLogger().warning("World '" + worldName + "' does not exist. Game rules not configured.");
        }
    }


    @Override
    public void onDisable() {
        World world = Bukkit.getWorld("world");

        if (world != null) {
            world.setPVP(true);
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        List<Team> teamsToRemove = new ArrayList<>(scoreboard.getTeams());

        for (Team team : teamsToRemove) {
            team.unregister();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setDisplayName(null);
        }

        clearPartiesYAMLFile(partiesConfig, partiesFile);
        clearStatusYAMLFile(statusConfig, statusFile);
    }

    /*@EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = (Player) event.getEntity();
        if (!UnknownGame.players.contains(player.getName())) {
            event.setDeathMessage("");
            playerJoinEventHandler.resetPlayer(player);
        }
    }*/

    public void clearPartiesYAMLFile(FileConfiguration config, File file) {
        config.set("parties", new HashMap<>());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearStatusYAMLFile(FileConfiguration config, File file) {
        /*config.set("status", new HashMap<>());*/
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("join")) {
            joinGame(player);
        } else if (command.getName().equalsIgnoreCase("leave")) {
            leaveGame(player);
        } else if (command.getName().equalsIgnoreCase("setgamespawnpoint")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /setgamespawnpoint <number>");
                return true;
            }

            int spawnPointNumber;
            try {
                spawnPointNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid number format.");
                return true;
            }

            File gameSpawnFile = new File(this.getDataFolder(), "gamespawn.yml");
            FileConfiguration gameSpawnConfig = YamlConfiguration.loadConfiguration(gameSpawnFile);

            double x = player.getLocation().getBlockX() + 0.5;
            double y = player.getLocation().getBlockY() + 0.5;
            double z = player.getLocation().getBlockZ() + 0.5;
            float yaw = player.getLocation().getYaw();

            yaw = Math.round(yaw * 10) / 10.0f;

            gameSpawnConfig.set("spawnpoints." + spawnPointNumber + ".world", player.getLocation().getWorld().getName());
            gameSpawnConfig.set("spawnpoints." + spawnPointNumber + ".x", x);
            gameSpawnConfig.set("spawnpoints." + spawnPointNumber + ".y", y);
            gameSpawnConfig.set("spawnpoints." + spawnPointNumber + ".z", z);
            gameSpawnConfig.set("spawnpoints." + spawnPointNumber + ".yaw", yaw);

            try {
                gameSpawnConfig.save(gameSpawnFile);
                player.sendMessage("Game spawn point " + spawnPointNumber + " saved at X: " + x + ", Y: " + y + ", Z: " + z + ", Yaw: " + yaw);
            } catch (IOException e) {
                player.sendMessage("An error occurred while saving the configuration.");
                e.printStackTrace();
            }

            return true;
        }


        return true;
    }

    public static ItemStack getHeadFromURL(String headName, String url) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", new String(Base64.getEncoder().encode(String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url).getBytes()))));

        Field profileField;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        meta.setDisplayName(ChatColor.WHITE + headName);
        stack.setItemMeta(meta);

        return stack;
    }

    public static ItemStack getPlayerHead(String playerName, String headName) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        meta.setOwner(playerName);

        meta.setDisplayName(headName);

        playerHead.setItemMeta(meta);

        return playerHead;
    }

    public void joinGame(Player player) {
        String playerName = player.getName();

        if (gameStarted) {
            Bukkit.getPlayer(playerName).sendMessage(ChatColor.RED + "» The game is already in progress.");
            return;
        }

        if (players.contains(playerName)) {
            Bukkit.getPlayer(playerName).sendMessage(ChatColor.RED + "» You're already joined the game.");
            return;
        }

        player.setHealth(20);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealthScale(20);
        player.setHealthScaled(true);

        /*player.teleport(teleportLocation);*/

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double maxHealthValue = maxHealthAttribute.getValue();
            player.setHealth(maxHealthValue);
        }
        player.setFoodLevel(20);

        players.add(playerName);
        statusManager.setStatus(player, "WAITING");
        Bukkit.getPlayer(playerName).sendMessage(ChatColor.GREEN + "» You've joined the game.");

        /* custom team */
        /*Scoreboard scoreboard = player.getScoreboard();
        String teamName = "joinTeam";
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.setPrefix(ChatColor.GRAY + "Player » ");
        *//*team.setSuffix(ChatColor.RED + " Suffix");*//*
        team.setColor(ChatColor.DARK_GRAY);

        team.addPlayer(player);

        *//*player.setDisplayName(ChatColor.DARK_GRAY + playerName + ChatColor.RESET);*//*

        Objective objective = scoreboard.getObjective(DisplaySlot.PLAYER_LIST);
        if (objective != null) {
            objective.getScore(player.getName()).setScore(0);
        } else {
            getLogger().severe("Objective is null!");
        }*/
    }

    public void leaveGame(Player player) {
        String playerName = player.getName();

        if (gameStarted) {
            Bukkit.getPlayer(playerName).sendMessage(ChatColor.RED + "» The game is still in progress.");
            return;
        }

        if (!players.contains(playerName)) {
            Bukkit.getPlayer(playerName).sendMessage(ChatColor.RED + "» You already left the game.");
            return;
        }

        player.setHealth(20);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealthScale(20);
        player.setHealthScaled(true);

        /*player.teleport(teleportLocation);*/

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double maxHealthValue = maxHealthAttribute.getValue();
            player.setHealth(maxHealthValue);
        }
        player.setFoodLevel(20);

        players.remove(playerName);
        statusManager.removeStatus(player);
        Bukkit.getPlayer(playerName).sendMessage(ChatColor.GREEN + "» You've left the game.");
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.ADVENTURE) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.ADVENTURE) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PRIMED_TNT) {
            event.setCancelled(true);

            TNTPrimed tntPrimed = (TNTPrimed) entity;
            tntPrimed.remove();
            spawnNewTNTBlock(tntPrimed.getLocation());
        }
    }

    private void spawnNewTNTBlock(Location location) {
        new BukkitRunnable() {
            @Override
            public void run() {
                location.getWorld().getBlockAt(location).setType(Material.TNT);
            }
        }.runTask(this);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.PRIMED_TNT) {
            event.setCancelled(true);
        } else if (event.getEntityType() == EntityType.MINECART_TNT) {
            event.setCancelled(true);
        }
    }
}
