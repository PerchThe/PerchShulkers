package me.perch.shulkers.listener;

import me.perch.shulkers.OpenShulker;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShulkerDupeListener implements Listener {
    private final OpenShulker openShulker;

    public ShulkerDupeListener(OpenShulker openShulker) {
        this.openShulker = openShulker;
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && item.getType().name().endsWith("SHULKER_BOX");
    }

    private boolean cancelIfPluginShulkerOpen(Cancellable event, Player player) {
        if (player.getOpenInventory().getType() != InventoryType.SHULKER_BOX) return false;
        if (!openShulker.GetShulkerActions().HasOpenShulkerBox(player)) return false;
        event.setCancelled(true);
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelIfPluginShulkerOpen(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (cancelIfPluginShulkerOpen(event, event.getPlayer())) return;
        handleContainerBreak(event, event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        cancelIfPluginShulkerOpen(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        cancelIfPluginShulkerOpen(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (cancelIfPluginShulkerOpen(event, (Player) event.getWhoClicked()))
            event.setResult(Event.Result.DENY);
    }

    private void handleContainerBreak(BlockEvent blockEvent, CommandSender sender, Cancellable cancellable) {
        if (!(blockEvent.getBlock().getState() instanceof Container)) return;
        Container container = (Container) blockEvent.getBlock().getState();
        ItemStack openShulkerItem = openShulker.GetShulkerActions().SearchShulkerBox(container.getInventory());
        if (openShulkerItem == null) return;
        cancellable.setCancelled(true);
        if (sender instanceof Player) {
            String prefix = ChatColor.translateAlternateColorCodes('&', openShulker.getConfig().getString("Messages.Prefix", "&c[OpenShulker] "));
            String msg = ChatColor.translateAlternateColorCodes('&', openShulker.getConfig().getString("Messages.CannotBreakContainer", "&cCannot break: this container holds an active shulker box."));
            sender.sendMessage(prefix + msg);
        }
    }

    private void removeContainersWithOpenShulkers(List<Block> blockList) {
        blockList.removeIf(block -> {
            if (block.getState() instanceof Container) {
                Container container = (Container) block.getState();
                return openShulker.GetShulkerActions().SearchShulkerBox(container.getInventory()) != null;
            }
            return false;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeContainersWithOpenShulkers(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeContainersWithOpenShulkers(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDupedShulkerPlace(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!isShulkerBox(item)) return;
        if (!openShulker.GetShulkerActions().IsOpenShulker(item, event.getPlayer())) return;
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isShulkerBox(clicked) && openShulker.GetShulkerActions().IsOpenShulker(clicked, player)) {
            if (player.getOpenInventory().getType() != InventoryType.SHULKER_BOX) {
                openShulker.GetShulkerActions().unmarkShulkerAsOpen(clicked);
            } else if (!player.isOp()) {
                event.setCancelled(true);
                return;
            }
        }
        if (isShulkerBox(cursor) && openShulker.GetShulkerActions().IsOpenShulker(cursor, player)) {
            if (player.getOpenInventory().getType() != InventoryType.SHULKER_BOX) {
                openShulker.GetShulkerActions().unmarkShulkerAsOpen(cursor);
            } else if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShulkerInventoryClose(InventoryCloseEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMove(InventoryMoveItemEvent event) {
        if (isShulkerBox(event.getItem()) && openShulker.GetShulkerActions().IsOpenShulker(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (isShulkerBox(event.getItem()) && openShulker.GetShulkerActions().IsOpenShulker(event.getItem())) {
            event.setCancelled(true);
        }
    }
}