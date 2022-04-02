package lausiv1024.networking;

import lausiv1024.tileentity.ElevatorPartTE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class TileEntityMsg<TE extends ElevatorPartTE> extends Packet {
    protected BlockPos tilePos;

    public TileEntityMsg(PacketBuffer buffer) {
        tilePos = buffer.readBlockPos();
    }

    public TileEntityMsg(BlockPos pos){
        tilePos = pos;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeBlockPos(tilePos);
        writeTeData(buffer);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ClientWorld clientSideL = Minecraft.getInstance().level;
            if (clientSideL == null) return;
            TileEntity te = clientSideL.getBlockEntity(tilePos);
            if (te instanceof ElevatorPartTE)
                handlePacket((TE) te);
        });
        context.setPacketHandled(true);
    }

    protected abstract void writeTeData(PacketBuffer buffer);

    protected abstract void handlePacket(TE tile);
}
