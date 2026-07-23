package com.advanceinv.advancedinvisibility;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final AdvancedInvisibilityPlugin plugin;
    private double pricePerMinute;
    private int defaultTime;
    private String displayType;
    private boolean disableMobDetection;
    private String msgAlreadyActive;
    private String msgNotEnoughMoney;
    private String msgActivated;
    private String msgAdminGave;
    private String msgAdminRemoved;
    private String msgRemoved;
    private String msgNoPermission;

    public ConfigManager(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.pricePerMinute = config.getDouble("advanced-invisibility.price-per-minute", 500.0);
        this.defaultTime = config.getInt("advanced-invisibility.default-time", 3);
        this.displayType = config.getString("advanced-invisibility.display-type", "BOSS_BAR").toUpperCase();
        this.disableMobDetection = config.getBoolean("advanced-invisibility.disable-mob-detection", true);

        this.msgAlreadyActive = config.getString("messages.already-active", "§cYou already have the Advanced Invisibility effect active.");
        this.msgNotEnoughMoney = config.getString("messages.not-enough-money", "§cYou don't have enough money. This effect costs $§e{price}§c.");
        this.msgActivated = config.getString("messages.activated", "§aAdvanced Invisibility activated for §e{time} minute(s)§a for $§e{price}§a.");
        this.msgAdminGave = config.getString("messages.admin-gave", "§aGave §e{player}§a advance invisibility for §e{time} minute(s)§a.");
        this.msgAdminRemoved = config.getString("messages.admin-removed", "§aRemoved advance invisibility from §e{player}§a.");
        this.msgRemoved = config.getString("messages.removed", "§eYour invisibility effect has been removed.");
        this.msgNoPermission = config.getString("messages.no-permission", "§cYou don't have permission to use this command.");
    }

    public double getPricePerMinute() { return pricePerMinute; }
    public int getDefaultTime() { return defaultTime; }
    public String getDisplayType() { return displayType; }
    public boolean isDisableMobDetection() { return disableMobDetection; }
    public String getMsgAlreadyActive() { return msgAlreadyActive; }
    public String getMsgNotEnoughMoney() { return msgNotEnoughMoney; }
    public String getMsgActivated() { return msgActivated; }
    public String getMsgAdminGave() { return msgAdminGave; }
    public String getMsgAdminRemoved() { return msgAdminRemoved; }
    public String getMsgRemoved() { return msgRemoved; }
    public String getMsgNoPermission() { return msgNoPermission; }
}
