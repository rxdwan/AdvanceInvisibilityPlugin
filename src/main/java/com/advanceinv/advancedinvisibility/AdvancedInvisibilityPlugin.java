package com.advanceinv.advancedinvisibility;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedInvisibilityPlugin extends JavaPlugin {

    private Economy econ = null;
    private ConfigManager configManager;
    private EffectManager effectManager;
    private PacketListeners packetListeners;
    private DataStorage dataStorage;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        dataStorage = new DataStorage(this);
        effectManager = new EffectManager(this);
        packetListeners = new PacketListeners(this);

        getCommand("advanceinv").setExecutor(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);

        packetListeners.register();
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.cleanupAll();
        }
        if (packetListeners != null) {
            packetListeners.unregister();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
