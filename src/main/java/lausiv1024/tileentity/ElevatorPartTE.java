package lausiv1024.tileentity;

import lausiv1024.RealElevatorCore;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public abstract class ElevatorPartTE extends TileEntity {
    UUID elevatorID;
    boolean registered = false;
    public ElevatorPartTE(TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    public void setElevatorID(UUID uuid){
        elevatorID = uuid;
        registered = true;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        elevatorID = nbt.getUUID("elevatorID");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        CompoundNBT nbt1 = new CompoundNBT();
        nbt1.putUUID("elevatorID", elevatorID);
        return nbt1;
    }

    public boolean isRegistered() {
        return registered;
    }

    public UUID getElevatorID(){
        return elevatorID;
    }
}
