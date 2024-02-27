package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Tasks implements Listener, CommandExecutor, TabCompleter {
    private final ArdaGone plugin;

    private final FileConfiguration dialoguesConfig;
    private final NamespacedKey dialogueSectionKey;
    private final NamespacedKey guiSectionKey;
    private final NamespacedKey villagerTagKey;

    private Set<String> dialogueKeys;
    private Set<String> guiKeys;

    private final File GUIconfigFile;
    private FileConfiguration GUIconfig;
    private final GUIManager guiManager;


    public Tasks(ArdaGone plugin, Tasks tasks) {
        this.plugin = plugin;
        this.guiManager = new GUIManager(plugin, tasks, new MoneyManager(plugin));
        this.dialoguesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "dialogues.yml"));
        this.villagerTagKey = new NamespacedKey(plugin, "villagerTag");
        this.dialogueSectionKey = new NamespacedKey(plugin, "dialogueSection");
        this.guiSectionKey = new NamespacedKey(plugin, "guiSection");
        this.dialogueKeys = new HashSet<>();
        this.guiKeys = new HashSet<>();

        this.GUIconfigFile = new File(plugin.getDataFolder(), "guis.yml");
        this.GUIconfig = YamlConfiguration.loadConfiguration(GUIconfigFile);

        initializeTabCompletions();
    }

    private void initializeTabCompletions() {
        ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
        if (dialogueSection != null) {
            dialogueKeys = dialogueSection.getKeys(false);
        }
        ConfigurationSection guiSection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());
        if (guiSection != null) {
            guiKeys = guiSection.getKeys(false);
        }
        setTabCompletions("setdialogue", dialogueKeys);
        setTabCompletions("setgui", guiKeys);
        setTabCompletions("removedialogue", dialogueKeys);
        setTabCompletions("getdialoguebook", dialogueKeys);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("summonvillager") || cmd.getAliases().contains("summonvillager")) {
            String villagerName = "";
            String villagerId = "";
            if (args.length == 1) {
                villagerName = ChatColor.YELLOW + "NPC";
            } else if (args.length > 1) {
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                villagerName = ChatColor.translateAlternateColorCodes('&', name);
            } else {
                player.sendMessage(ChatColor.RED + "err: <id> <name>");
                return true;
            }
            villagerId = args[0];

            Location location = player.getLocation();
            Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

            villager.setAI(false);
            villager.setCustomName(villagerName);
            villager.setCustomNameVisible(true);
            villager.setProfession(Villager.Profession.FARMER);
            villager.setCanPickupItems(false);
            villager.setCollidable(false);
            villager.setAgeLock(true);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            villager.setCanPickupItems(false);
            villager.getPersistentDataContainer().set(villagerTagKey, PersistentDataType.STRING, villagerId);

            player.sendMessage(ChatColor.GREEN + "Summoned villager! [" + villagerId + "]");

        } else if (cmd.getName().equalsIgnoreCase("setdialogue") || cmd.getAliases().contains("setdialogue")) {
            if (args.length == 1) {
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType() == Material.WRITABLE_BOOK) {
                    BookMeta bookMeta = (BookMeta) heldItem.getItemMeta();
                    if (bookMeta != null) {
                        List<String> pages = new ArrayList<>();
                        for (String page : bookMeta.getPages()) {
                            String formattedPage = formatPageContent(page, player.getName());
                            pages.add(formattedPage);
                        }
                        String dialogueKey = args[0];

                        ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                        if (dialogueSection == null) {
                            dialogueSection = dialoguesConfig.createSection(dialogueSectionKey.getKey());
                        }
                        dialogueSection.set(dialogueKey, pages);

                        saveConfig();

                        player.sendMessage(ChatColor.GREEN + "Dialogue saved for villager: " + dialogueKey);
                        refreshTabCompletions();
                    } else {
                        player.sendMessage(ChatColor.RED + "The held item is not a writable book.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You must be holding a writable book to set the dialogue.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /setdialogue <villagername>");
            }
        } else if (cmd.getName().equalsIgnoreCase("removedialogue") || cmd.getAliases().contains("removedialogue")) {
            if (args.length == 1) {
                String dialogueKey = args[0];
                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                if (dialogueSection != null && dialogueSection.contains(dialogueKey)) {
                    dialogueSection.set(dialogueKey, null);
                    saveConfig();
                    player.sendMessage(ChatColor.GREEN + "Dialogue removed: " + dialogueKey);
                } else {
                    player.sendMessage(ChatColor.RED + "Dialogue not found: " + dialogueKey);
                }

                refreshTabCompletions();
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /removedialogue <dialogueName>");
                listDialogues(player);
            }
        } else if (cmd.getName().equalsIgnoreCase("getdialoguebook") || cmd.getAliases().contains("getdialoguebook")) {
            if (args.length == 1) {
                String dialogueKey = args[0];
                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                if (dialogueSection != null && dialogueSection.contains(dialogueKey)) {
                    List<String> pages = dialogueSection.getStringList(dialogueKey);
                    if (!pages.isEmpty()) {
                        ItemStack book = createWritableBook(pages, dialogueKey);
                        if (book != null) {
                            player.getInventory().addItem(book);
                            player.sendMessage(ChatColor.GREEN + "Dialogue book added to your inventory.");
                            return true;
                        }
                    }
                }
                sender.sendMessage(ChatColor.RED + "Dialogue not found: " + dialogueKey);
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /getdialoguebook <dialogueName>");
            }
        } else if (cmd.getName().equalsIgnoreCase("getdialogues") || cmd.getAliases().contains("getdialogues")) {
            listDialogues(player);
        }  else if (cmd.getName().equalsIgnoreCase("setgui") || cmd.getAliases().contains("setgui")) {
            if (args.length == 2) {
                String villagerID = args[0];
                String GUIid = args[1];
                String guiName = guiManager.getGUIName(GUIid);

                ConfigurationSection guiSection = GUIconfig.getConfigurationSection(GUIid);
                if (guiSection != null) {
                    ConfigurationSection GUIdialogueSection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());
                    if (GUIdialogueSection == null) {
                        GUIdialogueSection = dialoguesConfig.createSection(guiSectionKey.getKey());
                    }
                    GUIdialogueSection.set(villagerID, GUIid);

                    saveConfig();

                    player.sendMessage(ChatColor.GREEN + "GUI saved for: " + villagerID);
                    refreshTabCompletions();

                } else {
                    player.sendMessage(ChatColor.RED + "GUI doesnt exist!");
                }
            } else if (args.length == 1) {
                String villagerID = args[0];
                ConfigurationSection GUIdialogueSection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());
                if (GUIdialogueSection == null) {
                    GUIdialogueSection = dialoguesConfig.createSection(guiSectionKey.getKey());
                }
                GUIdialogueSection.set(villagerID, null);
                saveConfig();

                player.sendMessage(ChatColor.RED + "GUI reset for: " + villagerID);
                refreshTabCompletions();
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /setgui <villagername> <gui>");
            }
        }

        return true;
    }

    public void refreshTabCompletions() {
/*
        Bukkit.getLogger().info("REFRESHING TAB COMPLETIONS!");
*/
        ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
        if (dialogueSection != null) {
            dialogueKeys = dialogueSection.getKeys(false);
        }
        ConfigurationSection guiSection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());
        if (guiSection != null) {
            guiKeys = guiSection.getKeys(false);
        }
        setTabCompletions("setdialogue", dialogueKeys);
        setTabCompletions("setgui", guiKeys);
        setTabCompletions("removedialogue", dialogueKeys);
        setTabCompletions("getdialoguebook", dialogueKeys);
    }

    private void setTabCompletions(String commandName, Set<String> completions) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setTabCompleter((sender, cmd, alias, args) -> {
                if (args.length == 1) {
                    return completions.stream().filter(completion -> completion.startsWith(args[0])).collect(Collectors.toList());
                } else if (args.length == 2) {
                    if (commandName.equalsIgnoreCase("setgui")) {
                        Set<String> guiSections = GUIconfig.getKeys(false);
                        return guiSections.stream().filter(completion -> completion.startsWith(args[1])).collect(Collectors.toList());
                    } else {
                        return completions.stream().filter(completion -> completion.startsWith(args[1])).collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();
            });
        }
    }


    private void listDialogues(Player player) {
        ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
        if (dialogueSection != null) {
            Set<String> dialogueKeys = dialogueSection.getKeys(false);
            if (!dialogueKeys.isEmpty()) {
                for (String dialogueKey : dialogueKeys) {
                    player.sendMessage(ChatColor.GOLD+ "| "+dialogueKey);
                }
            } else {
                player.sendMessage(ChatColor.RED+"No dialogues found.");
            }
        } else {
            player.sendMessage(ChatColor.RED+"No dialogue section found.");
        }
    }


    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getRightClicked();
            String villagerTag = villager.getPersistentDataContainer().get(villagerTagKey, PersistentDataType.STRING);

            if (hasTag(villager, "npc:shop")) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                player.sendMessage(ChatColor.GRAY + "\n» " + ChatColor.WHITE + "Welcome, " + ChatColor.YELLOW + player.getName() + "\n ");

                player.playSound(player, Sound.ENTITY_VILLAGER_TRADE, 0.3f, 1.2f);
            } else if (villagerTag != null) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                player.playSound(player, Sound.ENTITY_VILLAGER_TRADE, 0.3f, 1.2f);

                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                ConfigurationSection GUISection = dialoguesConfig.getConfigurationSection(guiSectionKey.getKey());

                if (dialogueSection != null && dialogueSection.contains(villagerTag)) {
                    List<String> pages = dialogueSection.getStringList(villagerTag);
                    if (!pages.isEmpty()) {
                        openDialogueBook(player, pages, villagerTag);
                        return;
                    }
                } else if (GUISection != null && GUISection.contains(villagerTag)) {
                    String argument = GUISection.getString(villagerTag);
                    if (argument != null) {
                        guiManager.openSavedGUI(player, argument);
                        return;
                    }
                }

                player.sendMessage(ChatColor.RED + "No dialogue/gui found for villager: " + villagerTag);
            }
        }
    }

    private ItemStack createWritableBook(List<String> pages, String dialogueKey) {
        try {
            ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
            BookMeta bookMeta = (BookMeta) book.getItemMeta();

            bookMeta.setTitle("Dialogue");
            bookMeta.setAuthor("NPC");

            List<String> formattedPages = new ArrayList<>();
            for (String page : pages) {
                String formattedPage = formatPageContentBack(page);
                formattedPages.add(formattedPage);
            }
            bookMeta.setPages(formattedPages);

            bookMeta.setDisplayName(ChatColor.GRAY + "Dialogue Book " + ChatColor.YELLOW + "[ " + dialogueKey + " ]");

            book.setItemMeta(bookMeta);

            return book;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String formatPageContentBack(String page) {
        String formattedPage = page;
        formattedPage = formattedPage.replace("§", "&");
        return formattedPage;
    }


    public void openDialogueBook(Player player, List<String> pages, String dialogueKey) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        String playerName = player.getName();

        bookMeta.setTitle("Dialogue");
        bookMeta.setAuthor("NPC");

        List<String> formattedPages = new ArrayList<>();
        for (String page : pages) {
            String formattedPage = formatPageContentInteract(page, playerName);
            formattedPages.add(formattedPage);
        }
        bookMeta.setPages(formattedPages);

        String displayName = ChatColor.YELLOW + "Dialogue Book";
        bookMeta.setDisplayName(displayName);

        book.setItemMeta(bookMeta);

        player.openBook(book);
    }


    private String formatPageContentInteract(String page, String playerName) {
        String formattedPage = page;
        formattedPage = formattedPage.replace("\"", "");
        formattedPage = formattedPage.replace("\\n", "\n");;
        formattedPage = formattedPage.replace("<pName>", playerName);
        formattedPage = ChatColor.translateAlternateColorCodes('&', formattedPage);
        return formattedPage;
    }

    private String formatPageContent(String page, String playerName) {
        String formattedPage = page;
        formattedPage = formattedPage.replace("\"", "");
        formattedPage = formattedPage.replace("\\n", "\n");
        formattedPage = ChatColor.translateAlternateColorCodes('&', formattedPage);
        return formattedPage;
    }

    private void saveConfig() {
        try {
            File dialoguesFile = new File(plugin.getDataFolder(), "dialogues.yml");
            dialoguesConfig.save(dialoguesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean hasTag(Villager villager, String tag) {
        PersistentDataContainer dataContainer = villager.getPersistentDataContainer();
        return dataContainer.has(villagerTagKey, PersistentDataType.STRING)
                && dataContainer.get(villagerTagKey, PersistentDataType.STRING).equals(tag);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("setdialogue") || command.getAliases().contains("setdialogue")) {
            if (args.length == 1) {
                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                if (dialogueSection != null) {
                    Set<String> dialogueKeys = dialogueSection.getKeys(false);
                    completions.addAll(dialogueKeys);
                }
            }
        } else if (command.getName().equalsIgnoreCase("removedialogue") || command.getAliases().contains("removedialogue")) {
            if (args.length == 1) {
                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                if (dialogueSection != null) {
                    Set<String> dialogueKeys = dialogueSection.getKeys(false);
                    completions.addAll(dialogueKeys);
                }
            }
        } else if (command.getName().equalsIgnoreCase("getdialoguebook") || command.getAliases().contains("getdialoguebook")) {
            if (args.length == 1) {
                ConfigurationSection dialogueSection = dialoguesConfig.getConfigurationSection(dialogueSectionKey.getKey());
                if (dialogueSection != null) {
                    Set<String> dialogueKeys = dialogueSection.getKeys(false);
                    completions.addAll(dialogueKeys);
                }
            }
        } else if (command.getName().equalsIgnoreCase("setgui") || command.getAliases().contains("setgui")) {
            if (args.length == 2) {
                if (GUIconfig != null) {
                    Set<String> guiSections = GUIconfig.getKeys(false);
                    completions.addAll(guiSections);
                }
            }
        }

        return completions;
    }


}