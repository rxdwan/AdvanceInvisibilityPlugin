package com.advanceinv.advancedinvisibility;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigManager {
    private final AdvancedInvisibilityPlugin plugin;
    private double pricePerMinute;
    private int defaultTime;
    private String displayType;
    private boolean disableMobDetection;
    private boolean revealWindowEnabled;
    private int revealWindowDuration;
    
    private boolean suppressHurt;
    private boolean suppressArmorEquip;
    private boolean suppressConsumeSounds;
    private boolean suppressBlockPlace;
    private boolean suppressBlockBreak;
    private boolean suppressBlockInteract;
    private boolean suppressTotemUse;

    private String msgAlreadyActive;
    private String msgNotEnoughMoney;
    private String msgActivated;
    private String msgAdminGave;
    private String msgAdminRemoved;
    private String msgRemoved;
    private String msgNoPermission;
    private String msgPaused;
    private String msgResumed;
    private List<Integer> warningThresholds;
    private boolean showExpiredTitle;
    private String warningExpiredTitle;
    private String warningSubtitle;
    private int warningFadeIn;
    private int warningStay;
    private int warningFadeOut;
    private String bossBarText;
    private String bossBarColor;
    private String bossBarStyle;
    private String actionBarText;

    public ConfigManager(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Automatically add any new missing keys from the jar's config.yml into the user's config.yml
        config.options().copyDefaults(true);
        plugin.saveConfig();

        this.pricePerMinute = config.getDouble("advanced-invisibility.price-per-minute", 500.0);
        this.defaultTime = config.getInt("advanced-invisibility.default-time", 3);
        this.displayType = config.getString("advanced-invisibility.display-type", "BOSS_BAR").toUpperCase();
        this.disableMobDetection = config.getBoolean("advanced-invisibility.disable-mob-detection", true);
        this.revealWindowEnabled = config.getBoolean("advanced-invisibility.reveal-window.enabled", true);
        this.revealWindowDuration = config.getInt("advanced-invisibility.reveal-window.duration", 3);
        
        this.suppressHurt = config.getBoolean("advanced-invisibility.suppress-sounds.hurt", true);
        this.suppressArmorEquip = config.getBoolean("advanced-invisibility.suppress-sounds.armor-equip", true);
        this.suppressConsumeSounds = config.getBoolean("advanced-invisibility.suppress-sounds.consume-sounds", true);
        this.suppressBlockPlace = config.getBoolean("advanced-invisibility.suppress-sounds.block-place", true);
        this.suppressBlockBreak = config.getBoolean("advanced-invisibility.suppress-sounds.block-break", true);
        this.suppressBlockInteract = config.getBoolean("advanced-invisibility.suppress-sounds.block-interact", false);
        this.suppressTotemUse = config.getBoolean("advanced-invisibility.suppress-sounds.totem-use", true);

        this.bossBarText = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("display.boss-bar-text", "&f&lAdvanced Invisibility &7- {time}"));
        this.bossBarColor = config.getString("display.boss-bar-color", "WHITE").toUpperCase();
        this.bossBarStyle = config.getString("display.boss-bar-style", "SOLID").toUpperCase();
        this.actionBarText = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("display.action-bar-text", "&f&l✨ Advanced Invisibility - {time} ✨"));

        this.msgAlreadyActive = config.getString("messages.already-active", "§cYou already have the Advanced Invisibility effect active.");
        this.msgNotEnoughMoney = config.getString("messages.not-enough-money", "§cYou don't have enough money. This effect costs $§e{price}§c.");
        this.msgActivated = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.activated", "&aAdvanced Invisibility activated for &e{time} minute(s)&a for $&e{price}&a."));
        this.msgAdminGave = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.admin-gave", "&aGave &e{player}&a advance invisibility for &e{time} minute(s)&a."));
        this.msgAdminRemoved = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.admin-removed", "&aRemoved advance invisibility from &e{player}&a."));
        this.msgRemoved = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.removed", "&eYour invisibility effect has been removed."));
        this.msgNoPermission = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.no-permission", "&cYou don't have permission to use this command."));
        this.msgPaused = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.paused", "&eYour invisibility effect has been paused because you left the overworld."));
        this.msgResumed = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("messages.resumed", "&aYour invisibility effect has resumed!"));

        this.warningThresholds = config.getIntegerList("warnings.thresholds");
        if (this.warningThresholds.isEmpty()) this.warningThresholds = List.of(60, 30, 10);
        this.showExpiredTitle = config.getBoolean("warnings.show-expired-title", false);
        this.warningExpiredTitle = config.getString("warnings.expired-title", "§4Invisibility Lost");
        this.warningSubtitle = config.getString("warnings.subtitle", "§7{time}s remaining");
        this.warningFadeIn = config.getInt("warnings.fade-in", 5);
        this.warningStay = config.getInt("warnings.stay", 60);
        this.warningFadeOut = config.getInt("warnings.fade-out", 10);
    }

    public double getPricePerMinute() { return pricePerMinute; }
    public int getDefaultTime() { return defaultTime; }
    public String getDisplayType() { return displayType; }
    public boolean isDisableMobDetection() { return disableMobDetection; }
    public boolean isRevealWindowEnabled() { return revealWindowEnabled; }
    public int getRevealWindowDuration() { return revealWindowDuration; }
    
    public boolean isSuppressHurt() { return suppressHurt; }
    public boolean isSuppressArmorEquip() { return suppressArmorEquip; }
    public boolean isSuppressConsumeSounds() { return suppressConsumeSounds; }
    public boolean isSuppressBlockPlace() { return suppressBlockPlace; }
    public boolean isSuppressBlockBreak() { return suppressBlockBreak; }
    public boolean isSuppressBlockInteract() { return suppressBlockInteract; }
    public boolean isSuppressTotemUse() { return suppressTotemUse; }

    public String getMsgAlreadyActive() { return msgAlreadyActive; }
    public String getMsgNotEnoughMoney() { return msgNotEnoughMoney; }
    public String getMsgActivated() { return msgActivated; }
    public String getMsgAdminGave() { return msgAdminGave; }
    public String getMsgAdminRemoved() { return msgAdminRemoved; }
    public String getMsgRemoved() { return msgRemoved; }
    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPaused() { return msgPaused; }
    public String getMsgResumed() { return msgResumed; }
    public List<Integer> getWarningThresholds() { return warningThresholds; }
    public boolean isShowExpiredTitle() { return showExpiredTitle; }
    public String getWarningExpiredTitle() { return warningExpiredTitle; }
    public String getWarningSubtitle() { return warningSubtitle; }
    public int getWarningFadeIn() { return warningFadeIn; }
    public int getWarningStay() { return warningStay; }
    public int getWarningFadeOut() { return warningFadeOut; }
    public String getBossBarText() { return bossBarText; }
    public String getBossBarColor() { return bossBarColor; }
    public String getBossBarStyle() { return bossBarStyle; }
    public String getActionBarText() { return actionBarText; }
}
