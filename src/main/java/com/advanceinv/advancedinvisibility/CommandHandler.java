package com.advanceinv.advancedinvisibility;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final AdvancedInvisibilityPlugin plugin;

    public CommandHandler(AdvancedInvisibilityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("advancedinvisibility.admin") || sender.isOp()) {
                plugin.getConfigManager().loadConfig();
                sender.sendMessage("§aAdvanced Invisibility config reloaded.");
                return true;
            }
        }

        // Parse args
        if (args.length == 0 || (args.length == 1 && isInteger(args[0]))) {
            // Player usage: /advanceinv OR /advanceinv <minutes>
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("advancedinvisibility.use")) {
                player.sendMessage(plugin.getConfigManager().getMsgNoPermission());
                return true;
            }
            // Restrict to overworld for normal players
            if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL
                    && !player.hasPermission("advancedinvisibility.admin") && !player.isOp()) {
                player.sendMessage("§cYou can only use this in the overworld.");
                return true;
            }
            if (plugin.getEffectManager().hasActiveOrPausedEffect(player)) {
                player.sendMessage(plugin.getConfigManager().getMsgAlreadyActive());
                return true;
            }

            int minutes = plugin.getConfigManager().getDefaultTime();
            if (args.length == 1) {
                minutes = Integer.parseInt(args[0]);
            }

            double price = plugin.getConfigManager().getPricePerMinute() * minutes;
            if (plugin.getEconomy() != null && price > 0) {
                if (!plugin.getEconomy().has(player, price)) {
                    String msg = plugin.getConfigManager().getMsgNotEnoughMoney().replace("{price}", String.valueOf(price));
                    player.sendMessage(msg);
                    return true;
                }
                EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, price);
                if (!response.transactionSuccess()) {
                    player.sendMessage("§cAn error occurred with the economy transaction.");
                    return true;
                }
            }

            plugin.getEffectManager().applyEffect(player, minutes * 60 * 20);

            String activatedMsg = plugin.getConfigManager().getMsgActivated()
                    .replace("{time}", String.valueOf(minutes))
                    .replace("{duration}", String.valueOf(minutes))
                    .replace("{price}", String.valueOf(price));
            player.sendMessage(activatedMsg);
            return true;
        } else if (sender.hasPermission("advancedinvisibility.admin") || sender.isOp()) {
            // Admin usage: /advanceinv <player> OR /advanceinv <player> <minutes> OR /advanceinv <player> remove
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            if (args.length == 1) {
                // Give for default time
                int minutes = plugin.getConfigManager().getDefaultTime();
                plugin.getEffectManager().applyEffect(target, minutes * 60 * 20);
                sender.sendMessage(plugin.getConfigManager().getMsgAdminGave()
                        .replace("{player}", target.getName())
                        .replace("{time}", String.valueOf(minutes))
                        .replace("{duration}", String.valueOf(minutes)));
                return true;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                if (plugin.getEffectManager().hasActiveOrPausedEffect(target)) {
                    plugin.getEffectManager().removeEffect(target, false);
                    sender.sendMessage(plugin.getConfigManager().getMsgAdminRemoved().replace("{player}", target.getName()));
                } else {
                    sender.sendMessage("§c" + target.getName() + " does not have the effect active.");
                }
                return true;
            } else if (isInteger(args[1])) {
                int minutes = Integer.parseInt(args[1]);
                plugin.getEffectManager().applyEffect(target, minutes * 60 * 20);
                sender.sendMessage(plugin.getConfigManager().getMsgAdminGave()
                        .replace("{player}", target.getName())
                        .replace("{time}", String.valueOf(minutes))
                        .replace("{duration}", String.valueOf(minutes)));
                return true;
            }
        }
        
        // Show usage based on permissions
        if (sender.hasPermission("advancedinvisibility.admin") || sender.isOp()) {
            sender.sendMessage("§cUsage: /advanceinv [minutes] OR /advanceinv <player> [minutes|remove]");
        } else if (sender.hasPermission("advancedinvisibility.use")) {
            sender.sendMessage("§cUsage: /advanceinv [minutes]");
        } else {
            sender.sendMessage(plugin.getConfigManager().getMsgNoPermission());
        }

        return true;
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("advancedinvisibility.use") && !sender.hasPermission("advancedinvisibility.admin") && !sender.isOp()) {
            return completions;
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("advancedinvisibility.use")) {
                options.addAll(Arrays.asList("1", "3", "5", "10"));
            }
            if (sender.hasPermission("advancedinvisibility.admin") || sender.isOp()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    options.add(p.getName());
                }
                options.add("reload");
            }
            StringUtil.copyPartialMatches(args[0], options, completions);
        } else if (args.length == 2 && (sender.hasPermission("advancedinvisibility.admin") || sender.isOp())) {
            // /advanceinv <player> [minutes|remove]
            if (Bukkit.getPlayer(args[0]) != null) {
                List<String> options = new ArrayList<>(Arrays.asList("1", "3", "5", "10", "remove"));
                StringUtil.copyPartialMatches(args[1], options, completions);
            }
        }
        return completions;
    }
}
