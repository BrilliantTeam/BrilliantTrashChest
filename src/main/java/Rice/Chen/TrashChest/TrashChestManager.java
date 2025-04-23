package Rice.Chen.TrashChest;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class TrashChestManager {
    private static final String CHEST_NAME = "§c銷毀儲物箱";
    private static final String METADATA_KEY = "trash_chest";
    
    private final Plugin plugin;
    private final MessageManager messageManager;
    private final ChestDataManager dataManager;
    private final SchedulerHelper schedulerHelper;

    public TrashChestManager(Plugin plugin, MessageManager messageManager, ChestDataManager dataManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.dataManager = dataManager;
        this.schedulerHelper = new SchedulerHelper(plugin);
    }

    public boolean setupTrashChest(Block block, Player player) {
        if (block == null || player == null) return false;
        if (!(block.getState() instanceof Chest chest)) return false;

        schedulerHelper.runBlockTask(block, () -> {
            chest.setCustomName(CHEST_NAME);
            chest.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            chest.update(true);

            dataManager.addTrashChest(block);
            messageManager.sendSuccessMessage(player);
        });

        return true;
    }

    public void removeTrashChest(Block block, Player player) {
        if (block == null) return;
        if (!(block.getState() instanceof Chest chest)) return;
        if (!isTrashChest(chest)) return;

        dataManager.removeTrashChest(block);

        schedulerHelper.runDelayedBlockTask(block, () -> {
            if (block.getState() instanceof Chest remainingChest) {
                remainingChest.removeMetadata(METADATA_KEY, plugin);
                remainingChest.setCustomName(null);
                remainingChest.update(true);
            }
            
            if (player != null) {
                messageManager.sendRemoveMessage(player);
            }
        }, 1L);
    }

    public boolean isTrashChest(Chest chest) {
        return chest != null && chest.hasMetadata(METADATA_KEY);
    }

    public void clearChestWithAnimation(Block block) {
        if (!(block.getState() instanceof Chest chest)) return;
        if (!isTrashChest(chest)) return;

        Inventory inv = chest.getInventory();
        
        chest.open();
        
        schedulerHelper.runDelayedBlockTask(block, () -> {
            chest.close();
            inv.clear();
        }, 10L);
    }

    public void clearChestImmediately(Block block) {
        if (!(block.getState() instanceof Chest chest)) return;
        if (!isTrashChest(chest)) return;

        schedulerHelper.runBlockTask(block, () -> {
            chest.getInventory().clear();
        });
    }

    public void restoreTrashChest(Block block) {
        if (block == null) return;
        if (!(block.getState() instanceof Chest chest)) return;

        schedulerHelper.runBlockTask(block, () -> {
            chest.setCustomName(CHEST_NAME);
            chest.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            chest.update(true);
        });
    }
}