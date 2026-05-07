package dev.lampyt.worldcorruptor;

import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorldCorruptorPlugin extends JavaPlugin implements TabExecutor {
    private CorruptionSettings settings;
    private CorruptionEngine engine;
    private BukkitTask task;
    private long totalMutations;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        getCommand("worldcorruptor").setExecutor(this);
        getCommand("worldcorruptor").setTabCompleter(this);
        if (settings.enabled()) {
            startCorruption();
        }
        getLogger().info("WorldCorruptor enabled for Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        stopCorruption();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                startCorruption();
                sender.sendMessage(prefix() + ChatColor.GREEN + "corruption started.");
            }
            case "stop" -> {
                stopCorruption();
                sender.sendMessage(prefix() + ChatColor.YELLOW + "corruption stopped.");
            }
            case "reload" -> {
                reloadConfig();
                boolean wasRunning = task != null;
                stopCorruption();
                reloadSettings();
                if (wasRunning || settings.enabled()) {
                    startCorruption();
                }
                sender.sendMessage(prefix() + ChatColor.GREEN + "config reloaded.");
            }
            case "status" -> sendStatus(sender);
            case "pulse" -> pulse(sender, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "reload", "status", "pulse").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private void reloadSettings() {
        settings = CorruptionSettings.fromConfig(getConfig());
        engine = new CorruptionEngine(settings);
    }

    private void startCorruption() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int changed = engine.tick(Bukkit.getOnlinePlayers().stream().toList());
            totalMutations += changed;
        }, settings.intervalTicks(), settings.intervalTicks());
    }

    private void stopCorruption() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void pulse(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + ChatColor.RED + "only players can pulse corruption at their location.");
            return;
        }

        int radius = parsePositiveInt(args, 1, settings.radius());
        int blocks = parsePositiveInt(args, 2, settings.blocksPerCycle());
        World world = player.getWorld();
        if (!settings.allowsWorld(world.getName())) {
            sender.sendMessage(prefix() + ChatColor.RED + "this world is not enabled in config.yml.");
            return;
        }

        int changed = engine.pulse(world, player.getLocation(), radius, blocks);
        totalMutations += changed;
        sender.sendMessage(prefix() + ChatColor.DARK_PURPLE + "pulse mutated " + changed + " blocks.");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(prefix() + ChatColor.WHITE + "commands:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " start" + ChatColor.DARK_GRAY + " - begin scheduled corruption");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " stop" + ChatColor.DARK_GRAY + " - pause scheduled corruption");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload" + ChatColor.DARK_GRAY + " - reload config.yml");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " status" + ChatColor.DARK_GRAY + " - show current settings");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " pulse [radius] [blocks]" + ChatColor.DARK_GRAY + " - corrupt near you once");
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(prefix() + ChatColor.WHITE + "running: " + ChatColor.AQUA + (task != null));
        sender.sendMessage(prefix() + ChatColor.WHITE + "interval: " + ChatColor.AQUA + settings.intervalTicks() + " ticks");
        sender.sendMessage(prefix() + ChatColor.WHITE + "amount: " + ChatColor.AQUA + settings.blocksPerCycle() + " blocks/cycle");
        sender.sendMessage(prefix() + ChatColor.WHITE + "radius: " + ChatColor.AQUA + settings.radius());
        sender.sendMessage(prefix() + ChatColor.WHITE + "total mutations: " + ChatColor.AQUA + totalMutations);
    }

    private int parsePositiveInt(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String prefix() {
        return ChatColor.DARK_PURPLE + "[WorldCorruptor] " + ChatColor.RESET;
    }
}
