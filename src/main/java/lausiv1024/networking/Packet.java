package lausiv1024.networking;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class Packet {
    public abstract void write(PacketBuffer buffer);

    public abstract void handle(Supplier<NetworkEvent.Context> ctx);
}
