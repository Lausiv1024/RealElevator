package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.REItems;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class EleButtonEntity extends ElevatorPartEntity {
    public static final DataParameter<Direction> DIRECTION_DATA = EntityDataManager.defineId(EleButtonEntity.class, DataSerializers.DIRECTION);
    public static final DataParameter<Integer> LIGHT_COLOR = EntityDataManager.defineId(EleButtonEntity.class, DataSerializers.INT);
    public static final DataParameter<String> DISP_FL = EntityDataManager.defineId(EleButtonEntity.class, DataSerializers.STRING);
    public static final DataParameter<Boolean> IS_ACTIVE = EntityDataManager.defineId(EleButtonEntity.class, DataSerializers.BOOLEAN);
    protected int floorIndex = 0;
    protected int onTick = 0;
    protected boolean isAltanative = false;

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
        return onClick();
    }

    private ActionResultType onClick(){
        if (isActive() && onTick > 0 && isAltanative){
            setActive(false);
            return ActionResultType.SUCCESS;
        }
        setActive(true);
        onTick = 6;
        return ActionResultType.SUCCESS;
    }

    private void updateBoundingBox(){
    }

    @Override
    public void setPos(double p_70107_1_, double p_70107_3_, double p_70107_5_) {
        super.setPos(p_70107_1_, p_70107_3_, p_70107_5_);
        updateBoundingBox();
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(DIRECTION_DATA, Direction.NORTH);
        getEntityData().define(LIGHT_COLOR, 0);
        getEntityData().define(DISP_FL, "");
        getEntityData().define(IS_ACTIVE, false);
    }

    @Override
    public boolean hurt(DamageSource source, float f1a) {
        if (source.getEntity() instanceof PlayerEntity){
            if (((PlayerEntity) source.getEntity()).getItemInHand(Hand.MAIN_HAND).getItem() != REItems.WRENCH.get()) {
                onClick();
                return false;
            }
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
        isAltanative = nbt.getBoolean("IsAltanative");
        setActive(nbt.getBoolean("Active"));
        putDisplayFloor(nbt.getString("DisplayStr"));
        putLightColor(nbt.getInt("LightColor"));
        try{
            putButDirection(Direction.valueOf(nbt.getString("Direction")));
        }catch (IllegalArgumentException e){
            putButDirection(Direction.NORTH);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Active", isActive());
        nbt.putInt("FloorIndex", floorIndex);
        nbt.putBoolean("IsAltanative", isAltanative);
        nbt.putString("Direction", getButDirection().name());
        nbt.putString("DisplayStr", getDisplayFloor());
        nbt.putInt("LightColor", getLightColor());
    }

    public int getLightColor(){
        return entityData.get(LIGHT_COLOR);
    }

    public void putLightColor(int ctr){
        entityData.set(LIGHT_COLOR, ctr);
    }

    public String getDisplayFloor(){
        return entityData.get(DISP_FL);
    }

    public void putDisplayFloor(String val){
        entityData.set(DISP_FL, val);
    }

    public boolean isActive(){
        return entityData.get(IS_ACTIVE);
    }

    public void setActive(boolean val){
        entityData.set(IS_ACTIVE, val);
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
        if (level.isClientSide) return;
        if (isActive() && onTick > 0)
            onTick--;
        if (isAltanative) return;
        if (onTick < 1){
            setActive(false);
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public Direction getButDirection(){
        return entityData.get(DIRECTION_DATA);
    }

    public void putButDirection(Direction direction){
        entityData.set(DIRECTION_DATA, direction);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        double sx = getX(), sy = getY() , sz = getZ();
        double ex = getX(), ey = getY() + 0.125, ez= getZ();
        if (getButDirection() == Direction.EAST || getButDirection() == Direction.WEST){
            sz -= 0.0625;
            ez += 0.0625;
            sx -= 0.015625;
            ex += 0.015625;
        }else{
            sx -= 0.0625;
            ex += 0.0625;
            sz -= 0.015625;
            ez += 0.015625;
        }
        return new AxisAlignedBB(sx, sy, sz, ex, ey, ez);
    }
}
