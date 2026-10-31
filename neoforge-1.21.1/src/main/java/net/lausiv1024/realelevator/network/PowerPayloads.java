package net.lausiv1024.realelevator.network;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 運転切替盤をつなぐエレベーターを、保守用レンチで選ぶやりとり。 */
public final class PowerPayloads {
    /** 乗場の扉から操作できる距離。手の届く距離に足す余裕。 */
    private static final double REACH_MARGIN = 2.0;

    private PowerPayloads() {
    }

    /** しゃがみながらレンチで乗場の扉 doorId を右クリックし、そのエレベーターを選んだ（クライアント → サーバー）。 */
    public record Select(int doorId) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "power_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Select::doorId,
                Select::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Select payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            ItemStack wrench = player.getMainHandItem();
            Entity entity = player.level().getEntity(payload.doorId());
            if (!wrench.is(Tags.Items.TOOLS_WRENCH) || !(entity instanceof LandingDoorEntity door)) {
                return;
            }
            double reach = player.blockInteractionRange() + REACH_MARGIN;
            ElevatorCarEntity car = door.car();
            if (car == null || door.getBoundingBox().distanceToSqr(player.getEyePosition()) > reach * reach) {
                return;
            }
            if (!ControllerBlockEntity.canManage(player, car)) {
                player.displayClientMessage(Component.translatable("message.realelevator.access.denied_selected"), true);
                return;
            }
            wrench.set(ModDataComponents.SELECTED_CAR.get(), car.getUUID());
            player.displayClientMessage(Component.translatable("message.realelevator.power.selected"), true);
        }
    }
}
