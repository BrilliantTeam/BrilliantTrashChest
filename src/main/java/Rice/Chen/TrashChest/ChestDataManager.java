package Rice.Chen.TrashChest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestDataManager {
    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Set<Location> trashChests;
    private final boolean isFolia;
    private static final int BATCH_SIZE = 20;
    private static final long BATCH_DELAY = 1L;
    
    public ChestDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "trashchests.yml");
        this.trashChests = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.isFolia = isFolia();
        loadData();
    }
    
    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public void loadData() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create trashchests.yml!");
                return;
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        int count = 0;
        
        for (String key : config.getKeys(false)) {
            String worldName = config.getString(key + ".world");
            int x = config.getInt(key + ".x");
            int y = config.getInt(key + ".y");
            int z = config.getInt(key + ".z");
            
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                trashChests.add(loc);
                count++;
            }
        }
        
        if (count > 0) {
            plugin.getLogger().info("Loaded " + count + " trash chest(s) from storage.");
        }
    }
    
    public void saveData() {
        config = new YamlConfiguration();
        
        int index = 0;
        for (Location loc : trashChests) {
            String key = "chest" + index;
            config.set(key + ".world", loc.getWorld().getName());
            config.set(key + ".x", loc.getBlockX());
            config.set(key + ".y", loc.getBlockY());
            config.set(key + ".z", loc.getBlockZ());
            
            index++;
        }
        
        try {
            config.save(configFile);
            if (index > 0) {
                plugin.getLogger().info("Saved " + index + " trash chest(s) to storage.");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save trash chests data!");
        }
    }
    
    public void addTrashChest(Block block) {
        trashChests.add(block.getLocation());
        saveData();
    }
    
    public void removeTrashChest(Block block) {
        trashChests.remove(block.getLocation());
        saveData();
    }
    
    public boolean isTrashChest(Block block) {
        return trashChests.contains(block.getLocation());
    }

    public void restoreTrashChests(TrashChestManager manager) {
        if (trashChests.isEmpty()) {
            return;
        }

        Map<String, List<Location>> worldGroups = new HashMap<>();
        for (Location loc : trashChests) {
            String worldName = loc.getWorld().getName();
            worldGroups.computeIfAbsent(worldName, k -> new ArrayList<>()).add(loc);
        }

        int totalChests = trashChests.size();
        plugin.getLogger().info("Starting to restore " + totalChests + " trash chest(s)...");

        for (Map.Entry<String, List<Location>> worldEntry : worldGroups.entrySet()) {
            String worldName = worldEntry.getKey();
            List<Location> worldChests = worldEntry.getValue();
            plugin.getLogger().info("Processing " + worldChests.size() + " chest(s) in world '" + worldName + "'");
            processBatch(worldChests, manager, 0, worldName);
        }
    }

    private void processBatch(List<Location> locations, 
                            TrashChestManager manager, 
                            int startIndex, 
                            String worldName) {
        if (startIndex >= locations.size()) {
            if (locations.size() > 0) {
                plugin.getLogger().info("Completed restoring trash chests in world '" + worldName + "'");
            }
            return;
        }

        int endIndex = Math.min(startIndex + BATCH_SIZE, locations.size());
        List<Location> batch = locations.subList(startIndex, endIndex);
        int processedCount = startIndex + batch.size();
        int worldTotal = locations.size();
        
        double percentage = (double) processedCount / worldTotal * 100;
        plugin.getLogger().info(String.format("World '%s': Processing batch %d-%d of %d (%.1f%%)", 
            worldName, startIndex + 1, endIndex, worldTotal, percentage));

        if (isFolia) {
            var regionScheduler = plugin.getServer().getRegionScheduler();
            for (Location loc : batch) {
                regionScheduler.execute(plugin, loc, () -> {
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Chest) {
                        manager.restoreTrashChest(block);
                    } else {
                        trashChests.remove(loc);
                    }
                });
            }

            if (endIndex < locations.size()) {
                plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, 
                    task -> processBatch(locations, manager, endIndex, worldName), 
                    BATCH_DELAY);
            }
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Location loc : batch) {
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Chest) {
                        manager.restoreTrashChest(block);
                    } else {
                        trashChests.remove(loc);
                    }
                }

                if (endIndex < locations.size()) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, 
                        () -> processBatch(locations, manager, endIndex, worldName), 
                        BATCH_DELAY);
                }
            });
        }
    }
}