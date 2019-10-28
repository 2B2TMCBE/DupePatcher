package dupepatcher;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.inventory.HopperInventory;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends PluginBase implements Listener {

    private static final int MAX_NAME_LENGTH = 30;

    private Set<Player> etOpen = new HashSet<>();

    private AtomicInteger exCount = new AtomicInteger();
    private AtomicInteger caCount = new AtomicInteger();
    private AtomicInteger suCount = new AtomicInteger();

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleRepeatingTask(this, this::clearCounts, 100);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == PlayerInteractEvent.Action.PHYSICAL) return;
        if (e.getBlock().getId() == Block.ENCHANT_TABLE) {
            boolean shulker = false;
            for (Item i : e.getPlayer().getInventory().getContents().values()) {
                if (i.getId() == Item.SHULKER_BOX || i.getId() == Item.UNDYED_SHULKER_BOX) {
                    shulker = true;
                }
            }
            if (shulker) {
                e.getPlayer().sendMessage(TextFormat.RED + "Enchantment Table: Please remove all shulker boxes from your inventory before use");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        this.checkItem(e.getPlayer().getInventory(), e.getSlot(), e.getItem());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        e.getInventory().getContents().forEach((slot, item) -> this.checkItem(e.getInventory(), slot, item));

        if (e.getInventory().getType() == InventoryType.ENCHANT_TABLE) {
            etOpen.add(e.getPlayer());
        }
    }

    private void checkItem(Inventory inventory, int slot, Item i) {
        boolean changed = false;
        if (i.hasCustomName() && i.getCustomName().length() > MAX_NAME_LENGTH) {
            i.clearCustomName();
            changed = true;
        }
        if (i.hasEnchantments() && checkAndRemove32k(i)) {
            changed = true;
        }
        if (i.getCount() > i.getMaxStackSize()) {
            i.setCount(i.getMaxStackSize());
            changed = true;
        }
        if (changed) {
            inventory.setItem(slot, i);
        }
    }

    @EventHandler
    public void onItemPickup(InventoryPickupItemEvent e) {
        if (e.getItem().getItem().getId() == Item.UNDYED_SHULKER_BOX || e.getItem().getItem().getId() == Item.SHULKER_BOX) {
            if (e.getInventory().getHolder() instanceof Player) {
                if (etOpen.contains((Player) e.getInventory().getHolder())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Inventory inventory = event.getPlayer().getInventory();
        inventory.getContents().forEach((slot, item) -> this.checkItem(inventory, slot, item));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        etOpen.remove(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() == InventoryType.ENCHANT_TABLE) {
            etOpen.remove(e.getPlayer());
        }
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent e) {
        if (e.getInventory() instanceof HopperInventory) {
            if (e.getItem().getId() == Item.UNDYED_SHULKER_BOX || e.getItem().getId() == Item.SHULKER_BOX) {
                if (e.getAction() == InventoryMoveItemEvent.Action.PICKUP || e.getAction() == InventoryMoveItemEvent.Action.SLOT_CHANGE) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onSpawnEntity(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EntityPrimedTNT) {
            if (exCount.get() > 30) {
                e.getEntity().close();
            }
            exCount.incrementAndGet();
        } else if (e.getEntity() instanceof EntityItem) {
            if (Item.CACTUS == ((EntityItem) e.getEntity()).getItem().getId()) {
                if (caCount.get() > 50) {
                    e.getEntity().close();
                }
                caCount.incrementAndGet();
            } else if (Item.SUGAR_CANE == ((EntityItem) e.getEntity()).getItem().getId()) {
                if (suCount.get() > 50) {
                    e.getEntity().close();
                }
                suCount.incrementAndGet();
            }
        }
    }

    /*private boolean checkNestedShulker(Item i) {
        //TODO
        return false;
    }

    private Item removeNestedShulker(Item i) {
        //TODO
        return i;
    }*/

    private void clearCounts() {
        exCount.set(0);
        caCount.set(0);
        suCount.set(0);
    }

    private boolean checkAndRemove32k(Item item) {
        boolean changed = false;
        Enchantment[] enchantments = item.getEnchantments();
        for (Enchantment e : enchantments) {
            if (e.getLevel() > e.getMaxLevel()) {
                e.setLevel(e.getMaxLevel());
                changed = true;
            }
        }
        if (changed) {
            item.addEnchantment(enchantments);
        }
        return changed;
    }
}
