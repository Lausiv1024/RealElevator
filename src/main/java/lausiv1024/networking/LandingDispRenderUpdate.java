package lausiv1024.networking;

import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.tileentity.ElevatorPartTE;
import lausiv1024.tileentity.FloorControllerTE;
import lausiv1024.tileentity.FloorDisplayTile;
import net.minecraft.network.PacketBuffer;

public class LandingDispRenderUpdate extends TileEntityMsg<ElevatorPartTE> {
    protected byte renderDirection;

    public LandingDispRenderUpdate(PacketBuffer buffer) {
        super(buffer);
        renderDirection = buffer.readByte();
    }

    public LandingDispRenderUpdate(ElevatorPartTE tile, ElevatorDirection direction){
        super(tile.getBlockPos());
        renderDirection = (byte) direction.nbt_index;
    }

    @Override
    protected void writeTeData(PacketBuffer buffer) {
        buffer.writeByte(renderDirection);
    }

    @Override
    protected void handlePacket(ElevatorPartTE tile) {
        if (tile instanceof FloorControllerTE){
            ((FloorControllerTE) tile).renderUpdate(ElevatorDirection.getElevatorDirectionFromIndex(renderDirection));
        }else if (tile instanceof FloorDisplayTile){
            ((FloorDisplayTile) tile).renderUpdate(ElevatorDirection.getElevatorDirectionFromIndex(renderDirection));
        }
    }
}
