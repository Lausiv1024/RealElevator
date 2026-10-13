package net.lausiv1024.realelevator.entity;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * かごに乗っているエンティティを、かごと一緒に動かす。
 *
 * <p>プレイヤーはクライアントが位置を決めるので、クライアントでローカルプレイヤーだけを動かす。
 * それ以外のエンティティはサーバーで動かす。どちらも、そのエンティティの移動処理の直前に、
 * このtickのかごの移動量をそのまま加える。
 */
@EventBusSubscriber(modid = RealElevator.MODID)
public final class CarTransport {
    private CarTransport() {
    }

    @SubscribeEvent
    static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (ElevatorTracker.isElevatorPart(entity) || entity.isSpectator() || entity.isPassenger()) {
            return;
        }
        Level level = entity.level();
        boolean movedHere = level.isClientSide ? entity.isControlledByLocalInstance() : !(entity instanceof Player);
        if (!movedHere) {
            return;
        }
        List<ElevatorCarEntity> cars = ElevatorTracker.cars(level);
        if (cars.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        for (ElevatorCarEntity car : cars) {
            if (!car.isMoving() || car.isRemoved()) {
                continue;
            }
            double previous = car.yAt(now - 1);
            double dy = car.yAt(now) - previous;
            if (dy != 0 && car.carries(entity, previous)) {
                entity.setPos(entity.getX(), entity.getY() + dy, entity.getZ());
                return;
            }
        }
    }

    /**
     * かごの下は昇降路の空間なので、バニラの「足元にブロックがない」判定で浮遊とみなされ、
     * 飛行が許可されていないサーバーでキックされてしまう。かごの中にいる間は浮遊時間を数えない。
     */
    @SubscribeEvent
    static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (ElevatorCarEntity car : ElevatorTracker.cars(player.level())) {
            if (!car.isRemoved() && car.carries(player, car.getY())) {
                player.connection.aboveGroundTickCount = 0;
                return;
            }
        }
    }
}
