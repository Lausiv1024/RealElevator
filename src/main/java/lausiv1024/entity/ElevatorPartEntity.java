package lausiv1024.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class ElevatorPartEntity extends Entity {
    protected UUID elevatorId = UUID.fromString("114514-1919-810-000-000");
    public ElevatorPartEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    public void setElevatorId(UUID elevatorId) {
        this.elevatorId = elevatorId;
    }

    public UUID getElevatorId() {
        return elevatorId;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        if (nbt.contains("ElevatorID"))
            elevatorId = nbt.getUUID("ElevatorID");
        else
            elevatorId = UUID.fromString("114514-1919-810-000-000");
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putUUID("ElevatorID", elevatorId);
    }
}
