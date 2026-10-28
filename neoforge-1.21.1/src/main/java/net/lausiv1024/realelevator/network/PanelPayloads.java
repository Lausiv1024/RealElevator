package net.lausiv1024.realelevator.network;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 制御盤の設定画面からサーバーへ送るもの。どちらも、制御盤の近くにいるプレイヤーからのものだけを受け付ける。
 */
public final class PanelPayloads {
    private PanelPayloads() {
    }

    /** 設定の変更。 */
    public record Settings(BlockPos pos, CompoundTag settings) implements CustomPacketPayload {
        public static final Type<Settings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "panel_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Settings::pos,
                ByteBufCodecs.COMPOUND_TAG, Settings::settings,
                Settings::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Settings payload, IPayloadContext context) {
            InstallerBlockEntity panel = panelNear(context, payload.pos());
            if (panel != null) {
                panel.applySettings(payload.settings());
            }
        }
    }

    /** 昇降路の設置（確定）。 */
    public record Install(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Install> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "panel_install"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Install> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Install::pos,
                Install::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Install payload, IPayloadContext context) {
            InstallerBlockEntity panel = panelNear(context, payload.pos());
            if (panel != null) {
                context.player().displayClientMessage(panel.install(context.player()), false);
            }
        }
    }

    @Nullable
    private static InstallerBlockEntity panelNear(IPayloadContext context, BlockPos pos) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
            return null;
        }
        double distance = InstallerBlockEntity.MAX_USE_DISTANCE;
        if (player.distanceToSqr(pos.getCenter()) > distance * distance || !player.level().isLoaded(pos)) {
            return null;
        }
        return player.level().getBlockEntity(pos) instanceof InstallerBlockEntity panel ? panel : null;
    }
}
