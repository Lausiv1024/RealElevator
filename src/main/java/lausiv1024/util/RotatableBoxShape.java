package lausiv1024.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;

public class RotatableBoxShape {
    final Vector3d start;
    final Vector3d end;

    public RotatableBoxShape(double startX, double startY, double startZ, double endX, double endY, double endZ){
        start = new Vector3d(startX, startY, startZ);
        end = new Vector3d(endX, endY, endZ);
    }

    public VoxelShape getNorth(){
        return ModelRotationHelper.rotate(start.x, start.y, start.z, end.x, end.y, end.z, Direction.NORTH);
    }

    public VoxelShape getSouth(){
        return ModelRotationHelper.rotate(start.x, start.y, start.z, end.x, end.y, end.z, Direction.SOUTH);
    }

    public VoxelShape getEast(){
        return ModelRotationHelper.rotate(start.x, start.y, start.z, end.x, end.y, end.z, Direction.EAST);
    }

    public VoxelShape getWest(){
        return ModelRotationHelper.rotate(start.x, start.y, start.z, end.x, end.y, end.z, Direction.WEST);
    }

    public RotatableBoxShape move(double x, double y, double z){
        return new RotatableBoxShape(start.x + x, start.y + y, start.z + z, end.x + x, end.y + y, end.z + z);
    }
}