package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.elevator.AbstractElevator;
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
    public static final DataParameter<Boolean> IS_ENABLED = EntityDataManager.defineId(EleButtonEntity.class, DataSerializers.BOOLEAN);
    protected int floorIndex = 0; //負の値は特殊ボタン(しめる ひらく 開延長) open:-1  close:-2  prolong:-3
    protected int onTick = 0;
    protected boolean isAltanative = false;
    protected boolean clicked = false;

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
        putButDirection(direction);
    }

    public EleButtonEntity(World world, Direction direction, boolean alt) {
        super(REEntities.ELEVATOR_BUTTON.get(), world);
        updateBoundingBox();
        putButDirection(direction);
        isAltanative = alt;
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        return onClick();
    }

    private ActionResultType onClick(){
        if (level.isClientSide) return ActionResultType.SUCCESS;
        AbstractElevator elevator = AbstractElevator.getElevatorFromUUID(elevatorId, level);
        LOGGER.info(elevator);
        if (isActive() && onTick > 0 && isAltanative){
            setActive(false);
            clicked = false;
            if (elevator != null) elevator.onEleButtonclicked(this, false);
            return ActionResultType.SUCCESS;
        }
        clicked = true;
        onTick = 6;
        if (elevator != null) setActive(elevator.onEleButtonclicked(this, true));
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
        getEntityData().define(IS_ENABLED, true);
    }

//    @Override
//    public boolean hurt(DamageSource source, float f1a) {
//        if (source.getEntity() instanceof PlayerEntity){
//            if (((PlayerEntity) source.getEntity()).getItemInHand(Hand.MAIN_HAND).getItem() != REItems.WRENCH.get()) {
//                onClick();
//                return false;
//            }
//            if (isAlive() && !level.isClientSide){
//                remove();
//            }
//            return true;
//        }
//        return false;
//    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        floorIndex = nbt.getInt("FloorIndex");
        isAltanative = nbt.getBoolean("IsAltanative");
        clicked = nbt.getBoolean("Clicked");
        setActive(nbt.getBoolean("Active"));
        setIsEnabled(nbt.getBoolean("Enabled"));
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
        nbt.putBoolean("IsAltanative", isAltanative);
        nbt.putBoolean("Clicked", clicked);
        nbt.putInt("FloorIndex", floorIndex);

        nbt.putBoolean("Enabled", is_Enabled());
        nbt.putString("Direction", getButDirection().name());
        nbt.putString("DisplayStr", getDisplayFloor());
        nbt.putInt("LightColor", getLightColor());
    }

    public void setFloorIndex(int floorIndex) {
        this.floorIndex = floorIndex;
    }

    public int getFloorIndex() {
        return floorIndex;
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

    public boolean isClicked() {
        return clicked;
    }

    public boolean is_Enabled(){
        return getEntityData().get(IS_ENABLED);
    }

    public void setIsEnabled(boolean enabled){
        getEntityData().set(IS_ENABLED, enabled);
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

    public boolean isAltanative() {
        return isAltanative;
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
