package dupepatcher;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.inventory.HopperInventory;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import com.google.common.collect.BiMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Main extends PluginBase implements Listener {

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static final int maxNameLength = 30;

    private List<String> etOpen = new ArrayList<>();

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
        Item i = e.getItem();
        if (i.hasCustomName()) {
            if (i.getCustomName().length() > maxNameLength) {
                i.clearCustomName();
                e.getPlayer().getInventory().setItemInHand(i);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        e.getInventory().getContents().forEach((v, i) -> {
            if (i.hasCustomName()) {
                if (i.getCustomName().length() > maxNameLength) {
                    i.clearCustomName();
                    e.getInventory().setItem(v, i);
                }
            }
            if (i.getId() == Item.UNDYED_SHULKER_BOX || i.getId() == Item.SHULKER_BOX) {
                if (checkNestedShulker(i)) {
                    e.getInventory().setItem(v, removeNestedShulker(i));
                }
            }
        });

        if (e.getInventory().getType() == InventoryType.ENCHANT_TABLE) {
            etOpen.add(e.getPlayer().getName());
        }
    }

    @EventHandler
    public void onItemPickup(InventoryPickupItemEvent e) {
        if (e.getItem().getItem().getId() == Item.UNDYED_SHULKER_BOX || e.getItem().getItem().getId() == Item.SHULKER_BOX) {
            if (e.getInventory().getHolder() instanceof Player) {
                if (etOpen.contains(((Player) e.getInventory().getHolder()).getName())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        etOpen.remove(e.getPlayer().getName());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() == InventoryType.ENCHANT_TABLE) {
            etOpen.remove(e.getPlayer().getName());
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

    private boolean checkNestedShulker(Item i) {
        //TODO
        return false;
    }

    private Item removeNestedShulker(Item i) {
        //TODO
        return i;
    }
}
