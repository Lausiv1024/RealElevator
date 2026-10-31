package net.lausiv1024.realelevator.entity;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * かごに乗っているエンティティを、かごと一緒に動かす。
 *
 * <p>プレイヤーはクライアントが位置を決めるので、クライアントでローカルプレイヤーだけを動かす。
 * それ以外のエンティティはサーバーで動かす。どちらも、そのエンティティの移動処理の直前に、
 * 前のtickからかごが実際に動いた量（{@link ElevatorCarEntity#carryDelta}）を加える。
 *
 * <p>サーバーからの位置で動くエンティティ（モブ、ボートなど）は、クライアントでは遅れて見えるので、描画のためにかごの床に合わせる。
 */
@EventBusSubscriber(modid = RealElevator.MODID)
public final class CarTransport {
    /** 浮遊時間を数えないときに見込む、サーバーにあるプレイヤーの位置の遅れ [tick]。 */
    private static final int FLOATING_LAG_TICKS = 40;

    private CarTransport() {
    }

    @SubscribeEvent
    static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (ElevatorTracker.isElevatorPart(entity) || entity.isSpectator() || entity.isPassenger()) {
            return;
        }
        Level level = entity.level();
        // プレイヤーが操縦している乗り物の位置も、プレイヤーのクライアントが決める
        boolean movedHere = level.isClientSide ? entity.isControlledByLocalInstance()
                : !(entity instanceof Player) && !(entity.getControllingPassenger() instanceof Player);
        if (!movedHere) {
            return;
        }
        List<ElevatorCarEntity> cars = ElevatorTracker.cars(level);
        if (cars.isEmpty()) {
            return;
        }

        double now = ElevatorClock.now(level);
        for (ElevatorCarEntity car : cars) {
            if (car.isRemoved()) {
                continue;
            }
            double dy = car.carryDelta(now);
            if (!car.carries(entity, car.carryFromY(now))) {
                continue;
            }
            double surface;
            if (dy != 0) {
                entity.setPos(entity.getX(), entity.getY() + dy, entity.getZ());
                // ゲーム時刻が飛んで床が1tick分より大きく動いたときは、床の中に取り残されるので押し上げる
                surface = car.sunkSurface(entity, now);
            } else {
                surface = car.restingSunkSurface(entity, now);
            }
            if (!Double.isNaN(surface)) {
                entity.setPos(entity.getX(), surface, entity.getZ());
                Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, Math.max(0, motion.y), motion.z);
            }
            return;
        }
    }

    /** サーバーからの位置で動くエンティティが地面に立っていれば、クライアントで、走っているかごの床に合わせる。 */
    @SubscribeEvent
    static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide || ElevatorTracker.isElevatorPart(entity) || entity.isSpectator() || entity.isPassenger()
                || entity.isControlledByLocalInstance() || !entity.onGround()) {
            return;
        }
        double now = ElevatorClock.now(level);
        for (ElevatorCarEntity car : ElevatorTracker.cars(level)) {
            if (car.isRemoved() || !car.isSettling(now)) {
                continue;
            }
            double surface = car.restingSurface(entity, now);
            if (!Double.isNaN(surface)) {
                entity.setPos(entity.getX(), surface, entity.getZ());
                return;
            }
        }
    }

    /**
     * かごの下は昇降路の空間なので、バニラの「足元にブロックがない」判定で浮遊とみなされ、
     * 飛行が許可されていないサーバーでキックされてしまう。かごの中にいる間は浮遊時間を数えない。
     *
     * <p>サーバーにあるプレイヤーの位置は、クライアントの時計の遅れと通信の分だけ、かごより前の時刻のものになる。
     * 上昇中はかごの床より下に見えるので、直近 {@value #FLOATING_LAG_TICKS} tick のかごの位置のどこかに乗っていればよいことにする。
     */
    @SubscribeEvent
    static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ridesLagging(player)) {
            player.connection.aboveGroundTickCount = 0;
            // ボートなどに乗っているときは、乗り物の浮遊時間でキックされる
            player.connection.aboveGroundVehicleTickCount = 0;
        }
    }

    /** サーバーにある entity の位置が、直近 {@value #FLOATING_LAG_TICKS} tick のどこかのかごの位置に乗っているか。 */
    public static boolean ridesLagging(Entity entity) {
        double now = ElevatorClock.now(entity.level());
        for (ElevatorCarEntity car : ElevatorTracker.cars(entity.level())) {
            if (car.isRemoved()) {
                continue;
            }
            double past = car.yAt(now - FLOATING_LAG_TICKS);
            double present = car.yAt(now);
            if (car.carries(entity, Math.min(past, present), Math.max(past, present))) {
                return true;
            }
        }
        return false;
    }
}
