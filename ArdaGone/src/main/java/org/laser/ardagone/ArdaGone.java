package org.laser.ardagone;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public final class ArdaGone extends JavaPlugin implements Listener {

    private Characters characters;
    private CharacterManager characterManager;
    private TheFog theFog;
    private SpawnPoints spawnPoints;
    private  DamageSystem damageSystem;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("test").setExecutor(this);

        characters = new Characters(this);

        characterManager = new CharacterManager(this, characters);
        getCommand("createcharacter").setExecutor(characterManager);
        getCommand("listcharacters").setExecutor(characterManager);
        getCommand("checkchars").setExecutor(characterManager);
        getCommand("character").setExecutor(characterManager);
        getCommand("selectcharacter").setExecutor(characterManager);
        getCommand("getchar").setExecutor(characterManager);/*
        getCommand("character").setTabCompleter(characterManager);*/
        getServer().getPluginManager().registerEvents(characterManager, this);

        theFog = new TheFog(this);
        getCommand("fornite").setExecutor(theFog);

        spawnPoints = new SpawnPoints(this);
        getCommand("savespawnpoint").setExecutor(spawnPoints);
        getCommand("randomspawn").setExecutor(spawnPoints);

        damageSystem = new DamageSystem(this, characters);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("test")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openCharacterSelectionGUI(player);
            } else {
                sender.sendMessage("Only players can use this command.");
            }
            return true;
        }
        return false;
    }

    // Method to open the character selection GUI for a player
    private void openCharacterSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, InventoryType.DROPPER, "Character Selection");

        ItemStack character1 = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta1 = character1.getItemMeta();
        meta1.setDisplayName("Henry");
        character1.setItemMeta(meta1);
        gui.setItem(0, character1);

        ItemStack character2 = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta2 = character1.getItemMeta();
        meta2.setDisplayName("Robin");
        character2.setItemMeta(meta2);
        gui.setItem(1, character2);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals("Character Selection")) {
            int slotID = event.getSlot();

            if (slotID >= 8) return;

            event.setCancelled(true);
            characters.selectCharacter(player, slotID);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.NETHER_STAR) {
            Player player = event.getPlayer();
            openCharacterSelectionGUI(player);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() != null && event.getHitBlock().getType() != Material.AIR) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getEntity().remove();
                }
            }.runTaskLater(this, 20);
        }
    }

    public static ItemStack getHeadFromURL(String headName, String url) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setHealthScaled(true);
        player.setHealthScale(20);
    }
}
