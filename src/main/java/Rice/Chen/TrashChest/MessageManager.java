package Rice.Chen.TrashChest;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class MessageManager {
    private final String SUCCESS_MESSAGE = "§7｜§6系統§7｜§f飯娘：§a已成功設定銷毀儲物箱";
    private final String REMOVE_MESSAGE = "§7｜§6系統§7｜§f飯娘：§c已移除銷毀儲物箱設定";
    
    public void sendSuccessMessage(Player player) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR, 
                new TextComponent(SUCCESS_MESSAGE)
            );
        }
    }
    
    public void sendRemoveMessage(Player player) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR, 
                new TextComponent(REMOVE_MESSAGE)
            );
        }
    }
}