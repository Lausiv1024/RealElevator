package lausiv1024.entity;

import lausiv1024.REEntities;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.ActionResultType;
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
        super(REEntities.ELEVATOR_BUTTON, world);
    }

    public EleButtonEntity(World world, Direction direction) {
        super(REEntities.ELEVATOR_BUTTON, world);
        //setAnBoundingBox();
        updateBoundingBox();
    }

    public int getWidthAndHeightPixels(){
        return 4;
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        return ActionResultType.SUCCESS;
    }

    private void setAnBoundingBox(){
        double boundStartX = this.blockPosition().getX() + 0.5;
        double boundStartY = this.blockPosition().getY() + 0.5;
        double boundStartZ = this.blockPosition().getZ() + 0.5;
        this.setBoundingBox(new AxisAlignedBB(boundStartX - 0.375, boundStartY - 0.375, boundStartZ - 0.03,
                boundStartX + 0.375, boundStartY + 0.375, boundStartZ + 0.03));
    }

    private void updateBoundingBox(){
        if (direction != null){
            double off = 0.2 - 1 / 256d;

            double x = this.getX() + 0.3 + direction.getStepX() * off;
            double y = this.getY() + 0.3 + direction.getStepY() * off;
            double z = this.getZ() + 0.3 + direction.getStepZ() * off;
            this.setPosRaw(x, y, z);
            double w = getWidthAndHeightPixels();
            double h = getWidthAndHeightPixels();
            double l = getWidthAndHeightPixels();
            Direction.Axis axis = this.direction.getAxis();
            double dep = 2 - 1 / 128d;
            switch (axis){
                case X:
                    w = dep;
                    break;
                case Y:
                    h = dep;
                    break;
                case Z:
                    l = dep;
            }
            w = w / 64.0D;
            h = h / 64.0D;
            l = l / 64.0D;
            this.setBoundingBox(new AxisAlignedBB(x - w, y - h, z - l, x + w, y + h, z + l));
        }
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

}
