package net.lausiv1024.realelevator.network;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * かごのアナウンスをクライアントへ伝える。送るのは種別・階名・読み上げ名・方向だけで、どう鳴らすかはクライアントに任せる。
 */
public record AnnouncePayload(int carId, Announcement announcement) implements CustomPacketPayload {
    public static final Type<AnnouncePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "announce"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnnouncePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnnouncePayload::carId,
            ByteBufCodecs.VAR_INT, p -> p.announcement().type().ordinal(),
            ByteBufCodecs.STRING_UTF8, p -> p.announcement().floorLabel(),
            ByteBufCodecs.STRING_UTF8, p -> p.announcement().speechName(),
            ByteBufCodecs.VAR_INT, p -> p.announcement().direction().ordinal(),
            (carId, type, label, speechName, direction) -> new AnnouncePayload(carId, new Announcement(
                    Announcement.Type.values()[Math.floorMod(type, Announcement.Type.values().length)], label, speechName,
                    TravelDirection.values()[Math.floorMod(direction, TravelDirection.values().length)])));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
