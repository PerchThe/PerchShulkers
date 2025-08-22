package me.perch.shulkers.listener;

import me.perch.shulkers.OpenShulker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class ShulkerReadOnlyListener implements Listener {
    private final OpenShulker _openShulker;

    public ShulkerReadOnlyListener(OpenShulker openShulker) {
        this._openShulker = openShulker;
    }

    @EventHandler
    public void OnShulkerInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.SHULKER_BOX) return;

        if (!this._openShulker.GetShulkerActions().HasOpenShulkerBox((Player) event.getWhoClicked())) return;

        if (event.getWhoClicked().hasPermission("openshulker.write")) return;

        event.setCancelled(true);
    }
}
