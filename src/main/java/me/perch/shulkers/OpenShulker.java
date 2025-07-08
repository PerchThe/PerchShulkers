package me.entity303.openshulker;

import me.entity303.openshulker.commands.CleanShulkersCommand;
import me.entity303.openshulker.commands.OpenShulkerCommand;
import me.entity303.openshulker.listener.ShulkerDupeListener;
import me.entity303.openshulker.listener.ShulkerOpenCloseListener;
import me.entity303.openshulker.listener.ShulkerReadOnlyListener;
import me.entity303.openshulker.util.ShulkerActions;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class OpenShulker extends JavaPlugin implements Listener {
    public boolean _allowInventoryOpen = true;
    public boolean _allowContainerOpen = true;
    public boolean _allowEnderChestOpen = true;
    public boolean _allowHandOpen = true;
    private ShulkerActions _shulkerActions;

    @Override
    public void onDisable() {
        for (Player all : Bukkit.getOnlinePlayers()) {
            if (!this._shulkerActions.HasOpenShulkerBox(all)) continue;

            ItemStack shulkerBox = this._shulkerActions.SearchShulkerBox(all);

            if (shulkerBox == null) continue;

            this._shulkerActions.SaveShulkerBox(shulkerBox, all.getOpenInventory().getTopInventory(), all);

            all.closeInventory();
        }
    }

    @Override
    public void onEnable() {
        this.InitializeConfig();

        this._shulkerActions = new ShulkerActions(this);

        Bukkit.getPluginManager().registerEvents(new ShulkerOpenCloseListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShulkerDupeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShulkerReadOnlyListener(this), this);

        // Register /openshulker command
        OpenShulkerCommand openShulkerCommand = new OpenShulkerCommand(this);
        PluginCommand command = this.getCommand("openshulker");
        command.setExecutor(openShulkerCommand);
        command.setTabCompleter(openShulkerCommand);

        // Register /cleanshulkers command
        this.getCommand("cleanshulkers").setExecutor(new CleanShulkersCommand(this));
    }

    public void InitializeConfig() {
        this.saveDefaultConfig();

        this.reloadConfig();

        String openSound = this.getConfig().getString("OpenSound");

        try {
            Sound.valueOf(openSound);
        } catch (Throwable ignored) {
            if (openSound == null) {
                Bukkit.getLogger().severe("You did not specify OpenSound, using default");
                this.getConfig().set("OpenSound", "BLOCK_SHULKER_BOX_OPEN");
                this.saveConfig();
                this.reloadConfig();
            } else Bukkit.getLogger()
                    .severe("There is no sound called '" + openSound +
                            "', for a list of sounds, visit https://www.spigotmc.org/wiki/cc-sounds-list/");
        }

        String closeSound = this.getConfig().getString("CloseSound");

        try {
            Sound.valueOf(closeSound);
        } catch (Throwable ignored) {
            if (closeSound == null) {
                Bukkit.getLogger().severe("You did not specify CloseSound, using default");
                this.getConfig().set("OpenSound", "BLOCK_SHULKER_BOX_CLOSE");
                this.saveConfig();
                this.reloadConfig();
            } else Bukkit.getLogger()
                    .severe("There is no sound called '" + closeSound +
                            "', for a list of sounds, visit https://www.spigotmc.org/wiki/cc-sounds-list/");
        }

        this._allowInventoryOpen = this.getConfig().getBoolean("OpenMethods.AllowInventoryOpen");
        this._allowContainerOpen = this.getConfig().getBoolean("OpenMethods.AllowContainerOpen");
        this._allowEnderChestOpen = this.getConfig().getBoolean("OpenMethods.AllowEnderChestOpen");
        this._allowHandOpen = this.getConfig().getBoolean("OpenMethods.AllowHandOpen");
    }

    public ShulkerActions GetShulkerActions() {
        return this._shulkerActions;
    }
}