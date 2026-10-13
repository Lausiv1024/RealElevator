package net.lausiv1024.realelevator.gametest;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * かごのサーバー側の動作確認。runGameTestServer で実行する。
 *
 * <p>空の 7×14×7 の空間に、下階と7ブロック上の上階を持つかごを置く。
 */
@GameTestHolder(RealElevator.MODID)
@PrefixGameTestTemplate(false)
public class ElevatorCarGameTests {
    private static final String TEMPLATE = "empty_shaft";
    private static final BlockPos LOWER = new BlockPos(3, 2, 3);
    private static final int RISE = 7;
    /** バニラの衝突判定は面の手前にわずかなすき間を残すので、その分を許す。 */
    private static final double EPS = 1e-3;

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void carLandsExactlyOnUpperFloor(GameTestHelper helper) {
        ElevatorCarEntity car = spawnCar(helper, LOWER, SpeedSpec.NORMAL_LOW);
        int upperY = helper.absolutePos(LOWER).getY() + RISE;
        helper.assertTrue(car.departTo(1), "the car should depart");
        helper.assertTrue(!car.departTo(0), "a moving car should not accept another departure");
        helper.succeedWhen(() -> {
            helper.assertTrue(!car.isMoving(), "still moving");
            helper.assertTrue(car.getY() == upperY, "landed at " + car.getY() + " instead of " + upperY);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void standingEntityRidesUp(GameTestHelper helper) {
        ridesTo(helper, SpeedSpec.NORMAL_HIGH, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void standingEntityRidesDown(GameTestHelper helper) {
        ridesTo(helper, SpeedSpec.NORMAL_HIGH, false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void standingEntityRidesUltraHighSpeed(GameTestHelper helper) {
        ridesTo(helper, SpeedSpec.ULTRA_HIGH, true);
    }

    /** かごの床に立たせた防具立てが、走行中も床から離れず、到着後も床の上にいること。 */
    private static void ridesTo(GameTestHelper helper, SpeedSpec speed, boolean up) {
        BlockPos start = up ? LOWER : LOWER.above(RISE);
        ElevatorCarEntity car = spawnCar(helper, start, speed);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, start);
        stand.moveTo(car.getX(), car.getY(), car.getZ());

        // 防具立てが床に落ち着いてから発車する
        helper.runAtTickTime(5, () -> helper.assertTrue(car.departTo(up ? 1 : 0), "the car should depart"));
        helper.onEachTick(() -> {
            double gap = stand.getY() - car.getY();
            if (helper.getTick() > 2 && Math.abs(gap) > 0.01) {
                helper.fail("left the floor by " + gap + " at tick " + helper.getTick(), stand);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 10 && !car.isMoving(), "still moving");
            helper.assertTrue(Math.abs(stand.getY() - car.getY()) < EPS, "not on the floor: " + stand.getY() + " / " + car.getY());
            helper.assertTrue(stand.onGround(), "not on ground");
        });
    }

    private static ElevatorCarEntity spawnCar(GameTestHelper helper, BlockPos relative, SpeedSpec speed) {
        BlockPos lower = helper.absolutePos(LOWER);
        BlockPos pos = helper.absolutePos(relative);
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        if (car == null) {
            throw new IllegalStateException("failed to create the car");
        }
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        car.setup(CarModel.P11, Direction.SOUTH, new int[] {lower.getY(), lower.getY() + RISE}, speed);
        helper.getLevel().addFreshEntity(car);
        return car;
    }
}
