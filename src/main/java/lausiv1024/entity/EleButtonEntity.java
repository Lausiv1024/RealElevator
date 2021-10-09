package lausiv1024.entity;

import lausiv1024.REEntities;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class EleButtonEntity extends ElevatorPartEntity {
    protected int floorIndex = 0;
    protected boolean isActive = false;
    private Direction direction = Direction.NORTH;
    private int count = 0;

    public EleButtonEntity(EntityType<?> type, World world) {
        super(type, world);
        updateBoundingBox();
    }

    public EleButtonEntity(World world){
        super(REEntities.ELEVATOR_BUTTON.get(), world);
    }

    public EleButtonEntity(World world, Direction direction) {
        super(REEntities.ELEVATOR_BUTTON.get(), world);
        updateBoundingBox();
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        return ActionResultType.SUCCESS;
    }

    private void updateBoundingBox(){
    }

    @Override
    public void setPos(double p_70107_1_, double p_70107_3_, double p_70107_5_) {
        super.setPos(p_70107_1_, p_70107_3_, p_70107_5_);
        updateBoundingBox();
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    public boolean hurt(DamageSource source, float f1a) {
        if (source.getEntity() instanceof PlayerEntity){
            if (isAlive() && !level.isClientSide){
                remove();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        floorIndex = nbt.getInt("FloorIndex");
        isActive = nbt.getBoolean("Active");
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Active", isActive);
        nbt.putInt("FloorIndex", floorIndex);
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected float getEyeHeight(Pose pose, EntitySize size) { return 0.0f; }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        if (direction != null){
            double x = this.getX() - 1 / 16d;
            double y = this.getY() - 1 / 16d;
            double z = this.getZ() - 1 / 16d;

            double x1 = this.getX() - 1 / 64d;
            double z1 = this.getZ() - 1 / 64d;

            Direction.Axis axis = this.direction.getAxis();
            boolean isZ = false;

            double a = 1 / 8d;
            double usui = 1 / 32d;

            switch (axis){
                case X:
                    x = x1;
                    break;
                case Z:
                    z = z1;
                    isZ = true;
            }

            if (isZ){
                return new AxisAlignedBB(x, y, z, x + a, y + a, z + usui);
            }else {
                return new AxisAlignedBB(x, y, z, x + usui, y + a, z + a);
            }
        }
        return super.getBoundingBox();
    }
}
