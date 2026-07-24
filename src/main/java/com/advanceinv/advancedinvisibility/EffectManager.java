package com.advanceinv.advancedinvisibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectManager {

    private final AdvancedInvisibilityPlugin plugin;
    private final Map<UUID, EffectTask> activeEffects = new ConcurrentHashMap<>();
    private final Set<UUID> stealthBrokenPlayers = new HashSet<>();
    private final Map<UUID, Set<UUID>> aggroedMobs = new ConcurrentHashMap<>(); // player UUID -> set of mob entity UUIDs

    public EffectManager(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasEffect(Player player) {
        return activeEffects.containsKey(player.getUniqueId());
    }

    public boolean isStealthBroken(Player player) {
        return stealthBrokenPlayers.contains(player.getUniqueId());
    }

    public void setStealthBroken(Player player) {
        stealthBrokenPlayers.add(player.getUniqueId());
    }

    public void trackAggroedMob(Player player, org.bukkit.entity.Mob mob) {
        aggroedMobs
            .computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
            .add(mob.getUniqueId());
    }

    public void restoreStealth(Player player) {
        stealthBrokenPlayers.remove(player.getUniqueId());
        
        // Clear aggro from EVERY mob that has EVER targeted this player
        // Using Bukkit.getEntity(UUID) — direct O(1) lookup, no world scan needed
        Set<UUID> mobsToUntrack = aggroedMobs.remove(player.getUniqueId());
        if (plugin.getConfigManager().isDisableMobDetection()) {
            if (mobsToUntrack != null) {
                for (UUID mobUuid : mobsToUntrack) {
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(mobUuid);
                    if (entity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) entity;
                        if (mob.getTarget() != null && mob.getTarget().equals(player)) {
                            mob.setTarget(null);
                        }
                    }
                }
            }
            // Also drop existing aggro from nearby mobs (catches mobs that aggroed before the effect/tracking started)
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(64, 64, 64)) {
                if (entity instanceof org.bukkit.entity.Mob) {
                    org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) entity;
                    if (mob.getTarget() != null && mob.getTarget().equals(player)) {
                        mob.setTarget(null);
                    }
                }
            }
        }
    }

    public int[] getTicks(Player player) {
        EffectTask task = activeEffects.get(player.getUniqueId());
        if (task != null) {
            return new int[]{task.getRemainingTicks(), task.getOriginalTicks()};
        }
        return null;
    }

    public void applyEffect(Player player, int durationTicks) {
        // Drop any existing aggro immediately upon buying the effect
        restoreStealth(player);

        int originalTicks = durationTicks;
        int remainingTicks = durationTicks;
        applyEffect(player, remainingTicks, originalTicks);
    }

    public void applyEffect(Player player, int remainingTicks, int originalTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, remainingTicks, 0, false, false, false));
        
        // Fresh start: always restore stealth when buying/resuming
        restoreStealth(player);

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer addPacket = null;
        try {
            addPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            addPacket.getPlayerInfoActions().write(0, EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED
            ));

            WrappedGameProfile profile = WrappedGameProfile.fromPlayer(player);
            PlayerInfoData data = new PlayerInfoData(
                    player.getUniqueId(),
                    player.getPing(),
                    true,
                    EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                    profile,
                    WrappedChatComponent.fromText(player.getPlayerListName()),
                    (WrappedRemoteChatSessionData) null
            );

            // Use write(1) as lists in 1.19.3+ are on index 1 for PLAYER_INFO
            addPacket.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.hidePlayer(plugin, player);

                if (addPacket != null) {
                    try {
                        protocolManager.sendServerPacket(onlinePlayer, addPacket);
                    } catch (Exception ignored) {}
                }
            }
        }

        EffectTask task = new EffectTask(player, remainingTicks, originalTicks);
        task.runTaskTimer(plugin, 20L, 20L); 
        activeEffects.put(player.getUniqueId(), task);
    }

    public void removeEffect(Player player, boolean isDisconnect) {
        EffectTask task = activeEffects.remove(player.getUniqueId());
        if (task != null) {
            task.cleanup();
            task.cancel();
        }
        
        restoreStealth(player);

        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.sendActionBar("");
            if (!isDisconnect) {
                // Send expiry title so the player always knows even with display-type NONE
                String expiredTitle = plugin.getConfigManager().getWarningExpiredTitle();
                player.sendTitle(expiredTitle, "",
                        plugin.getConfigManager().getWarningFadeIn(),
                        plugin.getConfigManager().getWarningStay(),
                        plugin.getConfigManager().getWarningFadeOut());
            }
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.showPlayer(plugin, player);
            }
        }
    }

    public void cleanupAll() {
        for (UUID uuid : activeEffects.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeEffect(player, false);
            }
        }
        activeEffects.clear();
        stealthBrokenPlayers.clear();
    }

    class EffectTask extends BukkitRunnable {
        private final Player player;
        private final int remainingTicks;
        private final int originalTicks;
        private int timePassed = 0;
        private BossBar bossBar = null;
        private final Set<Integer> firedThresholds = new HashSet<>();

        public EffectTask(Player player, int remainingTicks, int originalTicks) {
            this.player = player;
            this.remainingTicks = remainingTicks;
            this.originalTicks = originalTicks;
        }

        public int getRemainingTicks() {
            return remainingTicks - timePassed;
        }
        
        public int getOriginalTicks() {
            return originalTicks;
        }

        public void cleanup() {
            if (bossBar != null) {
                bossBar.removePlayer(player);
                bossBar = null;
            }
        }

        @Override
        public void run() {
            timePassed += 20; 
            if (timePassed >= remainingTicks || !player.isOnline()) {
                removeEffect(player, false);
                return;
            }

            int currentRemaining = remainingTicks - timePassed;
            String displayType = plugin.getConfigManager().getDisplayType();

            // Handle dynamic creation/destruction of BossBar based on config
            if ("BOSS_BAR".equals(displayType)) {
                BarColor color;
                try { color = BarColor.valueOf(plugin.getConfigManager().getBossBarColor()); }
                catch (IllegalArgumentException e) { color = BarColor.WHITE; }
                
                BarStyle style;
                try { style = BarStyle.valueOf(plugin.getConfigManager().getBossBarStyle()); }
                catch (IllegalArgumentException e) { style = BarStyle.SOLID; }

                if (bossBar == null) {
                    bossBar = Bukkit.createBossBar("", color, style);
                    bossBar.addPlayer(player);
                } else {
                    bossBar.setColor(color);
                    bossBar.setStyle(style);
                }
                
                double progress = (double) Math.max(0, currentRemaining) / originalTicks;
                bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));

                int secondsLeft = currentRemaining / 20;
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft % 60;
                String timeStr = String.format("%02d:%02d", minutes, seconds);
                String title = plugin.getConfigManager().getBossBarText().replace("{time}", timeStr);
                bossBar.setTitle(title);
            } else {
                // If it was BOSS_BAR but changed to something else, remove it
                if (bossBar != null) {
                    bossBar.removePlayer(player);
                    bossBar = null;
                }
                
                if ("ACTION_BAR".equals(displayType)) {
                    int secondsLeft = currentRemaining / 20;
                    int minutes = secondsLeft / 60;
                    int seconds = secondsLeft % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);
                    String actionText = plugin.getConfigManager().getActionBarText().replace("{time}", timeStr);
                    player.sendActionBar(actionText);
                }
            }
            // --- Milestone title warnings (ONLY for NONE display type) ---
            if ("NONE".equals(displayType)) {
                int secondsRemaining = currentRemaining / 20;
                for (int threshold : plugin.getConfigManager().getWarningThresholds()) {
                    if (secondsRemaining == threshold && !firedThresholds.contains(threshold)) {
                        firedThresholds.add(threshold);
                        String warningTitle;
                        if (threshold <= 10) {
                            warningTitle = "§c§lInvisibility ending!";
                        } else if (threshold <= 30) {
                            warningTitle = "§6§lInvisibility fading...";
                        } else {
                            warningTitle = "§e§lInvisibility fading...";
                        }
                        String sub = plugin.getConfigManager().getWarningSubtitle()
                                .replace("{time}", String.valueOf(secondsRemaining));
                        player.sendTitle(warningTitle, sub,
                                plugin.getConfigManager().getWarningFadeIn(),
                                plugin.getConfigManager().getWarningStay(),
                                plugin.getConfigManager().getWarningFadeOut());
                    }
                }
            }
        }
    }
}
