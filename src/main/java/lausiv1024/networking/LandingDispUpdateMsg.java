package lausiv1024.networking;

import lausiv1024.tileentity.ElevatorPartTE;
import lausiv1024.tileentity.FloorControllerTE;
import lausiv1024.tileentity.FloorDisplayTile;
import net.minecraft.network.PacketBuffer;

public class LandingDispUpdateMsg extends TileEntityMsg<ElevatorPartTE> {
    protected String floorStr;
    protected byte renderarrowDirection;
    protected byte arrowFrame;
    protected boolean isBlink;

    public LandingDispUpdateMsg(PacketBuffer buffer) {
        super(buffer);
        floorStr = buffer.readUtf();
        renderarrowDirection = buffer.readByte();
        arrowFrame = buffer.readByte();
        isBlink = buffer.readBoolean();
    }

    public LandingDispUpdateMsg(ElevatorPartTE tile, String floorStr, byte renderarrowDirection, byte arrowFrame, boolean isBlink){
        super(tile.getBlockPos());
        this.floorStr = floorStr;
        this.renderarrowDirection = renderarrowDirection;
        this.arrowFrame = arrowFrame;
        this.isBlink = isBlink;
    }

    @Override
    protected void writeTeData(PacketBuffer buffer) {
        buffer.writeUtf(floorStr);
        buffer.writeByte(renderarrowDirection);
        buffer.writeByte(arrowFrame);
        buffer.writeBoolean(isBlink);
    }

    @Override
    protected void handlePacket(ElevatorPartTE tile) {
        if (tile instanceof FloorControllerTE){
            ((FloorControllerTE) tile).clientUpdate(floorStr, renderarrowDirection, arrowFrame, isBlink);
        }else if (tile instanceof FloorDisplayTile){
            ((FloorDisplayTile) tile).clientUpdate(floorStr, renderarrowDirection, arrowFrame, isBlink);
        }
    }
}
