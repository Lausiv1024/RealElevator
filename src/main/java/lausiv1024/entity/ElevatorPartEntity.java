package lausiv1024.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public abstract class ElevatorPartEntity extends Entity {
    protected UUID elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected int lerpStep;

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
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        if (nbt.contains("ElevatorID"))
            elevatorId = nbt.getUUID("ElevatorID");
        else {
            elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");
        }
        moving = nbt.getBoolean("Moving");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void lerpTo(double lx, double ly, double lz, float yRot, float xRot, int a, boolean bb) {
        lerpX = lx;
        lerpY = ly;
        lerpZ = lz;
        lerpStep = 10;
    }

    @Override
    public void tick() {
        super.tick();
        if (moving) doMotion();
    }

    protected void doMotion(){
        tickLerp();
        if (isControlledByLocalInstance()){
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y,
                    getZ() + getDeltaMovement().z);
        }else{
            setDeltaMovement(Vector3d.ZERO);
        }
    }

    private void tickLerp(){
        if (isControlledByLocalInstance()){
            this.lerpStep = 0;
            setPacketCoordinates(getX(), getY(), getZ());
        }

        if (lerpStep > 0){
            double d0 = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpStep;
            double d1 = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpStep;
            double d2 = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpStep;

            lerpStep--;
            setPos(d0, d1, d2);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        if (elevatorId != null)
            nbt.putUUID("ElevatorID", elevatorId);
        nbt.putBoolean("Moving", moving);
    }
}
