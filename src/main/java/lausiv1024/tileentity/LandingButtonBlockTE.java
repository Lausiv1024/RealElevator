package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.blocks.FloorController;
import lausiv1024.elevator.AbstractElevator;
import lausiv1024.networking.LandingButtonUpdateMsg;
import lausiv1024.networking.REPackets;
import net.minecraft.block.BlockState;
import net.minecraft.command.impl.GiveCommand;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ChunkLoaderUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.UUID;

public class LandingButtonBlockTE extends ElevatorPartTE  implements ITickableTileEntity {
    protected boolean up = false;
    protected boolean down = false;
    protected boolean called = false;
    protected int color = 0;
    protected byte floorIndex;

    public LandingButtonBlockTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType, elevatorID);
    }

    public LandingButtonBlockTE(TileEntityType<?> tileEntityType){
        super(tileEntityType);
    }

    public LandingButtonBlockTE(){
        super(RETileEntities.LANDING_BUTTON_TE.get());
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        if (color > 1) return;
        this.color = color;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled() {
        if (getBlockState().getValue(FloorController.IS_SINGLE)) {
            this.called = updateData();
        }
        REPackets.CHANNEL.send(target(), new LandingButtonUpdateMsg(this, up, down, called));
    }

    public void upA(){
        boolean b = updateData();
        up = b;
        called = b;
        REPackets.CHANNEL.send(target(), new LandingButtonUpdateMsg(this, up, down, called));
    }

    public void dwA(){
        boolean b = updateData();
        down = b;
        called = b;
        REPackets.CHANNEL.send(target(), new LandingButtonUpdateMsg(this, up, down, called));
    }

    public void rm(boolean downM, boolean upM){
        down = !downM;
        up = !upM;
        if (getBlockState().getValue(FloorController.IS_SINGLE)) return;
        if (!down && !up) called = false;
    }

    public void clientUpdate(boolean up, boolean dw, boolean called){
        this.up = up;
        this.down = dw;
        this.called = called;
    }

    private boolean updateData(){
        AbstractElevator elevator = AbstractElevator.getElevatorFromUUID(elevatorID, level);
        if (elevator == null) return false;
        return elevator.onLandingBlockClicked(floorIndex, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        up = nbt.getBoolean("Up");
        down = nbt.getBoolean("Down");
        color = nbt.getInt("Color");
        called = nbt.getBoolean("Called");
        floorIndex = nbt.getByte("FloorIndex");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putBoolean("Up", up);
        nbt.putBoolean("Down", down);
        nbt.putInt("Color", color);
        nbt.putBoolean("Called", called);
        nbt.putByte("FloorIndex", floorIndex);
        return nbt;
    }



    public void setFloorIndex(byte floorIndex) {
        this.floorIndex = floorIndex;
    }

    @Override
    public void tick() {

    }
}
