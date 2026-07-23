package com.advanceinv.advancedinvisibility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataStorage {
    private final AdvancedInvisibilityPlugin plugin;
    private File file;
    private FileConfiguration config;

    public DataStorage(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveTime(UUID uuid, int remainingTicks, int originalTicks) {
        String path = "paused." + uuid.toString();
        config.set(path + ".remaining", remainingTicks);
        config.set(path + ".original", originalTicks);
        save();
    }

    public int[] getTime(UUID uuid) {
        String path = "paused." + uuid.toString();
        if (config.contains(path)) {
            int remaining = config.getInt(path + ".remaining");
            int original = config.getInt(path + ".original");
            config.set(path, null);
            save();
            return new int[]{remaining, original};
        }
        return null;
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
