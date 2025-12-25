package me.perch.shulkers.util;

import me.perch.shulkers.OpenShulker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ShulkerActions {
    private final NamespacedKey _openShulkerKey;
    private final NamespacedKey _openShulkerLocationKey;
    private final NamespacedKey _openShulkerTsKey;
    private final OpenShulker _openShulker;
    private final long ttlMs;

    public ShulkerActions(OpenShulker openShulker) {
        this._openShulker = openShulker;
        this._openShulkerKey = new NamespacedKey(this._openShulker, "openshulker");
        this._openShulkerLocationKey = new NamespacedKey(this._openShulker, "openshulkerlocation");
        this._openShulkerTsKey = new NamespacedKey(this._openShulker, "openshulker_ts");
        long s = this._openShulker.getConfig().getLong("OpenFlagTTLSeconds", 600L);
        if (s < 0) s = 0;
        this.ttlMs = s * 1000L;
    }

    public void SaveShulkerBox(ItemStack shulkerBoxStack, Inventory inventory, Player player) {
        BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBoxStack.getItemMeta();
        ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
        shulkerBox.getInventory().setContents(inventory.getContents());
        blockStateMeta.setBlockState(shulkerBox);
        PersistentDataContainer container = blockStateMeta.getPersistentDataContainer();
        container.remove(this._openShulkerKey);
        container.remove(this._openShulkerTsKey);
        shulkerBoxStack.setItemMeta(blockStateMeta);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, null);
        container = player.getPersistentDataContainer();
        container.remove(this._openShulkerKey);
        container.remove(this._openShulkerLocationKey);
        try {
            player.playSound(player, Sound.valueOf(this._openShulker.getConfig().getString("CloseSound")), 0.08F, 1.0F);
        } catch (Throwable ignored) {}
    }

    public boolean HasOpenShulkerBox(Player player) {
        ItemStack itemStack = this.SearchShulkerBox(player);
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (!container.has(this._openShulkerKey, PersistentDataType.STRING)) {
            if (itemStack != null) {
                Bukkit.getLogger().severe("Player " + player.getName() + " (" + player.getUniqueId() + ") may have duped!");
                Bukkit.getLogger().severe("Found opened Shulker while not having a shulker open!");
            }
            return false;
        }
        if (itemStack == null) {
            Bukkit.getLogger().severe("Player " + player.getName() + " (" + player.getUniqueId() + ") may have duped!");
            Bukkit.getLogger().severe("Currently viewing a shulker while not having an opened shulker!");
            return false;
        }
        return true;
    }

    public ItemStack SearchShulkerBox(Player player) {
        PersistentDataContainer dataContainer = player.getPersistentDataContainer();
        if (this.HasOpenShulkerInEnderChest(player)) return this.SearchShulkerBox(player.getEnderChest(), player);
        if (dataContainer.has(this._openShulkerLocationKey, PersistentDataType.STRING)) {
            Container container = this.GetShulkerHoldingContainer(player);
            return this.SearchShulkerBox(container.getInventory(), player);
        }
        return this.SearchShulkerBox(player.getInventory());
    }

    public boolean HasOpenShulkerInEnderChest(Player player) {
        PersistentDataContainer dataContainer = player.getPersistentDataContainer();
        return dataContainer.has(this._openShulkerLocationKey, PersistentDataType.BYTE);
    }

    public ItemStack SearchShulkerBox(Inventory inventory, Player player) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) continue;
            if (itemStack.getType() == Material.AIR) continue;
            if (!this.IsOpenShulker(itemStack, player)) continue;
            return itemStack;
        }
        return null;
    }

    public Container GetShulkerHoldingContainer(Player player) {
        PersistentDataContainer dataContainer = player.getPersistentDataContainer();
        if (dataContainer.has(this._openShulkerLocationKey, PersistentDataType.BYTE)) return null;
        if (!dataContainer.has(this._openShulkerLocationKey, PersistentDataType.STRING)) return null;
        String locationString = dataContainer.get(this._openShulkerLocationKey, PersistentDataType.STRING);
        String[] locationStringArray = locationString.split(";");
        double xCoordinate = Double.parseDouble(locationStringArray[0]);
        double yCoordinate = Double.parseDouble(locationStringArray[1]);
        double zCoordinate = Double.parseDouble(locationStringArray[2]);
        World world = Bukkit.getWorld(locationStringArray[3]);
        Location location = new Location(world, xCoordinate, yCoordinate, zCoordinate);
        Block block = location.getBlock();
        if (!(block.getState() instanceof Container)) return null;
        Container container = (Container) block.getState();
        return container;
    }

    public ItemStack SearchShulkerBox(Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) continue;
            if (itemStack.getType() == Material.AIR) continue;
            if (!this.IsOpenShulker(itemStack)) continue;
            return itemStack;
        }
        return null;
    }

    private boolean isStale(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(this._openShulkerTsKey, PersistentDataType.LONG)) return false;
        long ts = pdc.get(this._openShulkerTsKey, PersistentDataType.LONG);
        if (this.ttlMs <= 0) return false;
        return System.currentTimeMillis() - ts > this.ttlMs;
    }

    private void clearItemFlag(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(this._openShulkerKey);
        pdc.remove(this._openShulkerTsKey);
        itemStack.setItemMeta(meta);
    }

    public boolean IsOpenShulker(ItemStack itemStack, Player player) {
        ItemMeta meta = itemStack.getItemMeta();
        if (!(itemStack.getItemMeta() instanceof BlockStateMeta)) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(this._openShulkerKey, PersistentDataType.STRING)) return false;
        if (isStale(meta)) {
            clearItemFlag(itemStack);
            return false;
        }
        if (player == null) return true;
        String uniqueId = container.get(this._openShulkerKey, PersistentDataType.STRING);
        if (uniqueId == null) return false;
        if (!uniqueId.equalsIgnoreCase(player.getUniqueId().toString())) return false;
        return true;
    }

    public boolean IsOpenShulker(ItemStack itemStack) {
        return this.IsOpenShulker(itemStack, null);
    }

    public boolean AttemptToOpenShulkerBox(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return this.AttemptToOpenShulkerBox(player, itemStack);
    }

    public boolean AttemptToOpenShulkerBox(Player player, ItemStack itemStack) {
        if (!player.hasPermission("openshulker.use")) return false;
        if (itemStack.getAmount() <= 0) return false;
        if (itemStack.getAmount() > 1) return false;
        if (!itemStack.getType().name().contains(Material.SHULKER_BOX.name())) return false;
        if (!(itemStack.getItemMeta() instanceof BlockStateMeta)) return false;
        BlockStateMeta blockStateMeta = (BlockStateMeta) itemStack.getItemMeta();
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox)) return false;
        ShulkerBox shulker = (ShulkerBox) blockStateMeta.getBlockState();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(this._openShulkerKey, PersistentDataType.STRING)) return false;
        container.set(this._openShulkerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        container.set(this._openShulkerTsKey, PersistentDataType.LONG, System.currentTimeMillis());
        itemStack.setItemMeta(meta);
        container = player.getPersistentDataContainer();
        container.set(this._openShulkerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        Inventory inventory = Bukkit.createInventory(null, InventoryType.SHULKER_BOX);
        Bukkit.getScheduler().runTaskLater(this._openShulker, () -> {
            inventory.setContents(shulker.getInventory().getContents());
            player.openInventory(inventory);
        }, this._openShulker.getConfig().getLong("WaitSecondsBeforeOpen", 0) * 20);
        try {
            player.playSound(player, Sound.valueOf(this._openShulker.getConfig().getString("OpenSound")), 0.08F, 1.0F);
        } catch (Throwable ignored) {}
        return true;
    }

    public boolean AttemptToOpenShulkerBox(Player player, ItemStack itemStack, Location chest) {
        Block block = chest.getBlock();
        if (!(block.getState() instanceof Container)) return false;
        boolean open = this.AttemptToOpenShulkerBox(player, itemStack);
        if (!open) return false;
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(this._openShulkerLocationKey, PersistentDataType.STRING,
                chest.getX() + ";" + chest.getY() + ";" + chest.getZ() + ";" + chest.getWorld().getName());
        return true;
    }

    public boolean AttemptToOpenShulkerBox(Player player, ItemStack itemStack, boolean enderChest) {
        boolean open = this.AttemptToOpenShulkerBox(player, itemStack);
        if (!open) return false;
        if (!enderChest) return true;
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(this._openShulkerLocationKey, PersistentDataType.BYTE, (byte) 1);
        return true;
    }

    public void unmarkShulkerAsOpen(ItemStack shulker) {
        if (shulker == null) return;
        if (!shulker.getType().name().endsWith("SHULKER_BOX")) return;
        if (!shulker.hasItemMeta()) return;
        ItemMeta meta = shulker.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(this._openShulkerKey, PersistentDataType.STRING) || pdc.has(this._openShulkerTsKey, PersistentDataType.LONG)) {
            pdc.remove(this._openShulkerKey);
            pdc.remove(this._openShulkerTsKey);
            shulker.setItemMeta(meta);
        }
    }

    public void batchUnmarkShulkers(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) unmarkShulkerAsOpen(item);
        for (ItemStack item : inv.getArmorContents()) unmarkShulkerAsOpen(item);
        unmarkShulkerAsOpen(inv.getItemInOffHand());
        for (ItemStack item : player.getEnderChest().getContents()) unmarkShulkerAsOpen(item);
    }
}
