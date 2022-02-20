package lausiv1024.entity;

import lausiv1024.REEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class CageEntity extends ElevatorPartEntity {
    public static final DataParameter<Integer> ROTATION_DATA = EntityDataManager.defineId(CageEntity.class, DataSerializers.INT);

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
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        putRotation(nbt.getInt("Rotation"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Rotation", getRotation());
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(getX() - 2, getY() - 2, getZ() - 2, getX() + 2, getY() + 2, getZ() + 2);
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

    public int getRotation(){
        return entityData.get(ROTATION_DATA);
    }
}
