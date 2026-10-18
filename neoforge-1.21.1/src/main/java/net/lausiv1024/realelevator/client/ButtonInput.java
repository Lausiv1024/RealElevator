package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.network.PressButtonPayload;
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
 *
 * <p>レンチ（{@code c:tools/wrench} タグのアイテム）を持って乗場の操作盤のあたり（三方枠）を右クリックしたときは、
 * ボタンを押さずに保守用レンチのメニューを開く。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
public final class ButtonInput {
    /**
     * ボタンを押したあと、使用キーを押したままか。押したままのあいだの繰り返し（4tickごと）では押さない。
     * 押しっぱなしで行先ボタンの2度押しキャンセルにならないようにするため。
     */
    private static boolean useHeld;

    private ButtonInput() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (useHeld && !Minecraft.getInstance().options.keyUse.isDown()) {
            useHeld = false;
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
        if (player.getMainHandItem().is(Tags.Items.TOOLS_WRENCH)) {
            LandingDoorEntity door = findLandingDoor(minecraft.level, player);
            if (door != null) {
                event.setCanceled(true);
                event.setSwingHand(true);
                PacketDistributor.sendToServer(new RenewalPayloads.Open(door.getId()));
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
        if (useHeld) {
            // 押したままの繰り返し
            event.setSwingHand(false);
            return;
        }
        useHeld = true;
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new PressButtonPayload(target.host().getId(), target.index()));
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
                    if (point.isPresent()) {
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
