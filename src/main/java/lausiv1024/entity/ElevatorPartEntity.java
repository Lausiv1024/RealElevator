package lausiv1024.entity;

import com.google.common.collect.Maps;
import lausiv1024.elevator.AbstractElevator;
import lausiv1024.elevator.Elevator;
import lausiv1024.tileentity.ElevatorPartTE;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Map;
import java.util.UUID;

public abstract class ElevatorPartEntity extends Entity {
    public static class ElevatorTracker{
        protected static final Map<UUID, ElevatorPartEntity> eleMap = Maps.newHashMap();
    }

    public static ElevatorPartEntity getEntityFromUUID(UUID uuid, World level){
        if (level instanceof ServerWorld){
            ServerWorld s = (ServerWorld) level;
            Entity entity = ((ServerWorld) level).getEntity(uuid);
            if (entity instanceof ElevatorPartEntity) return (ElevatorPartEntity) entity;
        }
        return ElevatorTracker.eleMap.get(uuid);
    }

    protected UUID elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected int lerpStep;
    protected AbstractElevator elevator;
    protected BlockPos controllerPos;

    public ElevatorPartEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        elevator = null;
    }

    public ElevatorPartEntity(EntityType<? extends Entity> type, World world, Elevator elevator){
        super(type, world);
        this.elevator = elevator;
    }

    public void setElevatorId(UUID elevatorId) {
        this.elevatorId = elevatorId;
    }

    public UUID getElevatorId() {
        return elevatorId;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        if (nbt.hasUUID("ElevatorID"))
            elevatorId = nbt.getUUID("ElevatorID");
        else {
            elevatorId = UUID.fromString("C4FBA282-1EBF-4747-A805-5F32B948A8FD");
        }
        controllerPos = NBTUtil.readBlockPos(nbt.getCompound("ControllerPos"));
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
        doMotion();
        xo = getX();
        yo = getY();
        zo = getZ();
    }

    public Vector3d getPrevPos(){
        return new Vector3d(xo, yo, zo);
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

    public void setPos(Vector3d vec3){
        setPos(vec3.x, vec3.y, vec3.z);
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
    public void onAddedToWorld() {
        super.onAddedToWorld();
        ElevatorTracker.eleMap.put(getUUID(), this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        ElevatorTracker.eleMap.remove(getUUID());
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        if (elevatorId != null)
            nbt.putUUID("ElevatorID", elevatorId);
        if (controllerPos != null)
            nbt.put("ControllerPos", NBTUtil.writeBlockPos(controllerPos));
    }

    public AbstractElevator getElevator() {
        return elevator;
    }

    public void setElevator(AbstractElevator elevator) {
        this.elevator = elevator;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }
}
