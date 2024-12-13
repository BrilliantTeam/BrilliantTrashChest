package Rice.Chen.TrashChest;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

public class TrashChest extends JavaPlugin {
    private TrashChestManager manager;
    private TrashChestListener listener;
    private MessageManager messageManager;
    private SignValidator signValidator;
    private ChestDataManager dataManager;
    private final boolean isFolia;

    public TrashChest() {
        this.isFolia = isFolia();
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        messageManager = new MessageManager();
        signValidator = new SignValidator();
        dataManager = new ChestDataManager(this);
        manager = new TrashChestManager(this, messageManager, dataManager);
        listener = new TrashChestListener(this, manager, signValidator, messageManager);
        
        getServer().getPluginManager().registerEvents(listener, this);

        if (isFolia) {
            getServer().getAsyncScheduler().runNow(this, task -> {
                dataManager.restoreTrashChests(manager);
                getLogger().info("TrashChest " + getDescription().getVersion() + " has been enabled!");
                getLogger().info("Author: RiceChen_");
            });
        } else {
            getServer().getScheduler().runTask(this, () -> {
                dataManager.restoreTrashChests(manager);
                getLogger().info("TrashChest " + getDescription().getVersion() + " has been enabled!");
                getLogger().info("Author: RiceChen_");
            });
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }

        HandlerList.unregisterAll(this);
        manager = null;
        listener = null;
        messageManager = null;
        signValidator = null;
        dataManager = null;

        getLogger().info("TrashChest has been disabled!");
    }
}