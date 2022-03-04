package lausiv1024.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;

public class REMathHelper {
    public static AxisAlignedBB rotateLocalAABB(AxisAlignedBB bb, int rotation){
        switch (rotation){
            case 0:
                return new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            case 3:
                return new AxisAlignedBB(-bb.minZ, bb.minY, bb.minX, -bb.maxZ, bb.maxY, bb.maxX);
            case 2:
                return new AxisAlignedBB(-bb.minX, bb.minY, -bb.minZ, -bb.maxX, bb.maxY, -bb.maxZ);
            case 1:
                return new AxisAlignedBB(bb.minZ, bb.minY, -bb.minX, bb.maxZ, bb.maxY, -bb.maxX);
        }
        return bb;
    }
}
