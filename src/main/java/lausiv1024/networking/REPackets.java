package lausiv1024.networking;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;

import lausiv1024.RealElevator;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;


import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum REPackets {
    LANDING_UPDATE_BUT(LandingButtonUpdateMsg.class, LandingButtonUpdateMsg::new, PLAY_TO_CLIENT),
    DISPLAY_UPDATE(LandingDispUpdateMsg.class, LandingDispUpdateMsg::new, PLAY_TO_CLIENT),
    DISPLAY_RENDER_UPDATE(LandingDispRenderUpdate.class, LandingDispRenderUpdate::new, PLAY_TO_CLIENT)
    ;

    <T extends Packet> REPackets(Class<T> type, Function<PacketBuffer, T> factory, NetworkDirection direction){
        packet = new LoadedPacket<>(type, factory, direction);
    }

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(RealElevator.ID, "net"))
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION).simpleChannel();

    private LoadedPacket<?> packet;

    public static void registerPackets(){
        RealElevator.LOGGER.info("PacketRegister");
        for (REPackets packet : values())
            packet.packet.register();
    }

    private static class LoadedPacket<T extends Packet>{
        private static int index = 0;
        BiConsumer<T, PacketBuffer> encoder;
        Function<PacketBuffer, T> decoder;
        BiConsumer<T, Supplier<NetworkEvent.Context>> handler;
        Class<T> type;
        NetworkDirection netDirection;

        private LoadedPacket(Class<T> type, Function<PacketBuffer, T> factory, NetworkDirection netDirection){
            encoder = T::write;
            decoder = factory;
            handler = T::handle;
            this.type = type;
            this.netDirection = netDirection;
        }

        private void register(){
            CHANNEL.messageBuilder(type, index++, netDirection).encoder(encoder).decoder(decoder)
                    .consumer(handler).add();
        }
    }
}
