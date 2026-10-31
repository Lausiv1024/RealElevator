package net.lausiv1024.realelevator.network;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.lausiv1024.realelevator.item.SpecificationItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 仕様書（{@link SpecificationItem}）のやりとり。どれも、仕様書を手に持っているプレイヤーからのものだけを受け付ける。 */
public final class ProfilePayloads {
    private ProfilePayloads() {
    }

    /**
     * 仕様書で乗場の操作盤（三方枠）を右クリックした（クライアント → サーバー）。copy なら、そのかごの設定を写し取る。そうでなければ、
     * 仕様書の、設置したあとも変えられる部分をかごに反映する。
     */
    public record UseOnDoor(int doorId, boolean copy) implements CustomPacketPayload {
        public static final Type<UseOnDoor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "profile_use_on_door"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseOnDoor> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UseOnDoor::doorId,
                ByteBufCodecs.BOOL, UseOnDoor::copy,
                UseOnDoor::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(UseOnDoor payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SpecificationItem)) {
                return;
            }
            ControllerBlockEntity controller = RenewalPayloads.controllerFor(player, payload.doorId(), false);
            if (controller == null) {
                player.displayClientMessage(Component.translatable("message.realelevator.wrench.no_controller"), true);
                return;
            }
            if (payload.copy()) {
                SpecificationItem.copyFrom(stack, controller.renewalTag(), player);
                return;
            }
            CompoundTag profile = SpecificationItem.profile(stack);
            if (!ElevatorProfile.isPresent(profile)) {
                player.displayClientMessage(Component.translatable("message.realelevator.profile.blank"), true);
                return;
            }
            if (!RenewalPayloads.checkManage(player, controller)) {
                return;
            }
            controller.applyRenewal(ElevatorProfile.mergeRenewal(controller.renewalTag(), profile));
            player.displayClientMessage(Component.translatable("message.realelevator.profile.applied_car"), true);
        }
    }

    /** 手元のファイルのプロファイルを、手に持っている仕様書に書く（クライアント → サーバー）。 */
    public record Write(InteractionHand hand, CompoundTag profile) implements CustomPacketPayload {
        public static final Type<Write> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "profile_write"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Write> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, w -> w.hand() == InteractionHand.OFF_HAND,
                ByteBufCodecs.COMPOUND_TAG, Write::profile,
                (offHand, profile) -> new Write(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, profile));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Write payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (stack.getItem() instanceof SpecificationItem && ElevatorProfile.isPresent(payload.profile())) {
                SpecificationItem.write(stack, payload.profile());
                player.displayClientMessage(Component.translatable("message.realelevator.profile.written"), true);
            }
        }
    }
}
