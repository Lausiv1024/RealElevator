package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.lausiv1024.realelevator.entity.CarCollisions;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.item.SpecificationItem;
import net.lausiv1024.realelevator.network.PowerPayloads;
import net.lausiv1024.realelevator.network.PressButtonPayload;
import net.lausiv1024.realelevator.network.ProfilePayloads;
import net.lausiv1024.realelevator.network.RenewalPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 右クリックでボタンを押す。視線の先をボタン1つずつの箱でレイキャストし、ブロックより手前にあれば押す。
 * かごの壁や戸などにさえぎられたボタン（戸の窓越しに見えるかごのボタンなど）は押せない。
 *
 * <p>レンチ（{@code c:tools/wrench} タグのアイテム）を持って乗場の操作盤のあたり（三方枠）を右クリックしたときは、
 * ボタンを押さずに保守用レンチのメニューを開く。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
public final class ButtonInput {
    /**
     * ボタンを押したあと、使用キーを押したままか。押したままのあいだの繰り返し（4tickごと）では押さない。
     * 押しっぱなしで行先ボタンの2度押しキャンセルにならないようにするため。開ボタンだけは、押したままのあいだ繰り返し押し、
     * 戸を開けたままにする（離してから開放待機の時間がたつと閉まる）。
     */
    private static boolean useHeld;
    /** 押したままのボタン。離したときに知らせる（クリック音を鳴らす）。 */
    @Nullable
    private static Target pressed;

    private ButtonInput() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (useHeld && !Minecraft.getInstance().options.keyUse.isDown()) {
            useHeld = false;
            if (pressed != null && Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new PressButtonPayload(pressed.host().getId(), pressed.index(), PressButtonPayload.Action.RELEASE));
            }
            pressed = null;
        }
    }

    private record Target(Entity host, int index, double distanceSqr) {
    }

    @SubscribeEvent
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isUseItem() || player == null || player.isSpectator() || minecraft.level == null) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof SpecificationItem) {
            LandingDoorEntity door = findLandingDoor(minecraft.level, player);
            if (door != null) {
                event.setCanceled(true);
                event.setSwingHand(true);
                // しゃがんでいれば写し取り、そうでなければ反映
                PacketDistributor.sendToServer(new ProfilePayloads.UseOnDoor(door.getId(), player.isShiftKeyDown()));
            }
            return;
        }
        if (player.getMainHandItem().is(Tags.Items.TOOLS_WRENCH)) {
            LandingDoorEntity door = findLandingDoor(minecraft.level, player);
            if (door != null) {
                event.setCanceled(true);
                event.setSwingHand(true);
                // しゃがんでいれば、運転切替盤をつなぐエレベーターに選ぶ。そうでなければリニューアルと解体のメニュー
                PacketDistributor.sendToServer(player.isShiftKeyDown()
                        ? new PowerPayloads.Select(door.getId()) : new RenewalPayloads.Open(door.getId()));
            }
            return;
        }
        Target target = findTarget(minecraft.level, player);
        if (target == null) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS
                && hit.getLocation().distanceToSqr(player.getEyePosition()) < target.distanceSqr()) {
            return;
        }
        event.setCanceled(true);
        boolean openButton = target.host() instanceof ButtonHost host
                && host.buttons().get(target.index()).kind() == ButtonLayout.Kind.OPEN;
        if (useHeld) {
            // 押したままの繰り返し。開ボタンだけ、同じボタンを押し続けていれば音を鳴らさずに押し直す
            event.setSwingHand(false);
            if (openButton && pressed != null && pressed.host() == target.host() && pressed.index() == target.index()) {
                PacketDistributor.sendToServer(new PressButtonPayload(target.host().getId(), target.index(), PressButtonPayload.Action.REPEAT));
            }
            return;
        }
        useHeld = true;
        pressed = target;
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new PressButtonPayload(target.host().getId(), target.index(), PressButtonPayload.Action.PRESS));
    }

    /** 視線の先にある乗場の扉（三方枠を含む外形）。ブロックのほうが手前なら null。 */
    @Nullable
    private static LandingDoorEntity findLandingDoor(Level level, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        HitResult hit = Minecraft.getInstance().hitResult;
        double blockDistanceSqr = hit != null && hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().distanceToSqr(eye) : Double.MAX_VALUE;
        LandingDoorEntity best = null;
        double bestSqr = blockDistanceSqr;
        for (LandingDoorEntity door : ElevatorTracker.landingDoors(level)) {
            if (door.isRemoved()) {
                continue;
            }
            Optional<Vec3> point = door.getBoundingBox().clip(eye, end);
            // 外形の中から見ているときは、目の位置で当たっているとみなす
            double distanceSqr = door.getBoundingBox().contains(eye) ? 0 : point.map(p -> p.distanceToSqr(eye)).orElse(Double.MAX_VALUE);
            if (distanceSqr <= bestSqr && distanceSqr < Double.MAX_VALUE) {
                best = door;
                bestSqr = distanceSqr;
            }
        }
        return best;
    }

    @Nullable
    private static Target findTarget(Level level, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        double reach = player.blockInteractionRange();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        AABB area = new AABB(eye, end).inflate(1.0);

        Target best = null;
        for (List<? extends Entity> hosts : List.of(ElevatorTracker.cars(level), ElevatorTracker.landingDoors(level))) {
            for (Entity entity : hosts) {
                if (!(entity instanceof ButtonHost host) || entity.isRemoved()) {
                    continue;
                }
                for (int i = 0; i < host.buttons().size(); i++) {
                    AABB box = host.buttonBox(i);
                    if (!box.intersects(area)) {
                        continue;
                    }
                    Optional<Vec3> point = box.clip(eye, end);
                    if (point.isPresent() && !CarCollisions.isObstructed(level, eye, point.get())) {
                        double distanceSqr = point.get().distanceToSqr(eye);
                        if (best == null || distanceSqr < best.distanceSqr()) {
                            best = new Target(entity, i, distanceSqr);
                        }
                    }
                }
            }
        }
        return best;
    }
}
