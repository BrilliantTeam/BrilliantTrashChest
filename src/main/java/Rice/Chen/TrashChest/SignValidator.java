package Rice.Chen.TrashChest;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;

public class SignValidator {
    public boolean isValidSign(Block chestBlock) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : faces) {
            Block relativeBlock = chestBlock.getRelative(face);
            if (!isWallSign(relativeBlock)) continue;
            
            WallSign signData = (WallSign) relativeBlock.getBlockData();
            if (signData.getFacing() == face) {
                Sign sign = (Sign) relativeBlock.getState();
                // 檢查所有行
                for (String line : sign.getLines()) {
                    if (line != null && !line.isEmpty() && 
                        (line.equals("[銷毀]") || line.equals("[trash]"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public Block findAdjacentChest(Block signBlock) {
        if (!isWallSign(signBlock)) return null;
        
        WallSign signData = (WallSign) signBlock.getBlockData();
        Block adjacent = signBlock.getRelative(signData.getFacing().getOppositeFace());
        return adjacent.getType() == Material.TRAPPED_CHEST ? adjacent : null;
    }
    
    private boolean isWallSign(Block block) {
        return block != null && block.getType().name().contains("WALL_SIGN") && 
               block.getBlockData() instanceof WallSign;
    }
    
    public boolean hasTrashTag(String[] lines) {
        for (String line : lines) {
            if (line != null && !line.isEmpty() && 
                (line.equals("[銷毀]") || line.equals("[trash]"))) {
                return true;
            }
        }
        return false;
    }
}