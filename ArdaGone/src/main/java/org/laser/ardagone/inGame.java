package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class inGame implements CommandExecutor, Listener {

    private final ArdaGone plugin;
    private final CharacterManager characterManager;
    private final PartySystem partySystem;
    private final File partiesFile;
    private FileConfiguration partiesConfig;

    public inGame(ArdaGone plugin) {
        this.plugin = plugin;
        this.characterManager = new CharacterManager(plugin);
        this.partySystem = new PartySystem(plugin);
        this.partiesFile = new File(plugin.getDataFolder(), "parties.yml");
        this.partiesConfig = YamlConfiguration.loadConfiguration(partiesFile);
    }

    World gameWorld = Bukkit.getWorld("game");
    private final Location game_worldCenter = new Location(gameWorld, -43, 70, -49, 0, 90);

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("test")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /test <world>");
                return true;
            }

            World world = Bukkit.getWorld(args[0]);
            setRandomWorldBorderCenter(world);
            return true;
        }

        return true;
    }

    public void startFunctions() {
        plugin.gameStarted = true;
        World world = Bukkit.getWorld("game");
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(game_worldCenter.getX(), game_worldCenter.getZ());
        worldBorder.setSize(200);
        worldBorder.setSize(17, 120);
        setRandomWorldBorderCenter(world);
    }

    public void stopFunctions() {
        plugin.gameStarted = false;
    }

    public void setRandomWorldBorderCenter(World world) {
        Location corner1 = new Location(world, 0, 66, -6);
        Location corner2 = new Location(world, -86, 66, -92);

        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("Both locations must be in the same world.");
        }

        WorldBorder worldBorder = world.getWorldBorder();

        int x1 = (int) Math.min(corner1.getX(), corner2.getX());
        int z1 = (int) Math.min(corner1.getZ(), corner2.getZ());
        int x2 = (int) Math.max(corner1.getX(), corner2.getX());
        int z2 = (int) Math.max(corner1.getZ(), corner2.getZ());

        Random random = new Random();

        int randomX = x1 + random.nextInt(x2 - x1 + 1);
        int randomZ = z1 + random.nextInt(z2 - z1 + 1);

        worldBorder.setCenter(randomX + 0.5, randomZ + 0.5);
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.gameStarted) return;
        Player player = (Player) event.getEntity();
        if (ArdaGone.players.contains(player.getName())) {
            player.sendMessage(ChatColor.RED + "» You died!");

            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(game_worldCenter);
            }, 10L);
        }
    }

    private final Map<UUID, Long> abilityInteractCooldown = new HashMap<>();
    private final long abilityCooldownTime = 10000L;

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (player.getGameMode() == GameMode.SPECTATOR) return;

        /*if (!plugin.gameStarted) return;*/

        if (!ArdaGone.players.contains(player.getName())) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            //special ability
            if (itemInHand.getType() == Material.BLAZE_ROD) {
                ItemStack firstSlotItem = player.getInventory().getItem(0);

                if (firstSlotItem != null && firstSlotItem != itemInHand) return;

                // special ability cooldown
                if (abilityInteractCooldown.containsKey(playerUUID)) {
                    long lastInteractTime = abilityInteractCooldown.get(playerUUID);
                    long currentTime = System.currentTimeMillis();
                    long remainingCooldown = lastInteractTime + abilityCooldownTime - currentTime;

                    if (remainingCooldown > 0) {
/*
                player.sendMessage(ChatColor.RED + "» You must wait " + String.format("%.2f", (double)remainingCooldown / 1000) + " seconds.");
*/
                        return;
                    }
                }
                player.setVelocity(new Vector(player.getVelocity().getX(), player.getVelocity().getY() + 1, player.getVelocity().getZ()));
                player.playSound(player, Sound.BLOCK_BELL_RESONATE, 0.3f, 2.0f);
                abilityInteractCooldown.put(playerUUID, System.currentTimeMillis());
            } else if (itemInHand.getType() == Material.SNOWBALL) {
                player.playSound(player, Sound.ENTITY_ARROW_SHOOT, 0.3f, 2.0f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    ItemStack firstSlotItem = player.getInventory().getItem(0);

                    if (firstSlotItem == null || firstSlotItem.getType() == Material.AIR) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.getInventory().setItem(0, new ItemStack(Material.SNOWBALL, 24));
                        }, 60L);
                    }
                }, 10L);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Snowball) {
            Entity hitEntity = event.getHitEntity();
            if (hitEntity instanceof Player) {
                Player player = (Player) hitEntity;
                player.sendMessage("You were hit by a snowball!");
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 25, 1, false, false));
            }
        }
    }

}
