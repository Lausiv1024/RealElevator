package lausiv1024.tileentity;

import lausiv1024.elevator.Elevator;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class ElevatorPartTE extends TileEntity {
    protected UUID elevatorID;
    boolean registered = false;
    protected boolean isController;
    BlockPos controllerPos;
    public ElevatorPartTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType);
        this.elevatorID = elevatorID;
        isController = false;
    }

    public ElevatorPartTE(TileEntityType<?> tileEntityType){
        super(tileEntityType);
        isController = false;
    }

    public void setElevatorID(UUID uuid){
        elevatorID = uuid;
        registered = true;
    }

//    @Override
//    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
//        this.handleUpdateTag(getLevel().getBlockState(this.worldPosition), pkt.getTag());
//    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.hasUUID("ElevatorId"))
            elevatorID = nbt.getUUID("ElevatorId");
        if (nbt.contains("ControllerPos")){
            NBTUtil.readBlockPos(nbt.getCompound("ControllerPos"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        if (elevatorID != null)
            nbt.putUUID("ElevatorId", elevatorID);
        if (controllerPos != null)
            nbt.put("ControllerPos",NBTUtil.writeBlockPos(controllerPos));
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

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }

    public boolean isController() {
        return isController;
    }

    public PacketDistributor.PacketTarget target(){
        return PacketDistributor.TRACKING_CHUNK.with(this::containedChunk);
    }

    public Chunk containedChunk(){
        SectionPos pos = SectionPos.of(worldPosition);
        return level.getChunk(pos.x(), pos.z());
    }
}
