package org.laser.ardagone;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GUIManager implements Listener, CommandExecutor, TabCompleter {

    private final ArdaGone plugin;
    private final Tasks tasks;
    private final MoneyManager moneyManager;
    private final File configFile;
    private FileConfiguration config;
    private static final String LIME_PANE_NAME = "Open GUI";
    private static final String CustomGUIName = "Edit GUI: ";
    private static final String SavedGUIName = ChatColor.DARK_GRAY + "» ";
    private static final String ItemEditorName = "Item Editor";
    private static final int[] ALLOWED_SLOTS = { 9, 18, 27, 36, 45, 54 };
    private final NamespacedKey PRICE_NAME_KEY;

    private final File configFilePrices;
    private FileConfiguration configPrices;

    public GUIManager(ArdaGone plugin, Tasks tasks, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.tasks = tasks;
        this.moneyManager = moneyManager;
        this.configFile = new File(plugin.getDataFolder(), "guis.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.configFilePrices = new File(plugin.getDataFolder(), "prices.yml");
        this.configPrices = YamlConfiguration.loadConfiguration(configFilePrices);

        PRICE_NAME_KEY = new NamespacedKey(plugin, "price_name_key");
    }

    private void createNewGUI(CommandSender sender, String id) {
        if (config.contains(id)) {
            sender.sendMessage("GUI with ID '" + id + "' already exists.");
            return;
        }

        config.createSection(id);
        saveConfig();
        sender.sendMessage("Created a new GUI with ID '" + id + "'.");
    }

    private void deleteGUI(CommandSender sender, String id) {
        if (!config.contains(id)) {
            sender.sendMessage("GUI with ID '" + id + "' does not exist.");
            return;
        }

        config.set(id, null);
        saveConfig();
        sender.sendMessage("Deleted the GUI with ID '" + id + "'.");
    }

    public String getGUIName(String guiName) {
        ConfigurationSection guiSection = config.getConfigurationSection(guiName);
        if (guiSection != null) {
            String name = guiSection.getString("name", "");
            return ChatColor.translateAlternateColorCodes('&', name);
        }
        return "";
    }

    private void openGUI(Player player, String id) {
        if (!config.contains(id)) {
            player.sendMessage("GUI with ID '" + id + "' does not exist.");
            return;
        }

        ConfigurationSection guiSection = config.getConfigurationSection(id);

        Inventory guiInventory;

        guiInventory = Bukkit.createInventory(player, 27, "GUI: " + id);

        if (guiSection != null) {
            int slots = guiSection.getInt("slots", 9);

            for (int i = 0; i < ALLOWED_SLOTS.length; i++) {
                int slotSize = ALLOWED_SLOTS[i];

                ItemStack slotButton = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                ItemMeta buttonMeta = slotButton.getItemMeta();
                buttonMeta.setDisplayName(Integer.toString(slotSize));
                slotButton.setItemMeta(buttonMeta);

                guiInventory.setItem(i, slotButton);

                if (slotSize == slots) {
                    slotButton.setType(Material.LIME_STAINED_GLASS_PANE);
                }
            }

            ItemStack limePane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta limeMeta = limePane.getItemMeta();
            limeMeta.setDisplayName(LIME_PANE_NAME);
            limePane.setItemMeta(limeMeta);
            guiInventory.setItem(26, limePane);

            ItemStack writableBook = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta bookMeta = writableBook.getItemMeta();
            bookMeta.setDisplayName(ChatColor.GREEN + ItemEditorName);
            writableBook.setItemMeta(bookMeta);

            guiInventory.setItem(18, writableBook);
        }

        player.openInventory(guiInventory);
    }


    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Error saving guis.yml file: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("createnewgui") || command.getAliases().contains("createnewgui")) {
            if (args.length == 1) {
                createNewGUI(sender, args[0]);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
            } else {
                sender.sendMessage("Usage: /createnewgui <id>");
            }
        } else if (command.getName().equalsIgnoreCase("deletegui") || command.getAliases().contains("deletegui")) {
            if (args.length == 1) {
                deleteGUI(sender, args[0]);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
            } else {
                sender.sendMessage("Usage: /deletegui <id>");
            }
        } else if (command.getName().equalsIgnoreCase("opengui") || command.getAliases().contains("opengui")) {
            if (args.length == 1) {
                openGUI(player, args[0]);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
            } else {
                sender.sendMessage("Usage: /opengui <id>");
            }
        }else if (command.getName().equalsIgnoreCase("edititem") || command.getAliases().contains("edititem")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /edititem name/lore/function/price/hideFlags/enchant <string>\n"+ ChatColor.GRAY +"Use: \\n for adding a new line.");
                return true;
            } else if (args[0].equalsIgnoreCase("function")) {
                setItemFunction(player, Arrays.copyOfRange(args, 1, args.length));
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
                return true;
            } else if (args[0].equalsIgnoreCase("hideFlags")) {
                hideAttributesFromItem(player, args);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
                return true;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("enchant")) {
                if (args[1].equalsIgnoreCase("none")) {
                    removeAllEnchantments(player);
                    return true;
                } else if (args[1].equalsIgnoreCase("hide")) {
                    addGlowEffect(player);
                    return true;
                }
            } else if (args.length >= 3 && args[0].equalsIgnoreCase("enchant")) {
                setEnchantment(player, args[1], args[2]);
                return true;
            }

            setItemNameAndLore(player, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("gui") || command.getAliases().contains("gui")) {
            if (args.length == 1) {
                openSavedGUI(player, args[0]);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
            } else {
                sender.sendMessage("Usage: /opengui <id>");
            }
        } else if (command.getName().equalsIgnoreCase("setguiname") || command.getAliases().contains("setguiname")) {
            if (args.length >= 2) {
                String guiId = args[0];
                String guiName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                setGUIName(sender, guiId, guiName);
                if (this.tasks != null) {
                    this.tasks.refreshTabCompletions();
                }
            } else {
                sender.sendMessage("Usage: /setguiname <gui> <name>");
            }
        } else if (command.getName().equalsIgnoreCase("setprice") || command.getAliases().contains("setprice")) {
            if (args.length < 1) {
                sender.sendMessage("Usage: /setprice <name> [sell/buy] [price] [item_amount]");
                return false;
            }

            String priceName = args[0].toLowerCase();

            File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
            FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
            ConfigurationSection pricesSection = pricesConfig.getConfigurationSection("prices");
            if (pricesSection == null) {
                pricesSection = pricesConfig.createSection("prices");
            }

            if (args.length == 1) {
                if (pricesSection.contains(priceName)) {
                    pricesSection.set(priceName, null);
                    try {
                        pricesConfig.save(pricesFile);
                        reloadPricesConfig();
                        sender.sendMessage("Price '" + priceName + "' removed successfully.");
                    } catch (IOException e) {
                        plugin.getLogger().severe("Unable to save prices.yml.");
                    }
                } else {
                    sender.sendMessage("Price '" + priceName + "' not found in the config.");
                }
                return true;
            }

            String type = args[1].toLowerCase();

            if (!type.equals("sell") && !type.equals("buy")) {
                sender.sendMessage("Invalid price type. Use 'sell' or 'buy'.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("Usage: /setprice <name> sell/buy <price> <item_amount>");
                return false;
            }

            try {
                int priceValue = Integer.parseInt(args[2]);

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                int oldItemAmount = itemInHand.getAmount();
                int itemAmount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
                itemInHand.setAmount(itemAmount);

                pricesSection.set(priceName + ".type", type);
                pricesSection.set(priceName + ".price", priceValue);

                ConfigurationSection itemSection = pricesSection.createSection(priceName + ".item");
                itemSection.set("type", itemInHand.getType().name());
                itemSection.set("amount", itemInHand.getAmount());
                ItemMeta itemMeta = itemInHand.getItemMeta();
                if (itemMeta != null) {
                    if (itemMeta.hasDisplayName()) {
                        itemSection.set("meta.display-name", itemMeta.getDisplayName());
                    }
                    if (itemMeta.hasLore()) {
                        itemSection.set("meta.lore", itemMeta.getLore());
                    }

                    Map<Enchantment, Integer> enchantments = itemMeta.getEnchants();
                    if (!enchantments.isEmpty()) {
                        ConfigurationSection enchantSection = itemSection.createSection("meta.enchants");
                        for (Enchantment enchantment : enchantments.keySet()) {
                            int level = enchantments.get(enchantment);
                            enchantSection.set(enchantment.getName(), level);
                        }
                    }

                    Set<ItemFlag> itemFlags = itemMeta.getItemFlags();
                    if (!itemFlags.isEmpty()) {
                        List<String> itemFlagNames = itemFlags.stream().map(ItemFlag::name).collect(Collectors.toList());
                        itemSection.set("meta.ItemFlags", itemFlagNames);
                    }
                }

                itemInHand.setAmount(oldItemAmount);

                sender.sendMessage("Price set successfully. [" + priceName + "; " + type + "; " + priceValue + "; " + itemAmount + "]");
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid price value or item amount. Please provide valid whole numbers.");
                return true;
            }


            try {
                pricesConfig.save(pricesFile);
                reloadPricesConfig();
            } catch (IOException e) {
                plugin.getLogger().severe("Unable to save prices.yml.");
            }

            return true;
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("deletegui") || command.getAliases().contains("deletegui") && args.length == 1) {
            Set<String> guiIds = config.getKeys(false);
            completions.addAll(guiIds);
        } else if (command.getName().equalsIgnoreCase("opengui") || command.getAliases().contains("opengui") && args.length == 1) {
            Set<String> guiIds = config.getKeys(false);
            completions.addAll(guiIds);
        } else if (command.getName().equalsIgnoreCase("gui") || command.getAliases().contains("gui") && args.length == 1) {
            Set<String> guiIds = config.getKeys(false);
            completions.addAll(guiIds);
        } else if (command.getName().equalsIgnoreCase("edititem") || command.getAliases().contains("edititem")) {
            if (args.length == 1) {
                completions.add("name");
                completions.add("lore");
                completions.add("function");
                completions.add("hideFlags");
                completions.add("enchant");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("enchant")) {
                return getEnchantmentCompletions(args[1]);
            } else if (args.length > 1 && args[0].equalsIgnoreCase("hideFlags")) {
                for (ItemFlag flag : ItemFlag.values()) {
                    completions.add(flag.name());
                }
                completions.add("ALL");
            }
        } else if (command.getName().equalsIgnoreCase("setguiname") || command.getAliases().contains("setguiname") && args.length == 1) {
            Set<String> guiIds = config.getKeys(false);
            completions.addAll(guiIds);
        } else if (command.getName().equalsIgnoreCase("setprice") || command.getAliases().contains("setprice")) {
            if (args.length == 1) {
                return getPriceNameCompletions().stream()
                        .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
                completions.add("sell");
                completions.add("buy");
            }
        }
        return completions;
    }

    private List<String> getEnchantmentCompletions(String startsWith) {
        List<String> completions = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) {
            String enchantmentName = enchantment.getKey().getKey();
            if (startsWith.isEmpty() || enchantmentName.startsWith(startsWith.toLowerCase())) {
                completions.add(enchantmentName);
            }
        }
        completions.add("none");
        completions.add("hide");
        Collections.sort(completions);
        return completions;
    }

    private void openNewGUI(Player player, String id) {
        ConfigurationSection guiSection = config.getConfigurationSection(id);
        if (guiSection != null) {
            int slots = guiSection.getInt("slots", 9);
            ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");

            Inventory newGuiInventory = Bukkit.createInventory(null, slots, CustomGUIName + id);

            if (itemsSection != null) {
                for (String slotString : itemsSection.getKeys(false)) {
                    int slot = Integer.parseInt(slotString);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotString);

                    if (itemSection != null) {
                        ItemStack item = itemSection.getItemStack("item");
                        newGuiInventory.setItem(slot, item);
                    }
                }
            }

            player.openInventory(newGuiInventory);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().startsWith("GUI: ")) {
            event.setCancelled(true);

            int rawSlot = event.getRawSlot();

            String title = event.getView().getTitle();

            if (rawSlot >= 0 && rawSlot < ALLOWED_SLOTS.length) {
                event.setCancelled(true);

                int selectedSlots = ALLOWED_SLOTS[rawSlot];
                String id = title.substring(5);
                ConfigurationSection guiSection = config.getConfigurationSection(id);

                if (guiSection != null) {
                    guiSection.set("slots", selectedSlots);
                    saveConfig();
                }

                openGUI(player, id);
            } else if (rawSlot == 26) {
                ItemStack clickedItem = event.getInventory().getItem(rawSlot);
                if (clickedItem != null && clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
                    ItemMeta limeMeta = clickedItem.getItemMeta();
                    if (limeMeta.getDisplayName().equals(LIME_PANE_NAME)) {
                        String id = title.substring(5);

                        ConfigurationSection guiSection = config.getConfigurationSection(id);
                        if (guiSection != null) {
                            openNewGUI(player, id);
                        }
                    }
                }

            }if (rawSlot == 18) {
                event.setCancelled(true);

                String id = title.substring(5);

                openItemEditorGUI(player, id);
            }
        }if (event.getView().getTitle().equals(ItemEditorName)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.BIRCH_DOOR) {

                String previousGuiTitle = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName().substring(11));
                openGUI(player, previousGuiTitle);
            }
        }if (event.getView().getTitle().startsWith(SavedGUIName)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null) {
                return;
            }

            if (meta.getPersistentDataContainer().has(PRICE_NAME_KEY, PersistentDataType.STRING)) {

                String priceName = meta.getPersistentDataContainer().get(PRICE_NAME_KEY, PersistentDataType.STRING);

                File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
                FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

                ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);
                if (priceSection != null) {
                    String itemName = priceSection.getString("item.type");
                    int itemCount = priceSection.getInt("item.amount");
                    int moneyAmount = priceSection.getInt("price");

                    String priceType = priceSection.getString("type", "sell");
                    if (priceType.equalsIgnoreCase("buy")) {
                        performBuyAction(player, itemName, itemCount, moneyAmount, priceName);
                    } else if (priceType.equalsIgnoreCase("sell")) {
                        performSellAction(player, itemName, itemCount, moneyAmount, priceName);
                    } else {
                        player.sendMessage("err.wrongType " + priceType);
                    }
                }
            }
        }
    }

    private void performBuyAction(Player player, String itemName, int itemCount, int moneyAmount, String priceName) {
        File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);

        long playerMoney = moneyManager.getMoney(player.getName());

        String itemFinalName = itemName;
        if (playerMoney >= moneyAmount) {
            Material itemMaterial = Material.getMaterial(itemName);

            if (itemMaterial != null) {
                int requiredSlots = (int) Math.ceil((double) itemCount / itemMaterial.getMaxStackSize());

                int emptySlots = 0;
                for (int i = 0; i < 36; i++) {
                    ItemStack itemStack = player.getInventory().getItem(i);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        emptySlots++;
                    }
                }

                if (emptySlots >= requiredSlots) {
                    ItemStack buyItem = new ItemStack(itemMaterial, itemCount);
                    ItemMeta itemMeta = buyItem.getItemMeta();

                    ConfigurationSection itemSection = priceSection.getConfigurationSection("item");
                    if (itemSection != null) {
                        ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
                        if (metaSection != null) {
                            String itemDisplayName = metaSection.getString("display-name");
                            if (itemDisplayName != null) {
                                String displayName = ChatColor.translateAlternateColorCodes('&', itemDisplayName);
                                itemMeta.setDisplayName(displayName);
                                itemFinalName = displayName;
                            }
                            List<String> itemLore = metaSection.getStringList("lore");
                            if (!itemLore.isEmpty()) {
                                List<String> displayLore = new ArrayList<>();
                                for (String loreLine : itemLore) {
                                    displayLore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                                }
                                itemMeta.setLore(displayLore);
                            }
                            ConfigurationSection enchantSection = metaSection.getConfigurationSection("enchants");
                            if (enchantSection != null) {
                                for (String enchantName : enchantSection.getKeys(false)) {
                                    int enchantLevel = enchantSection.getInt(enchantName, 0);
                                    Enchantment enchantment = Enchantment.getByName(enchantName);
                                    if (enchantment != null) {
                                        itemMeta.addEnchant(enchantment, enchantLevel, true);
                                    } else {
                                        player.sendMessage("Invalid enchantment: " + enchantName);
                                    }
                                }
                            }
                            List<String> itemFlagsList = metaSection.getStringList("ItemFlags");
                            if (!itemFlagsList.isEmpty()) {
                                for (String flagName : itemFlagsList) {
                                    try {
                                        ItemFlag itemFlag = ItemFlag.valueOf(flagName);
                                        itemMeta.addItemFlags(itemFlag);
                                    } catch (IllegalArgumentException e) {
                                        player.sendMessage("Invalid item flag: " + flagName);
                                    }
                                }
                            }
                            buyItem.setItemMeta(itemMeta);buyItem.setItemMeta(itemMeta);

                        }
                        player.getInventory().addItem(buyItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2f);
                        moneyManager.setMoney(player.getName(), playerMoney - moneyAmount);
                        player.sendMessage(ChatColor.GRAY + "You bought " + ChatColor.YELLOW + itemCount + ChatColor.RESET + " " + itemFinalName + ChatColor.GRAY + " for " + ChatColor.GREEN + moneyAmount + " " + PublicVars.currencyName + ChatColor.GRAY + ".");

                        /*int itemsAdded = 0;
                        for (int i = 0; i < 36; i++) {
                            ItemStack slotItem = player.getInventory().getItem(i);
                            if (slotItem == null || slotItem.getType() == Material.AIR) {
                                int itemsToAdd = Math.min(itemCount - itemsAdded, itemMaterial.getMaxStackSize());
                                player.getInventory().setItem(i, new ItemStack(itemMaterial, itemsToAdd));
                                itemsAdded += itemsToAdd;

                                if (itemsAdded >= itemCount) {
                                    break;
                                }
                            }
                        }*/
                    } else {
                        player.sendMessage(ChatColor.GRAY + "Invalid item configuration.");
                    }
                } else {
                    player.sendMessage(ChatColor.GRAY + "You don't have enough space in your inventory to buy " + ChatColor.RESET + itemFinalName + ChatColor.GRAY + ".");
                }
            } else {
                player.sendMessage("Invalid item name: " + itemName);
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "You don't have enough " + PublicVars.currencyName + ChatColor.GRAY + " to buy " + ChatColor.RESET + itemFinalName + ChatColor.GRAY + ".");
        }
    }

    private void performSellAction(Player player, String itemName, int itemCount, int moneyAmount, String priceName) {
        long playerMoney = moneyManager.getMoney(player.getName());
        Material itemMaterial = Material.getMaterial(itemName);
        String itemFinalName = itemName;

        if (itemMaterial != null) {
            int totalItemCount = countItemInInventory(player.getInventory(), new ItemStack(itemMaterial));
            int totalMatchingItems = 0;

            ItemStack itemToCheck = new ItemStack(itemMaterial);
            ItemMeta itemToCheckMeta = itemToCheck.getItemMeta();

            File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
            FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
            ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);

            if (priceSection != null) {
                ConfigurationSection itemSection = priceSection.getConfigurationSection("item");
                if (itemSection != null) {
                    ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
                    if (metaSection != null) {
                        String itemDisplayName = metaSection.getString("display-name");
                        if (itemDisplayName != null) {
                            itemFinalName = itemDisplayName;
                            String displayName = ChatColor.translateAlternateColorCodes('&', itemDisplayName);
                            itemToCheckMeta.setDisplayName(displayName);
                        }

                        List<String> itemLore = metaSection.getStringList("lore");
                        if (!itemLore.isEmpty()) {
                            List<String> displayLore = new ArrayList<>();
                            for (String loreLine : itemLore) {
                                displayLore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                            }
                            itemToCheckMeta.setLore(displayLore);
                        }

                        ConfigurationSection enchantSection = metaSection.getConfigurationSection("enchants");
                        if (enchantSection != null) {
                            for (String enchantName : enchantSection.getKeys(false)) {
                                int enchantLevel = enchantSection.getInt(enchantName, 0);
                                Enchantment enchantment = Enchantment.getByName(enchantName);
                                if (enchantment != null) {
                                    itemToCheckMeta.addEnchant(enchantment, enchantLevel, true);
                                } else {
                                    player.sendMessage("Invalid enchantment: " + enchantName);
                                }
                            }
                        }

                        List<String> itemFlagsList = metaSection.getStringList("ItemFlags");
                        if (!itemFlagsList.isEmpty()) {
                            for (String flagName : itemFlagsList) {
                                try {
                                    ItemFlag itemFlag = ItemFlag.valueOf(flagName);
                                    itemToCheckMeta.addItemFlags(itemFlag);
                                } catch (IllegalArgumentException e) {
                                    player.sendMessage("Invalid item flag: " + flagName);
                                }
                            }
                        }

                        itemToCheck.setItemMeta(itemToCheckMeta);
                    }
                } else {
                    player.sendMessage("ERROR: No 'item' section found in the configuration for the item.");
                }
            } else {
                player.sendMessage("ERROR: No price configuration found for " + itemName);
            }

            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack != null && itemStack.getType() == itemMaterial) {
                    if ((itemStack.hasItemMeta() && itemStack.getItemMeta().equals(itemToCheckMeta)) ||
                            (!itemStack.hasItemMeta() && !itemToCheckMeta.hasDisplayName() && !itemToCheckMeta.hasLore())) {
                        totalMatchingItems += itemStack.getAmount();
                    }
                }
            }

            if (totalMatchingItems >= itemCount) {
                int remainingItems = itemCount;

                for (ItemStack itemStack : player.getInventory().getContents()) {
                    if (itemStack != null && itemStack.getType() == itemMaterial) {
                        if ((itemStack.hasItemMeta() && itemStack.getItemMeta().equals(itemToCheckMeta)) ||
                                (!itemStack.hasItemMeta() && !itemToCheckMeta.hasDisplayName() && !itemToCheckMeta.hasLore())) {

                            int stackAmount = itemStack.getAmount();
                            if (remainingItems >= stackAmount) {
                                remainingItems -= stackAmount;
                                player.getInventory().removeItem(itemStack);
                            } else {
                                itemStack.setAmount(stackAmount - remainingItems);
                                break;
                            }
                        }
                    }
                }

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2f);
                moneyManager.setMoney(player.getName(), playerMoney + moneyAmount);
                player.sendMessage(ChatColor.GRAY + "You sold " + ChatColor.YELLOW + itemCount + ChatColor.RESET + " " + itemFinalName + ChatColor.GRAY + " for " + ChatColor.GREEN + moneyAmount + " " + PublicVars.currencyName + ChatColor.GRAY + ".");
            } else {
                player.sendMessage(ChatColor.GRAY + "You don't have enough " + ChatColor.RESET + itemFinalName + ChatColor.GRAY + " in your inventory to sell.");
            }
        } else {
            player.sendMessage("Invalid item name: " + itemName);
        }
    }

    private int countItemInInventory(Inventory inventory, ItemStack itemToCount) {
        int itemCount = 0;

        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.isSimilar(itemToCount)) {
                itemCount += itemStack.getAmount();
            }
        }

        return itemCount;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        String title = event.getView().getTitle();

        if (title.startsWith(CustomGUIName)) {
            String id = title.substring(CustomGUIName.length());

            ConfigurationSection guiSection = config.getConfigurationSection(id);
            if (guiSection != null) {
                ConfigurationSection itemsSection = guiSection.createSection("items");

                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        ConfigurationSection itemSection = itemsSection.createSection(Integer.toString(slot));
                        itemSection.set("item", item);
                    }
                }

                guiSection.set("slots", inventory.getSize());
                saveConfig();
                player.sendMessage("GUI items saved for ID '" + id + "'.");
            }
        }
    }

    private void openItemEditorGUI(Player player, String id) {
        Inventory itemEditorInventory = Bukkit.createInventory(null, 27, ItemEditorName);

        ItemStack goBackButton = new ItemStack(Material.BIRCH_DOOR);
        ItemMeta buttonMeta = goBackButton.getItemMeta();
        buttonMeta.setDisplayName(ChatColor.RED + "Go Back: " + id);
        goBackButton.setItemMeta(buttonMeta);

        itemEditorInventory.setItem(26, goBackButton);

        player.openInventory(itemEditorInventory);
    }

    private void setItemNameAndLore(Player player, String[] args) {
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
            ItemStack item = player.getInventory().getItemInMainHand();
            ItemMeta meta = item.getItemMeta();

            if (args.length >= 2 && args[0].equalsIgnoreCase("name")) {
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                item.setItemMeta(meta);
                player.sendMessage("Item name updated!");
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("lore")) {
                String lore = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                List<String> loreLines = new ArrayList<>();
                for (String line : lore.split("\\\\n")) {
                    loreLines.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreLines);
                item.setItemMeta(meta);
                player.sendMessage("Item lore updated!");
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("price")) {
                String lore = "%priceVariable:<" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ">%";
                List<String> loreLines = new ArrayList<>();
                loreLines.add(ChatColor.translateAlternateColorCodes('&', lore));
                meta.setLore(loreLines);
                item.setItemMeta(meta);
                player.sendMessage("Item lore updated!");
            }  else {
                player.sendMessage(ChatColor.YELLOW + "Usage: /edititem name/lore/function/price/hideFlags/enchant <string>\n"+ ChatColor.GRAY +"Use: \\n for adding a new line.");
            }
        } else {
            player.sendMessage("Hold an item in your hand to set its name and lore.");
        }
    }

    private void setItemFunction(Player player, String[] args) {
        if (args.length >= 4) {
            String functionType = args[0].toLowerCase();
            String itemName = args[1];
            int itemCount;
            int moneyAmount;

            try {
                itemCount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please provide a valid number.");
                return;
            }
            try {
                moneyAmount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid " + PublicVars.currencyName + " amount. Please provide a valid number.");
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().equals(Material.AIR)) {
                player.sendMessage("Hold an item in your hand to set its function.");
                return;
            }

            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(PRICE_NAME_KEY, PersistentDataType.STRING, functionType);

            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            player.sendMessage("Item function updated!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Usage: /edititem function buy/sell <item> <item_amount> <money_amount>");
        }
    }

    public void openSavedGUI(Player player, String id) {
        String guiName = getGUIName(id);

        ConfigurationSection guiSection = config.getConfigurationSection(id);
        if (guiSection != null) {
            int slots = guiSection.getInt("slots", 9);
            ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");

            Inventory newGuiInventory;
            if (!guiName.isEmpty()) {
                newGuiInventory = Bukkit.createInventory(player, slots, SavedGUIName + guiName);
            } else {
                newGuiInventory = Bukkit.createInventory(player, slots, SavedGUIName);
            }

            if (itemsSection != null) {
                for (String slotString : itemsSection.getKeys(false)) {
                    int slot = Integer.parseInt(slotString);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotString);

                    if (itemSection != null) {
                        ItemStack item = itemSection.getItemStack("item");
                        replacePriceVariables(item);
                        newGuiInventory.setItem(slot, item);
                    }
                }
            }

            player.openInventory(newGuiInventory);
            resetConfigToSavedState();
        }
    }

    private void resetConfigToSavedState() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading guis.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private int getPriceValue(String priceName) {
        reloadPricesConfig();

        if (configPrices.contains("prices." + priceName)) {
            return configPrices.getInt("prices." + priceName + ".price");
        }
        return 0;
    }

    private int getPriceItemAmount(String priceName) {
        reloadPricesConfig();

        if (configPrices.contains("prices." + priceName)) {
            return configPrices.getInt("prices." + priceName + ".item" + ".amount");
        }
        return 0;
    }

    private void reloadPricesConfig() {
        try {
            FileConfiguration newConfigPrices = YamlConfiguration.loadConfiguration(configFilePrices);

            configPrices = new YamlConfiguration();
            configPrices.setDefaults(newConfigPrices);
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading prices.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replacePriceVariables(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore != null) {
            List<String> updatedLore = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("%priceVariable:")) {
                    int startIdx = line.indexOf("<");
                    int endIdx = line.indexOf(">");
                    if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                        String priceName = line.substring(startIdx + 1, endIdx);
                        int priceValue = getPriceValue(priceName);
                        line = line.replace("%priceVariable:<" + priceName + ">%", String.valueOf(priceValue));

                        String functionType = getFunctionType(priceName);

                        ItemStack itemFromConfig = getItemStackFromConfig(priceName);

                        if (itemFromConfig != null) {
                            int itemCount = itemFromConfig.getAmount();
                            int moneyAmount = getPriceValueFromConfig(priceName);

                            meta.getPersistentDataContainer().set(PRICE_NAME_KEY, PersistentDataType.STRING, priceName);

                            String finishedLine = ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + functionType + " » " + ChatColor.GRAY + itemCount + "x " + item.getType().name() + ChatColor.DARK_GRAY + " - " + ChatColor.GREEN + moneyAmount + " " + PublicVars.currencyName;
                            updatedLore.add(finishedLine);
                        }
                    }
                }
                /*updatedLore.add(line);*/
            }
            meta.setLore(updatedLore);
            item.setItemMeta(meta);
        }
    }

    private String getFunctionType(String priceName) {
        File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);
        if (priceSection != null) {
            return priceSection.getString("type", "buy");
        }

        return "buy";
    }

    private ItemStack getItemStackFromConfig(String priceName) {
        File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);
        if (priceSection != null) {
            ConfigurationSection itemSection = priceSection.getConfigurationSection("item");
            if (itemSection != null) {
                int itemId = itemSection.getInt("v", 1);
                Material itemType = Material.matchMaterial(itemSection.getString("type", "STONE"));
                int itemAmount = itemSection.getInt("amount", 1);

                ItemStack item = new ItemStack(itemType, itemAmount, (short) itemId);

                ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
                if (metaSection != null) {
                    ItemMeta itemMeta = item.getItemMeta();

                    for (String key : metaSection.getKeys(false)) {
                        String value = metaSection.getString(key);
                        itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
                    }

                    item.setItemMeta(itemMeta);
                }

                return item;
            }
        }

        return null;
    }



    private int getPriceValueFromConfig(String priceName) {
        File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        ConfigurationSection priceSection = pricesConfig.getConfigurationSection("prices." + priceName);
        if (priceSection != null) {
            return priceSection.getInt("price", 0);
        }

        return 0;
    }

    private void setGUIName(CommandSender sender, String id, String name) {
        if (!config.contains(id)) {
            sender.sendMessage("GUI with ID '" + id + "' does not exist.");
            return;
        }

        ConfigurationSection guiSection = config.getConfigurationSection(id);
        if (guiSection != null) {
            guiSection.set("name", name);
            saveConfig();
            sender.sendMessage("GUI name updated for ID '" + id + "'.");
        }
    }

    private void hideAttributesFromItem(Player player, String[] args) {
        if (!(player instanceof Player)) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack item = playerInventory.getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (args.length == 1) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.removeItemFlags(flag);
            }
            player.sendMessage("All item flags removed from the item's tooltip.");
        } else if (args.length == 2 && args[1].equalsIgnoreCase("ALL")) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            player.sendMessage("All item flags added to the item's tooltip.");
        } else {
            for (int i = 1; i < args.length; i++) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(args[i].toUpperCase());
                    meta.addItemFlags(flag);
                    player.sendMessage(flag + " flag added to the item's tooltip.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage("Invalid flag name: " + args[i]);
                }
            }
        }

        item.setItemMeta(meta);
        playerInventory.setItemInMainHand(item);
    }

    private void setEnchantment(Player player, String enchantmentName, String levelString) {
        Enchantment enchantment = getEnchantmentByName(enchantmentName);
        if (enchantment == null) {
            player.sendMessage("Invalid enchantment: " + enchantmentName);
            return;
        }

        int level;
        try {
            level = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid enchantment level: " + levelString);
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack item = playerInventory.getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("Hold an item in your hand to add the enchantment.");
            return;
        }

        item.addUnsafeEnchantment(enchantment, level);
        playerInventory.setItemInMainHand(item);
        player.sendMessage("Enchantment " + enchantment.getKey().getKey() + " set to level " + level + " for the item.");
    }

    private Enchantment getEnchantmentByName(String name) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.getKey().getKey().equalsIgnoreCase(name)) {
                return enchantment;
            }
        }
        return null;
    }

    private void removeAllEnchantments(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        ItemStack item = playerInventory.getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("Hold an item in your hand to remove its enchantments.");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        for (Enchantment enchantment : meta.getEnchants().keySet()) {
            meta.removeEnchant(enchantment);
        }

        item.setItemMeta(meta);
        playerInventory.setItemInMainHand(item);
        player.sendMessage("All enchantments have been removed from the item.");
    }

    private void addGlowEffect(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        ItemStack item = playerInventory.getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("Hold an item in your hand to add a glowing effect.");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        playerInventory.setItemInMainHand(item);
        player.sendMessage("The item is now glowing with a special effect.");
    }

    private List<String> getPriceNameCompletions() {
        List<String> completions = new ArrayList<>();

        File pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (pricesFile.exists()) {
            FileConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
            ConfigurationSection pricesSection = pricesConfig.getConfigurationSection("prices");
            if (pricesSection != null) {
                completions.addAll(pricesSection.getKeys(false));
            }
        }

        return completions;
    }

    private boolean sendTabCompletionsPrice(CommandSender sender, List<String> completions, String[] args) {
        if (completions.isEmpty()) return false;

        List<String> matchedCompletions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();

        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(currentArg)) {
                matchedCompletions.add(completion);
            }
        }

        sender.sendMessage(String.join(" ", matchedCompletions));
        return true;
    }

}