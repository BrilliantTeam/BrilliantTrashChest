package Rice.Chen.TrashChest;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.SoundCategory;

import java.util.concurrent.atomic.AtomicReference;

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
        World world = block.getWorld();
        Location loc = block.getLocation();

        // 音效消除任務引用
        AtomicReference<BukkitTask> soundCancellerTask = new AtomicReference<>();
        
        // 音效消除邏輯
        Runnable soundCanceller = () -> {
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(loc) <= 16) {
                    cancelChestSounds(player);
                }
            }
        };

        // 開始音效消除任務
        if (schedulerHelper.isFolia()) {
            // 修改：使用 1L 作為初始延遲，然後每 tick 執行一次
            plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, loc, 
                task -> soundCanceller.run(), 1L, 1L);
        } else {
            soundCancellerTask.set(plugin.getServer().getScheduler().runTaskTimer(plugin, 
                soundCanceller, 0L, 1L));
        }

        // 先執行一次音效消除
        soundCanceller.run();

        // 執行開箱動畫
        chest.open();
        
        // 延遲關閉和清除
        schedulerHelper.runDelayedBlockTask(block, () -> {
            chest.close();
            inv.clear();
            
            // 停止音效消除任務
            if (soundCancellerTask.get() != null) {
                soundCancellerTask.get().cancel();
            }
            
            // 最後再清一次音效
            soundCanceller.run();
        }, 10L);
    }

    public void clearChestImmediately(Block block) {
        if (!(block.getState() instanceof Chest chest)) return;
        if (!isTrashChest(chest)) return;

        schedulerHelper.runBlockTask(block, () -> {
            World world = block.getWorld();
            Location loc = block.getLocation();

            // 清除附近玩家的音效
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(loc) <= 2) {
                    cancelChestSounds(player);
                }
            }

            // 清除物品
            chest.getInventory().clear();
        });
    }

    private void cancelChestSounds(Player player) {
        // 嘗試所有音效類別
        for (SoundCategory category : SoundCategory.values()) {
            player.stopSound(Sound.BLOCK_CHEST_OPEN, category);
            player.stopSound(Sound.BLOCK_CHEST_CLOSE, category);
        }
        
        // 不指定類別的音效清除
        player.stopSound(Sound.BLOCK_CHEST_OPEN);
        player.stopSound(Sound.BLOCK_CHEST_CLOSE);
        
        // 播放音量為 0 的音效來覆蓋
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 
                        SoundCategory.BLOCKS, 0.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 
                        SoundCategory.BLOCKS, 0.0f, 1.0f);
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