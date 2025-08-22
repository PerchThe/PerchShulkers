package me.perch.shulkers.listener;

import me.perch.shulkers.OpenShulker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ShulkerOpenCloseListener implements Listener {
    private final OpenShulker _openShulker;
    private final NamespacedKey _clickedShulkerKey;
    private final NamespacedKey _openShulkerKey;
    private final NamespacedKey _openShulkerLocationKey;
    private final NamespacedKey _openShulkerTsKey;

    public ShulkerOpenCloseListener(OpenShulker openShulker) {
        this._openShulker = openShulker;
        this._clickedShulkerKey = new NamespacedKey(this._openShulker, "clickedshulker");
        this._openShulkerKey = new NamespacedKey(this._openShulker, "openshulker");
        this._openShulkerLocationKey = new NamespacedKey(this._openShulker, "openshulkerlocation");
        this._openShulkerTsKey = new NamespacedKey(this._openShulker, "openshulker_ts");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnShulkerOpen(PlayerInteractEvent event) {
        if (!this._openShulker._allowHandOpen) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.HOPPER) return;
        event.setCancelled(this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void OnShulkerOpenAlternative(InventoryClickEvent event) {
        if (!event.getWhoClicked().hasPermission("openshulker.use")) return;
        if (event.getClickedInventory() == null) return;
        int clickedSlot = event.getSlot();
        ItemStack clickedItemStack = event.getClickedInventory().getItem(clickedSlot);
        if (clickedItemStack == null) return;
        if (clickedItemStack.getType() == Material.AIR) return;
        if (!clickedItemStack.getType().name().contains(Material.SHULKER_BOX.name())) return;
        if (!event.isRightClick()) return;
        if (!event.isShiftClick()) return;
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            if (!this._openShulker._allowInventoryOpen) return;
            if (event.getView().getTopInventory().getType() == InventoryType.SHULKER_BOX) {
                if (this._openShulker.GetShulkerActions().HasOpenShulkerBox((Player) event.getWhoClicked())) {
                    ItemStack shulkerBox = this._openShulker.GetShulkerActions().SearchShulkerBox((Player) event.getWhoClicked());
                    this._openShulker.GetShulkerActions().SaveShulkerBox(shulkerBox, event.getView().getTopInventory(), (Player) event.getWhoClicked());
                }
                event.getWhoClicked().closeInventory();
            }
            boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox((Player) event.getWhoClicked(), clickedItemStack);
            if (!open) return;
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory().getType() == InventoryType.ENDER_CHEST) {
            if (!this._openShulker._allowEnderChestOpen) return;
            if (!this.IsOwnerOfEnderChest((Player) event.getWhoClicked(), clickedItemStack, clickedSlot)) return;
            boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox((Player) event.getWhoClicked(), clickedItemStack, true);
            if (!open) return;
            event.setCancelled(true);
            return;
        }
        if (!this._openShulker._allowContainerOpen) return;
        Location location = event.getClickedInventory().getLocation();
        if (location == null) return;
        boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox((Player) event.getWhoClicked(), clickedItemStack, location);
        if (!open) return;
        event.setCancelled(true);
    }

    private boolean IsOwnerOfEnderChest(Player player, ItemStack clickedItemStack, int clickedSlot) {
        ItemMeta clickedItemMeta = clickedItemStack.getItemMeta();
        PersistentDataContainer clickedItemContainer = clickedItemMeta.getPersistentDataContainer();
        clickedItemContainer.set(this._clickedShulkerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        clickedItemStack.setItemMeta(clickedItemMeta);
        ItemStack potentiallyClickedItem = player.getEnderChest().getItem(clickedSlot);
        if (potentiallyClickedItem == null) {
            clickedItemContainer.remove(this._clickedShulkerKey);
            clickedItemStack.setItemMeta(clickedItemMeta);
            return false;
        }
        ItemMeta potentiallyClickedItemMeta = potentiallyClickedItem.getItemMeta();
        PersistentDataContainer potentiallyClickedItemContainer = potentiallyClickedItemMeta.getPersistentDataContainer();
        boolean isOwner = false;
        if (potentiallyClickedItemContainer.has(this._clickedShulkerKey, PersistentDataType.STRING)) {
            String uuid = potentiallyClickedItemContainer.get(this._clickedShulkerKey, PersistentDataType.STRING);
            isOwner = uuid.equalsIgnoreCase(player.getUniqueId().toString());
        }
        clickedItemContainer.remove(this._clickedShulkerKey);
        clickedItemStack.setItemMeta(clickedItemMeta);
        return isOwner;
    }

    @EventHandler(ignoreCancelled = true)
    public void OnShulkerItemDrop(PlayerDropItemEvent event) {
        if (!this._openShulker.GetShulkerActions().HasOpenShulkerBox(event.getPlayer())) return;
        ItemStack shulkerBox = event.getItemDrop().getItemStack();
        if (!this._openShulker.GetShulkerActions().IsOpenShulker(shulkerBox, event.getPlayer())) return;
        boolean enderChest = this._openShulker.GetShulkerActions().HasOpenShulkerInEnderChest((Player) event.getPlayer());
        Container container = this._openShulker.GetShulkerActions().GetShulkerHoldingContainer(event.getPlayer());
        this._openShulker.GetShulkerActions().SaveShulkerBox(shulkerBox, event.getPlayer().getOpenInventory().getTopInventory(), event.getPlayer());
        this.ReopenInventory(enderChest, container, event.getPlayer());
    }

    private void ReopenInventory(boolean enderChest, Container container, HumanEntity player) {
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(this._openShulker, () -> {
            if (container != null) {
                if (container.getWorld() != player.getWorld()) return;
                if (container.getLocation().distance(player.getLocation()) > 4) return;
                player.openInventory(container.getInventory());
                return;
            }
            if (!enderChest) return;
            player.openInventory(player.getEnderChest());
        }, 1L);
    }

    @EventHandler
    public void OnShulkerInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.SHULKER_BOX) return;
        if (!this._openShulker.GetShulkerActions().HasOpenShulkerBox((Player) event.getPlayer())) return;
        ItemStack itemStack = this._openShulker.GetShulkerActions().SearchShulkerBox((Player) event.getPlayer());
        if (itemStack == null) return;
        boolean enderChest = this._openShulker.GetShulkerActions().HasOpenShulkerInEnderChest((Player) event.getPlayer());
        Container container = this._openShulker.GetShulkerActions().GetShulkerHoldingContainer((Player) event.getPlayer());
        this._openShulker.GetShulkerActions().SaveShulkerBox(itemStack, event.getInventory(), (Player) event.getPlayer());
        this.ReopenInventory(enderChest, container, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack out = event.getResult();
        if (out == null) return;
        if (!out.getType().name().endsWith("SHULKER_BOX")) return;
        ItemMeta m = out.getItemMeta();
        if (m == null) return;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        boolean changed = false;
        if (pdc.has(this._openShulkerKey, PersistentDataType.STRING)) {
            pdc.remove(this._openShulkerKey);
            changed = true;
        }
        if (pdc.has(this._openShulkerTsKey, PersistentDataType.LONG)) {
            pdc.remove(this._openShulkerTsKey);
            changed = true;
        }
        if (changed) {
            out.setItemMeta(m);
            event.setResult(out);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        this._openShulker.GetShulkerActions().batchUnmarkShulkers(p);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.remove(this._openShulkerKey);
        pdc.remove(this._openShulkerLocationKey);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        this._openShulker.GetShulkerActions().batchUnmarkShulkers(p);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.remove(this._openShulkerKey);
        pdc.remove(this._openShulkerLocationKey);
    }
}
