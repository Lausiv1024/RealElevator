package lausiv1024.util;

import net.minecraft.block.Block;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

public class ModelRotationHelper {

    public static VoxelShape rotate(double startX, double startY, double startZ, double endX, double endY, double endZ, Direction direction){
        switch (direction){
            case NORTH:
                return Block.box(startX, startY, startZ, endX, endY, endZ);
            case SOUTH:
                return Block.box(16 - startX, startY, 16- startZ, 16 - endX, endY, 16 - endZ);
            case EAST:
                return Block.box(16 - startZ, startY, startX, 16 - endZ, endY, endX);
            case WEST:
                return Block.box(startZ, startY,16 - startX, endZ, endY,16 - endX);
            default:
                throw new IllegalArgumentException("Invalid Direction : UP DOWN");
        }
    }

    public static Rotation toRotation(Direction direction){
        switch (direction){
            case NORTH:
                return Rotation.NONE;
            case SOUTH:
                return Rotation.CLOCKWISE_180;
            case EAST:
                return Rotation.CLOCKWISE_90;
            case WEST:
                return Rotation.COUNTERCLOCKWISE_90;
            default:throw new UnsupportedOperationException("Can't set Vertical Direction");
        }
    }
}
