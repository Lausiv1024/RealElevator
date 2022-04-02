package lausiv1024.networking;

import lausiv1024.tileentity.ElevatorPartTE;
import lausiv1024.tileentity.FloorControllerTE;
import lausiv1024.tileentity.FloorDisplayTile;
import net.minecraft.network.PacketBuffer;

public class LandingDispUpdateMsg extends TileEntityMsg<ElevatorPartTE> {
    protected String floorStr;
    protected int arrowDirection;
    protected boolean isBlink;

    public LandingDispUpdateMsg(PacketBuffer buffer) {
        super(buffer);
        floorStr = buffer.readUtf();
        arrowDirection = buffer.readInt();
        isBlink = buffer.readBoolean();
    }

    public LandingDispUpdateMsg(ElevatorPartTE tile, String floorStr, int arrowDirection, boolean isBlink){
        super(tile.getBlockPos());
        this.floorStr = floorStr;
        this.arrowDirection = arrowDirection;
        this.isBlink = isBlink;
    }

    @Override
    protected void writeTeData(PacketBuffer buffer) {
        buffer.writeUtf(floorStr);
        buffer.writeInt(arrowDirection);
        buffer.writeBoolean(isBlink);
    }

    @Override
    protected void handlePacket(ElevatorPartTE tile) {
        if (tile instanceof FloorControllerTE){
            ((FloorControllerTE) tile).clientUpdate(floorStr, arrowDirection, isBlink);
        }else if (tile instanceof FloorDisplayTile){
            ((FloorDisplayTile) tile).clientUpdate(floorStr, arrowDirection, isBlink);
        }
    }
}
