package net.lausiv1024.realelevator.network;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアントでボタンを押したことをサーバーへ伝える。ボタンは、それを持つエンティティとボタンの番号で指す。
 *
 * <p>クリック音は押したとき（{@link Action#PRESS}）と離したとき（{@link Action#RELEASE}）に鳴らす。開ボタンを押したままのあいだの
 * 繰り返し（{@link Action#REPEAT}）では鳴らさない。繰り返しは、開ボタンと乗場のボタンだけ（{@link ButtonHost#holdButton}）。
 */
public record PressButtonPayload(int entityId, int buttonIndex, Action action) implements CustomPacketPayload {
    public enum Action {
        PRESS,
        /** 開ボタンを押したままのあいだの繰り返し。 */
        REPEAT,
        RELEASE
    }

    public static final Type<PressButtonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "press_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PressButtonPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PressButtonPayload::entityId,
            ByteBufCodecs.VAR_INT, PressButtonPayload::buttonIndex,
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal), PressButtonPayload::action,
            PressButtonPayload::new);

    /** クライアントとサーバーで位置が少しずれていても押せるよう、手の届く距離に足す余裕。 */
    private static final double REACH_MARGIN = 1.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバーのメインスレッドで呼ばれる。手の届く範囲にあるボタンだけを押す。 */
    public static void handle(PressButtonPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        Entity entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof ButtonHost host) || payload.buttonIndex() < 0 || payload.buttonIndex() >= host.buttons().size()) {
            return;
        }
        AABB box = host.buttonBox(payload.buttonIndex());
        // ボタンはブロックと同じ距離まで届く
        double reach = player.blockInteractionRange() + REACH_MARGIN;
        if (box.distanceToSqr(player.getEyePosition()) > reach * reach) {
            return;
        }
        Vec3 center = box.getCenter();
        switch (payload.action()) {
            case PRESS -> {
                host.pressButton(payload.buttonIndex());
                player.level().playSound(null, center.x, center.y, center.z, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.5F);
            }
            case REPEAT -> {
                if (ButtonHost.isHoldable(host.buttons().get(payload.buttonIndex()))) {
                    host.holdButton(payload.buttonIndex());
                }
            }
            case RELEASE -> player.level().playSound(null, center.x, center.y, center.z, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 1.5F);
        }
    }
}
