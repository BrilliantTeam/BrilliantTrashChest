package Rice.Chen.TrashChest;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

public class SchedulerHelper {
    private final Plugin plugin;
    private final boolean isFolia;

    public SchedulerHelper(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
    }

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    public void runBlockTask(Block block, Runnable task) {
        if (isFolia) {
            plugin.getServer().getRegionScheduler().execute(plugin, block.getLocation(), task);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public void runDelayedBlockTask(Block block, Runnable task, long delay) {
        if (isFolia) {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, block.getLocation(), 
                scheduledTask -> task.run(), delay);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public void runGlobalTask(Runnable task) {
        if (isFolia) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public void runAsyncTask(Runnable task) {
        if (isFolia) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void cancelTasks() {
        if (isFolia) {
        } else {
            plugin.getServer().getScheduler().cancelTasks(plugin);
        }
    }
}