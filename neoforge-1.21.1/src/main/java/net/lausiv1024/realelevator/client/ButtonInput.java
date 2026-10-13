package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.network.PressButtonPayload;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 右クリックでボタンを押す。視線の先をボタン1つずつの箱でレイキャストし、ブロックより手前にあれば押す。
 */
@EventBusSubscriber(modid = RealElevator.MODID, value = Dist.CLIENT)
public final class ButtonInput {
    private ButtonInput() {
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
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new PressButtonPayload(target.host().getId(), target.index()));
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
