package lausiv1024.entity.doors;

import lausiv1024.REItems;
import lausiv1024.entity.ElevatorPartEntity;
import lausiv1024.items.REBaseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public abstract class AbstractElevatorDoorEntity extends ElevatorPartEntity {
    public static final DataParameter<Integer> ROTATION = EntityDataManager.defineId(AbstractElevatorDoorEntity.class, DataSerializers.INT);
    public static final DataParameter<Boolean> MIRROR = EntityDataManager.defineId(AbstractElevatorDoorEntity.class, DataSerializers.BOOLEAN);
    public static final DataParameter<Integer> VENEER_ID = EntityDataManager.defineId(AbstractElevatorDoorEntity.class, DataSerializers.INT);
    public static final DataParameter<Boolean> ISLANDING = EntityDataManager.defineId(AbstractElevatorDoorEntity.class, DataSerializers.BOOLEAN);

    public AbstractElevatorDoorEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
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
    public AxisAlignedBB getBoundingBox() {
        double sx = getX(), sy = getY(), sz = getZ();
        double ex = getX(), ey = getY() + 3, ez= getZ();
        if (getRotation1() == 1 || getRotation1() == 3){
            sz -= 0.375;
            ez += 0.375;
            sx -= 0.0625;
            ex += 0.0625;
        }else{
            sx -= 0.375;
            ex += 0.375;
            sz -= 0.0625;
            ez += 0.0625;
        }
        return new AxisAlignedBB(sx, sy, sz, ex, ey, ez);
    }

    @Override
    public boolean hurt(DamageSource src, float v) {
        if (src.getEntity() instanceof PlayerEntity){
            if( ((PlayerEntity) src.getEntity()).getItemInHand(Hand.MAIN_HAND).getItem() == REItems.WRENCH.get()){
                remove();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(ROTATION, 0);
        getEntityData().define(MIRROR, false);
        getEntityData().define(VENEER_ID, 0);
        getEntityData().define(ISLANDING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        setMirror1(nbt.getBoolean("Mirror"));
        setRotation1(nbt.getInt("Rotation"));
        setVeneerId(nbt.getInt("VeneerID"));
        setLand( nbt.getBoolean("IsLanding"));
        super.readAdditionalSaveData(nbt);
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Mirror", isMirror1());
        nbt.putBoolean("IsLanding", getLand());
        nbt.putInt("Rotation", getRotation1());
        nbt.putInt("VeneerID", getVeneerId());
    }

    //isMirrorがすでにEntityで定義されてるのでこうなった
    public boolean isMirror1(){
        return getEntityData().get(MIRROR);
    }

    public void setMirror1(boolean b){
        getEntityData().set(MIRROR, b);
    }

    public void setRotation1(int rotation){
        getEntityData().set(ROTATION, rotation);
    }

    public void setVeneerId(int veneerId){
        getEntityData().set(VENEER_ID, veneerId);
    }

    //getrotationがすでにEntityで定義されてるのでこうなった
    public int getRotation1(){
        return getEntityData().get(ROTATION);
    }

    public int getVeneerId(){
        return getEntityData().get(VENEER_ID);
    }

    public boolean getLand(){
        return getEntityData().get(ISLANDING);
    }

    public void setLand(boolean a){
        getEntityData().set(ISLANDING, a);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
