package net.lausiv1024.realelevator.network;

import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorGroups;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 保守用レンチのメニュー（リニューアルと解体）のやりとり。メニューは乗場の扉（の三方枠）から開くので、どれも乗場の扉の
 * エンティティで指し、そのかごの頂部の制御盤を操作する。サーバーでは、レンチを持って乗場の扉の近くにいるプレイヤーからのものだけを受け付ける。
 * 操作できるのは、エレベーターの設置者、許可されたプレイヤー、OPだけ（{@link ControllerBlockEntity#canManage(net.minecraft.world.entity.player.Player)}）。
 */
public final class RenewalPayloads {
    /** 乗場の扉から操作できる距離。手の届く距離に足す余裕。 */
    private static final double REACH_MARGIN = 2.0;

    private RenewalPayloads() {
    }

    /** メニューを開きたい（クライアント → サーバー）。 */
    public record Open(int doorId) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::doorId,
                Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ControllerBlockEntity controller = controllerFor(player, payload.doorId(), true);
            if (controller == null) {
                player.displayClientMessage(Component.translatable("message.realelevator.wrench.no_controller"), true);
                return;
            }
            if (!checkManage(player, controller)) {
                return;
            }
            CompoundTag settings = controller.renewalTag();
            settings.put("Access", accessTag(controller, player));
            // 群管理の群の台数（群に入っていなければ0）
            if (player.level().getEntity(payload.doorId()) instanceof LandingDoorEntity door && door.car() != null
                    && door.car().groupId().isPresent()) {
                settings.putInt("GroupSize", ElevatorGroups.members(door.car()).size());
            }
            PacketDistributor.sendToPlayer(player, new Show(payload.doorId(), settings));
        }
    }

    /** メニューを開く（サーバー → クライアント）。設定と停止階を渡す。 */
    public record Show(int doorId, CompoundTag settings) implements CustomPacketPayload {
        public static final Type<Show> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_show"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Show> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Show::doorId,
                ByteBufCodecs.COMPOUND_TAG, Show::settings,
                Show::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** リニューアルの設定を反映する（クライアント → サーバー）。 */
    public record Apply(int doorId, CompoundTag settings) implements CustomPacketPayload {
        public static final Type<Apply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_apply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Apply> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Apply::doorId,
                ByteBufCodecs.COMPOUND_TAG, Apply::settings,
                Apply::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Apply payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                // 画面を開いたあとは、レンチを持ち替えていても受け付ける
                ControllerBlockEntity controller = controllerFor(player, payload.doorId(), false);
                if (controller != null && checkManage(player, controller)) {
                    controller.applyRenewal(payload.settings());
                }
            }
        }
    }

    /**
     * 群管理の群に入れる・群から外す（クライアント → サーバー）。入れるときは、しゃがみながらレンチで選んでおいたエレベーター
     * （レンチに覚えたもの）と同じ群にする。
     */
    public record Group(int doorId, boolean join) implements CustomPacketPayload {
        public static final Type<Group> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_group"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Group> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Group::doorId,
                ByteBufCodecs.BOOL, Group::join,
                Group::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Group payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ControllerBlockEntity controller = controllerFor(player, payload.doorId(), false);
            if (controller == null || !checkManage(player, controller)
                    || !(player.level().getEntity(payload.doorId()) instanceof LandingDoorEntity door) || door.car() == null) {
                return;
            }
            ElevatorCarEntity car = door.car();
            if (!payload.join()) {
                player.displayClientMessage(ElevatorGroups.leave(car), false);
                return;
            }
            UUID selectedId = player.getMainHandItem().get(ModDataComponents.SELECTED_CAR.get());
            ElevatorCarEntity selected = selectedId != null ? ElevatorTracker.findCar(player.level(), selectedId) : null;
            if (selected == null) {
                player.displayClientMessage(Component.translatable("message.realelevator.group.select_first"), false);
                return;
            }
            if (!ControllerBlockEntity.canManage(player, selected)) {
                player.displayClientMessage(Component.translatable("message.realelevator.access.denied_selected"), true);
                return;
            }
            player.displayClientMessage(ElevatorGroups.join(selected, car), false);
        }
    }

    /** 解体する（クライアント → サーバー）。 */
    public record Dismantle(int doorId) implements CustomPacketPayload {
        public static final Type<Dismantle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_dismantle"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Dismantle> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Dismantle::doorId,
                Dismantle::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Dismantle payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                ControllerBlockEntity controller = controllerFor(player, payload.doorId(), false);
                if (controller != null && checkManage(player, controller)) {
                    controller.dismantle(player);
                    player.displayClientMessage(Component.translatable("message.realelevator.wrench.dismantled"), false);
                }
            }
        }
    }

    /** 許可するプレイヤーを追加、削除する（クライアント → サーバー）。追加できるのはオンラインのプレイヤーだけ。 */
    public record AccessEdit(int doorId, boolean add, String name) implements CustomPacketPayload {
        public static final Type<AccessEdit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_access_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AccessEdit> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AccessEdit::doorId,
                ByteBufCodecs.BOOL, AccessEdit::add,
                ByteBufCodecs.stringUtf8(16), AccessEdit::name,
                AccessEdit::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(AccessEdit payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ControllerBlockEntity controller = controllerFor(player, payload.doorId(), false);
            if (controller == null || !controller.canEditAccess(player)) {
                return;
            }
            player.displayClientMessage(edit(controller, player, payload), true);
            controller.setChanged();
            PacketDistributor.sendToPlayer(player, new AccessList(payload.doorId(), accessTag(controller, player)));
        }

        private static Component edit(ControllerBlockEntity controller, ServerPlayer player, AccessEdit payload) {
            if (!payload.add()) {
                return Component.translatable(controller.access().revoke(payload.name())
                        ? "message.realelevator.access.removed" : "message.realelevator.access.not_listed", payload.name());
            }
            ServerPlayer target = player.server.getPlayerList().getPlayerByName(payload.name());
            if (target == null) {
                return Component.translatable("message.realelevator.access.not_online", payload.name());
            }
            String name = target.getGameProfile().getName();
            return Component.translatable(controller.access().allow(target.getUUID(), name)
                    ? "message.realelevator.access.added" : "message.realelevator.access.cannot_add", name);
        }
    }

    /** 管理できるプレイヤーの一覧を送る（サーバー → クライアント）。 */
    public record AccessList(int doorId, CompoundTag access) implements CustomPacketPayload {
        public static final Type<AccessList> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "renewal_access_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AccessList> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AccessList::doorId,
                ByteBufCodecs.COMPOUND_TAG, AccessList::access,
                AccessList::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 管理できるプレイヤーの一覧（画面へ渡す形）。 */
    private static CompoundTag accessTag(ControllerBlockEntity controller, ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putString("OwnerName", controller.access().ownerName());
        tag.putBoolean("CanEdit", controller.canEditAccess(player));
        ListTag allowed = new ListTag();
        controller.access().allowed().values().forEach(name -> allowed.add(StringTag.valueOf(name)));
        tag.put("Allowed", allowed);
        return tag;
    }

    /** player がこのエレベーターを管理できるか。できなければ、設置者の名前を添えて知らせる。 */
    static boolean checkManage(ServerPlayer player, ControllerBlockEntity controller) {
        if (controller.canManage(player)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.realelevator.access.denied", controller.access().ownerName()), true);
        return false;
    }

    /**
     * プレイヤーが操作してよい、乗場の扉 doorId のかごの頂部の制御盤。
     *
     * @param needWrench レンチを手に持っている必要があるか
     */
    @Nullable
    static ControllerBlockEntity controllerFor(ServerPlayer player, int doorId, boolean needWrench) {
        if (player.isSpectator() || (needWrench && !player.getMainHandItem().is(Tags.Items.TOOLS_WRENCH))) {
            return null;
        }
        Entity entity = player.level().getEntity(doorId);
        if (!(entity instanceof LandingDoorEntity door)) {
            return null;
        }
        double reach = player.blockInteractionRange() + REACH_MARGIN;
        if (door.getBoundingBox().distanceToSqr(player.getEyePosition()) > reach * reach) {
            return null;
        }
        ElevatorCarEntity car = door.car();
        BlockPos pos = car != null ? car.controllerPos() : null;
        if (pos == null || !player.level().isLoaded(pos)) {
            return null;
        }
        return player.level().getBlockEntity(pos) instanceof ControllerBlockEntity controller ? controller : null;
    }
}
