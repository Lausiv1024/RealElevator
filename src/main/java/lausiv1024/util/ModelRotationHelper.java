package lausiv1024.util;

import net.minecraft.block.Block;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;

public class ModelRotationHelper {
    public static VoxelShape rotate(double startX, double startY, double startZ, double endX, double endY, double endZ, Direction direction){
        switch (direction){
            case NORTH:
                return Block.box(startX, startY, startZ, endX, endY, endZ);
            case SOUTH:
                return Block.box(16 - startX, startY, 16- startZ, 16 - endX, endY, 16 - endZ);
            case EAST:
                return Block.box(16 - startZ, startY, 16 - startX, 16 - endZ, endY, 16 - endX);
            case WEST:
                return Block.box(startZ, startY, startX, endZ, endY, endX);
            default:
                throw new IllegalArgumentException("Invalid Direction : UP DOWN");
        }
    }
}
