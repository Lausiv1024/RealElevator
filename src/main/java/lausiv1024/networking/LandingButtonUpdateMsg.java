package lausiv1024.networking;

import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.network.PacketBuffer;

public class LandingButtonUpdateMsg extends TileEntityMsg<LandingButtonBlockTE>{
    protected boolean up;
    protected boolean dw;
    protected boolean called;

    public LandingButtonUpdateMsg(PacketBuffer buffer){
        super(buffer);

        up = buffer.readBoolean();
        dw = buffer.readBoolean();
        called = buffer.readBoolean();
    }

    public LandingButtonUpdateMsg(LandingButtonBlockTE tile, boolean up, boolean dw, boolean called){
        super(tile.getBlockPos());
        this.up = up;
        this.dw = dw;
        this.called = called;
    }

    @Override
    protected void writeTeData(PacketBuffer buffer) {
        buffer.writeBoolean(up);
        buffer.writeBoolean(dw);
        buffer.writeBoolean(called);
    }

    @Override
    protected void handlePacket(LandingButtonBlockTE tile) {
        tile.clientUpdate(up, dw, called);
    }
}
