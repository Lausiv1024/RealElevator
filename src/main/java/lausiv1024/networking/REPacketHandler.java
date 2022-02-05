package lausiv1024.networking;

import lausiv1024.RealElevator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class REPacketHandler {
    private REPacketHandler(){

    }

    private static final String PROTOCOL_VERSION = "1";


    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RealElevator.ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init(){
        int index = 0;
        INSTANCE.messageBuilder(EntityRenderUpdatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT).encoder(EntityRenderUpdatePacket::encode)
                .decoder(EntityRenderUpdatePacket::new).consumer(EntityRenderUpdatePacket::handle).add();
    }
}
