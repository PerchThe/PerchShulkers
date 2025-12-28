package me.perch.shulkers.listener;

import me.perch.shulkers.OpenShulker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    private boolean isPluginVirtualShulkerView(Player p) {
        if (p == null) return false;
        if (p.getOpenInventory() == null) return false;
        if (p.getOpenInventory().getTopInventory() == null) return false;
        return p.getOpenInventory().getTopInventory().getType() == InventoryType.SHULKER_BOX
                && p.getOpenInventory().getTopInventory().getHolder() == null;
    }

    private boolean isPluginVirtualShulkerClose(InventoryCloseEvent event) {
        if (event.getView() == null) return false;
        if (event.getView().getTopInventory() == null) return false;
        return event.getView().getTopInventory().getType() == InventoryType.SHULKER_BOX
                && event.getView().getTopInventory().getHolder() == null;
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("openshulker.use")) return;
        if (event.getClickedInventory() == null) return;

        int clickedSlot = event.getSlot();
        ItemStack clickedItemStack = event.getClickedInventory().getItem(clickedSlot);
        if (clickedItemStack == null) return;
        if (clickedItemStack.getType() == Material.AIR) return;
        if (!clickedItemStack.getType().name().contains(Material.SHULKER_BOX.name())) return;
        if (!event.isRightClick()) return;
        if (!event.isShiftClick()) return;

        if (event.getClickedInventory() == player.getInventory()) {
            if (!this._openShulker._allowInventoryOpen) return;

            if (isPluginVirtualShulkerView(player)) {
                if (this._openShulker.GetShulkerActions().HasOpenShulkerBox(player)) {
                    ItemStack shulkerBox = this._openShulker.GetShulkerActions().SearchShulkerBox(player);
                    this._openShulker.GetShulkerActions().SaveShulkerBox(shulkerBox, player.getOpenInventory().getTopInventory(), player);
                }
                player.closeInventory();
            }

            boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox(player, clickedItemStack);
            if (!open) return;
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().getType() == InventoryType.ENDER_CHEST) {
            if (!this._openShulker._allowEnderChestOpen) return;
            if (!this.IsOwnerOfEnderChest(player, clickedItemStack, clickedSlot)) return;
            boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox(player, clickedItemStack, true);
            if (!open) return;
            event.setCancelled(true);
            return;
        }

        if (!this._openShulker._allowContainerOpen) return;
        Location location = event.getClickedInventory().getLocation();
        if (location == null) return;
        boolean open = this._openShulker.GetShulkerActions().AttemptToOpenShulkerBox(player, clickedItemStack, location);
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
        if (!isPluginVirtualShulkerView(event.getPlayer())) return;
        if (!this._openShulker.GetShulkerActions().HasOpenShulkerBox(event.getPlayer())) return;
        ItemStack shulkerBox = event.getItemDrop().getItemStack();
        if (!this._openShulker.GetShulkerActions().IsOpenShulker(shulkerBox, event.getPlayer())) return;
        boolean enderChest = this._openShulker.GetShulkerActions().HasOpenShulkerInEnderChest(event.getPlayer());
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
        if (!(event.getPlayer() instanceof Player)) return;
        if (!isPluginVirtualShulkerClose(event)) return;
        Player player = (Player) event.getPlayer();
        if (!this._openShulker.GetShulkerActions().HasOpenShulkerBox(player)) return;
        ItemStack itemStack = this._openShulker.GetShulkerActions().SearchShulkerBox(player);
        if (itemStack == null) return;
        boolean enderChest = this._openShulker.GetShulkerActions().HasOpenShulkerInEnderChest(player);
        Container container = this._openShulker.GetShulkerActions().GetShulkerHoldingContainer(player);
        this._openShulker.GetShulkerActions().SaveShulkerBox(itemStack, event.getInventory(), player);
        this.ReopenInventory(enderChest, container, player);
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
        if (isPluginVirtualShulkerView(p)) {
            if (this._openShulker.GetShulkerActions().HasOpenShulkerBox(p)) {
                ItemStack shulkerBox = this._openShulker.GetShulkerActions().SearchShulkerBox(p);
                if (shulkerBox != null) {
                    this._openShulker.GetShulkerActions().SaveShulkerBox(shulkerBox, p.getOpenInventory().getTopInventory(), p);
                }
            }
        }
        this._openShulker.GetShulkerActions().batchUnmarkShulkers(p);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.remove(this._openShulkerKey);
        pdc.remove(this._openShulkerLocationKey);
    }
}
