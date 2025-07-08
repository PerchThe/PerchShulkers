package me.entity303.openshulker.commands;

import me.entity303.openshulker.OpenShulker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenShulkerCommand implements TabExecutor {
    private final OpenShulker _openShulker;

    public OpenShulkerCommand(OpenShulker openShulker) {
        this._openShulker = openShulker;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!commandSender.hasPermission("openshulker.admin")) return true;

        String prefix = ChatColor.translateAlternateColorCodes('&', this._openShulker.getConfig().getString("Messages.Prefix"));

        if (args.length == 0) {
            commandSender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this._openShulker.getConfig()
                                                                                                            .getString(
                                                                                                                    "Messages.OpenShulkerCommand.Syntax")
                                                                                                            .replace("<LABEL>", label)));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            this._openShulker.InitializeConfig();
            commandSender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this._openShulker.getConfig()
                                                                                                            .getString(
                                                                                                                    "Messages.OpenShulkerCommand.Reloaded")
                                                                                                            .replace("<LABEL>", label)));
            return true;
        }

        commandSender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this._openShulker.getConfig()
                                                                                                        .getString("Messages.OpenShulkerCommand.Syntax")
                                                                                                        .replace("<LABEL>", label)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s,
                                                @NotNull String[] args) {
        if (args.length == 1) return Collections.singletonList("reload");

        return new ArrayList<>();
    }
}
