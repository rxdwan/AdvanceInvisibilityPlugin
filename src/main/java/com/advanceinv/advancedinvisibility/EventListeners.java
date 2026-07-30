package com.advanceinv.advancedinvisibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.SoundGroup;
import org.bukkit.event.entity.PlayerDeathEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.entity.Mob;

import java.util.Collections;

public class EventListeners implements Listener {

    private final AdvancedInvisibilityPlugin plugin;

    public EventListeners(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        
        // Resume paused effect if it exists
        int[] pausedData = plugin.getDataStorage().getTime(joiner.getUniqueId());
        if (pausedData != null && pausedData[0] > 0) {
            plugin.getEffectManager().applyEffect(joiner, pausedData[0], pausedData[1]);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getEffectManager().hasEffect(online)) {
                joiner.hidePlayer(plugin, online);
                // Re-send PLAYER_INFO so the invisible player stays in joiner's tab list
                plugin.getEffectManager().sendTabListPacketTo(online, joiner);
            }
        }
        
        // If an invisible player is currently in reveal window, show them to the joiner
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getEffectManager().hasEffect(online) && plugin.getEffectManager().isRevealed(online.getUniqueId())) {
                joiner.showPlayer(plugin, online);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getEffectManager().hasActiveOrPausedEffect(player)) {
            int[] ticks = plugin.getEffectManager().getTicks(player);
            if (ticks != null && ticks[0] > 0) {
                plugin.getDataStorage().saveTime(player.getUniqueId(), ticks[0], ticks[1]);
            }
            plugin.getEffectManager().removeEffect(player, true);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
            if (plugin.getEffectManager().hasEffect(player) && !plugin.getEffectManager().isPaused(player)) {
                plugin.getEffectManager().pauseInvisibility(player);
                player.sendMessage(plugin.getConfigManager().getMsgPaused());
            }
        } else {
            if (plugin.getEffectManager().isPaused(player)) {
                plugin.getEffectManager().resumeInvisibility(player);
                player.sendMessage(plugin.getConfigManager().getMsgResumed());
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.MILK_BUCKET) {
            if (plugin.getEffectManager().hasEffect(event.getPlayer())) {
                plugin.getEffectManager().removeEffect(event.getPlayer(), false);
            }
        } else if (event.getItem().getType() == Material.POTION) {
            if (event.getItem().getItemMeta() instanceof PotionMeta) {
                PotionMeta meta = (PotionMeta) event.getItem().getItemMeta();
                if (meta.getBasePotionType() == PotionType.AWKWARD) {
                    if (plugin.getEffectManager().hasEffect(event.getPlayer())) {
                        plugin.getEffectManager().restoreStealth(event.getPlayer());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (plugin.getConfigManager().isDisableMobDetection() && plugin.getEffectManager().hasEffect(player)) {
                if (!plugin.getEffectManager().isStealthBroken(player)) {
                    // Stealth is intact — cancel the mob targeting the player
                    event.setCancelled(true);
                } else {
                    // Stealth is broken — record this mob so we can clear its aggro later
                    if (event.getEntity() instanceof Mob) {
                        plugin.getEffectManager().trackAggroedMob(player, (Mob) event.getEntity());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (plugin.getEffectManager().hasEffect(player)) {
                // "Stealth broken" (mobs can now target the player) applies when attacking ANY entity/mob
                plugin.getEffectManager().setStealthBroken(player);
                
                // The Reveal Window (visible to other players) only activates when attacking another PLAYER
                if (event.getEntity() instanceof Player) {
                    if (plugin.getConfigManager().isRevealWindowEnabled()) {
                        plugin.getEffectManager().enterRevealWindow(player);
                        // Small subtitle-only text — doesn't clash with action bar timer or milestone titles
                        int dur = plugin.getConfigManager().getRevealWindowDuration();
                        player.sendTitle("", "§c· Stealth disrupted for §f" + dur + "s", 2, 50, 10);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (plugin.getEffectManager().hasActiveOrPausedEffect(victim)) {
            plugin.getEffectManager().removeEffect(victim, false);
        }

        if (killer != null && plugin.getEffectManager().hasEffect(killer)) {
            String victimName = victim.getDisplayName() != null ? victim.getDisplayName() : victim.getName();
            event.setDeathMessage(victimName + " died");

            // Null out last damage cause to trick Grave plugins (tested strategy against GravesX/DeadChest)
            victim.setLastDamageCause(new EntityDamageEvent(victim, EntityDamageEvent.DamageCause.CUSTOM, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (plugin.getEffectManager().hasEffect(event.getPlayer())) {
            event.renderer((source, sourceDisplayName, message, viewer) -> 
                net.kyori.adventure.text.Component.text("<Player> ").append(message)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.getEffectManager().hasEffect(player)) {
                // By default, the client doesn't play the totem sound if the player is hidden via hidePlayer().
                // So if suppressTotemUse is false, we manually play the sound to all players.
                if (!plugin.getConfigManager().isSuppressTotemUse()) {
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getEffectManager().hasEffect(player)) {
            // Like the totem, Paper sometimes natively silences block placement sounds for hidden players.
            // If the user wants to HEAR placing sounds (suppressPlacingSounds = false), we manually play them.
            if (!plugin.getConfigManager().isSuppressBlockPlace()) {
                SoundGroup soundGroup = event.getBlockPlaced().getBlockData().getSoundGroup();
                player.getWorld().playSound(event.getBlock().getLocation(), soundGroup.getPlaceSound(), soundGroup.getVolume(), soundGroup.getPitch());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getEffectManager().hasEffect(player)) {
            // If suppressBlockBreak is false, manually broadcast the break sound so other players can hear it.
            if (!plugin.getConfigManager().isSuppressBlockBreak()) {
                SoundGroup soundGroup = event.getBlock().getBlockData().getSoundGroup();
                player.getWorld().playSound(event.getBlock().getLocation(), soundGroup.getBreakSound(), soundGroup.getVolume(), soundGroup.getPitch());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (plugin.getEffectManager().hasEffect(player)) {
            if (!plugin.getConfigManager().isSuppressBlockPlace()) {
                org.bukkit.Sound bucketSound = event.getBucket() == org.bukkit.Material.LAVA_BUCKET ? 
                        org.bukkit.Sound.ITEM_BUCKET_EMPTY_LAVA : org.bukkit.Sound.ITEM_BUCKET_EMPTY;
                player.getWorld().playSound(event.getBlockClicked().getLocation(), bucketSound, 1.0f, 1.0f);
            }
        }
    }
}
