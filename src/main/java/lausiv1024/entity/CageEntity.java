package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.util.CageUtil;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

public class CageEntity extends ElevatorPartEntity {
    public static final DataParameter<Integer> ROTATION_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);
    private boolean initialized = false;

    public CageEntity(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public CageEntity(World world){
        super(REEntities.CAGE.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(ROTATION_DATA, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!initialized) initCage();

        applyCollisionToPassenger();

        xo = getX();
        yo = getY();
        zo = getZ();
    }

    private void applyCollisionToPassenger(){
//        List<Entity> e = level.getEntities(this ,CageUtil.getFloorCollision(getX(), getY(), getZ()));
//        List<Entity> eRoof = level.getEntities(this, CageUtil.getRoofCollision(getX(), getY(), getZ()));
        CageUtil.applyCollision(this);
    }

    private void initCage(){
        initialized = true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        putRotation(nbt.getInt("Rotation"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Rotation", getRotation());
    }

    public Vector3d getPrevPos(){
        return initialized ? position() : new Vector3d(xo, yo, zo);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(getX() - 2, getY() - 1.8, getZ() - 2, getX() + 2, getY() + 5, getZ() + 2);
    }

    @Override
    protected float getEyeHeight(Pose p_213316_1_, EntitySize p_213316_2_) {
        return 0.0f;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void putRotation(int rotation){
        if (rotation > 4 || rotation < 0){
            LOGGER.warn("Invalid rotation!! Require : (0 <= rotation < 4) Provided : {}", rotation);
            return;
        }
        entityData.set(ROTATION_DATA, rotation);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getRotation(){
        return entityData.get(ROTATION_DATA);
    }

    public boolean canCollideWith(Entity entity){
        if (entity instanceof PlayerEntity && entity.isSpectator()){
            return false;
        }
        if (entity.noPhysics) return false;
        if (entity instanceof HangingEntity) return false;
        if (entity instanceof ProjectileEntity) return false;
        if (entity instanceof ElevatorPartEntity) return false;
        return entity.getPistonPushReaction() == PushReaction.NORMAL;
    }
}
