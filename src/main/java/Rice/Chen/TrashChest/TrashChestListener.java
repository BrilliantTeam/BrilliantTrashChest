package Rice.Chen.TrashChest;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class TrashChestListener implements Listener {
    private final Plugin plugin;
    private final TrashChestManager manager;
    private final SignValidator signValidator;
    private final MessageManager messageManager;
    private final SchedulerHelper schedulerHelper;
    
    public TrashChestListener(Plugin plugin, TrashChestManager manager, 
                            SignValidator signValidator, MessageManager messageManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.signValidator = signValidator;
        this.messageManager = messageManager;
        this.schedulerHelper = new SchedulerHelper(plugin);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        Block chestBlock = signValidator.findAdjacentChest(signBlock);
        if (chestBlock == null) return;

        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = event.getLine(i);
        }

        boolean hadTrashTag = false;
        if (chestBlock.getState() instanceof Chest chest && manager.isTrashChest(chest)) {
            hadTrashTag = true;
        }

        boolean hasNewTrashTag = signValidator.hasTrashTag(lines);

        if (!hasNewTrashTag && hadTrashTag) {
            manager.removeTrashChest(chestBlock, event.getPlayer());
        } else if (hasNewTrashTag && !hadTrashTag) {
            manager.setupTrashChest(chestBlock, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (!(destination.getHolder() instanceof Chest chest)) return;
        if (!manager.isTrashChest(chest)) return;

        // 檢查來源是否為漏斗
        if (event.getSource().getType() == InventoryType.HOPPER) {
            Block chestBlock = chest.getBlock();
            // 使用 SchedulerHelper 來安排延遲任務
            schedulerHelper.runDelayedBlockTask(chestBlock, () -> {
                if (!destination.isEmpty()) {
                    manager.clearChestWithAnimation(chestBlock);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        if (!manager.isTrashChest(chest)) return;

        // 確保物品欄是空的當玩家開啟時
        if (!event.getInventory().isEmpty()) {
            manager.clearChestImmediately(chest.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() != InventoryType.CHEST) return;
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        if (!manager.isTrashChest(chest)) return;

        // 只有當物品欄不為空時才執行清除
        if (!event.getInventory().isEmpty()) {
            manager.clearChestImmediately(chest.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        if (!manager.isTrashChest(chest)) return;

        // 如果是往箱子內放入物品，立即清除
        if (event.getView().getTopInventory().equals(event.getClickedInventory())) {
            Block chestBlock = chest.getBlock();
            schedulerHelper.runDelayedBlockTask(chestBlock, () -> {
                if (!event.getInventory().isEmpty()) {
                    manager.clearChestImmediately(chestBlock);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;
        if (!manager.isTrashChest(chest)) return;

        // 檢查是否有物品被拖曳到箱子內
        boolean draggedToChest = event.getRawSlots().stream()
            .anyMatch(slot -> slot < event.getView().getTopInventory().getSize());

        if (draggedToChest) {
            Block chestBlock = chest.getBlock();
            schedulerHelper.runDelayedBlockTask(chestBlock, () -> {
                if (!event.getInventory().isEmpty()) {
                    manager.clearChestImmediately(chestBlock);
                }
            }, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() == Material.TRAPPED_CHEST) {
            if (block.getState() instanceof Chest chest && manager.isTrashChest(chest)) {
                chest.setCustomName(null);
                chest.update(true);
                manager.removeTrashChest(block, event.getPlayer());
            }
            return;
        }
        
        if (!block.getType().name().contains("SIGN")) return;
        
        Block chestBlock = signValidator.findAdjacentChest(block);
        if (chestBlock == null) return;
        
        if (chestBlock.getState() instanceof Chest chest && manager.isTrashChest(chest)) {
            manager.removeTrashChest(chestBlock, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() == Material.TRAPPED_CHEST) {
                if (block.getState() instanceof Chest chest && manager.isTrashChest(chest)) {
                    chest.setCustomName(null);
                    chest.update(true);
                    manager.removeTrashChest(block, null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() == Material.TRAPPED_CHEST) {
                if (block.getState() instanceof Chest chest && manager.isTrashChest(chest)) {
                    chest.setCustomName(null);
                    chest.update(true);
                    manager.removeTrashChest(block, null);
                }
            }
        }
    }
}