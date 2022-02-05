package lausiv1024.entity;

import lausiv1024.networking.Vec3Serializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class ElevatorPartEntity extends Entity {
    protected UUID elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");

//    public static final DataParameter<Vector3d> VELOCITY = EntityDataManager.defineId(ElevatorPartEntity.class, Vec3Serializer.VEC3);

    public ElevatorPartEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    public void setElevatorId(UUID elevatorId) {
        this.elevatorId = elevatorId;
    }

    public UUID getElevatorId() {
        return elevatorId;
    }

    protected boolean moving = false;

    @Override
    protected void defineSynchedData() {
//        entityData.define(VELOCITY, Vector3d.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        if (nbt.contains("ElevatorID"))
            elevatorId = nbt.getUUID("ElevatorID");
        else
            elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");
//        if (entityData != null)
//            setVelocity(new Vector3d(nbt.getDouble("VelocityX"), nbt.getDouble("VelocityY"), nbt.getDouble("VelocityZ")));
        moving = nbt.getBoolean("Moving");
    }

    @Override
    public void tick() {
        super.tick();
//        xo = getX();
//        yo = getY();
//        zo = getZ();
        if (moving) doMotion();
    }

    protected void doMotion(){
        if (!level.isClientSide){
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y,
                    getZ() + getDeltaMovement().z);
            //move(MoverType.SELF, getDeltaMovement());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        if (elevatorId != null)
            nbt.putUUID("ElevatorID", elevatorId);
        //nbt.putDouble("VelocityX", getVelocity().x);
        //nbt.putDouble("VelocityY", getVelocity().y);
        //nbt.putDouble("VelocityZ", getVelocity().z);
        nbt.putBoolean("Moving", moving);
    }
}
