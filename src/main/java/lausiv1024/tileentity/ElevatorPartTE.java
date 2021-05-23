package lausiv1024.tileentity;

import lausiv1024.RealElevatorCore;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class ElevatorPartTE extends TileEntity {
    protected UUID elevatorID;
    boolean registered = false;
    public ElevatorPartTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType);
        this.elevatorID = elevatorID;
    }

    public void setElevatorID(UUID uuid){
        elevatorID = uuid;
        registered = true;
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        this.handleUpdateTag(getLevel().getBlockState(this.worldPosition), pkt.getTag());
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        elevatorID = nbt.getUUID("ElevatorId");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putUUID("ElevatorId", elevatorID);
        return nbt;
    }

    public boolean isRegistered() {
        return registered;
    }

    public UUID getElevatorID(){
        return elevatorID;
    }
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.worldPosition, 3, this.getUpdateTag());
    }

    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }
}
