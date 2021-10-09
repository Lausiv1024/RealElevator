package lausiv1024.entity.doors;

import lausiv1024.RealElevatorCore;
import lausiv1024.entity.ElevatorPartEntity;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public abstract class AbstractElevatorDoorEntity extends ElevatorPartEntity {

    private Direction direction = Direction.NORTH;
    public int tall = 3;
    public int surfaceType = 0;
    public AbstractElevatorDoorEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    protected void applyDirection(){
        if (direction != null & !level.isClientSide){
            this.yRot = this.direction.getOpposite().get2DDataValue() * 90;
            this.yRotO = yRot;
        }
    }

    @Override
    protected float getEyeHeight(Pose pose, EntitySize entitySize) {
        return 0.0f;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void setPos(double p_70107_1_, double p_70107_3_, double p_70107_5_) {
        super.setPos(p_70107_1_, p_70107_3_, p_70107_5_);
        applyDirection();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected AxisAlignedBB getBoundingBoxForPose(Pose p_213321_1_) {
        return getBoundingBox();
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        if (this.getDirection() != null){
            double x = this.getX() - 0.5d;
            //double y = tall == 3 ? this.getY() - 3d / 2 : this.getY() - 2;
            double y = this.getY();
            double z = this.getZ() - 0.5d;

            double x1 = this.getX() - 1 / 16d;
            double z1 = this.getZ() - 1 / 16d;

            Direction.Axis axis = Direction.Axis.Z;
            if (yRot == -90.0f || yRot == 90.0f){
                axis = Direction.Axis.X;
            }
            boolean isZ = false;

            double a = 1;
            double height = tall;
            double usui = 1 / 8d;

            switch (axis){
                case X:
                    x = x1;
                    break;
                case Z:
                    z = z1;
                    isZ = true;
                    break;
            }

            if (isZ){
                return new AxisAlignedBB(x, y, z, x + a, y + height, z + usui);
            }else {
                return new AxisAlignedBB(x, y, z, x + usui, y + height, z + a);
            }
        }
        return super.getBoundingBox();
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
