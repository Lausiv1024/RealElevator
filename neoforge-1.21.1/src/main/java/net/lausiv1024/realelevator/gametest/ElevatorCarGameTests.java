package net.lausiv1024.realelevator.gametest;

import java.util.concurrent.atomic.AtomicBoolean;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity.CallResult;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
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
 * かごと扉のサーバー側の動作確認。runGameTestServer で実行する。
 *
 * <p>空の 7×14×7 の空間に、下階と7ブロック上の上階を持つかご（P-11、南向き）と乗場の扉を置く。
 */
@GameTestHolder(RealElevator.MODID)
@PrefixGameTestTemplate(false)
public class ElevatorCarGameTests {
    private static final String TEMPLATE = "empty_shaft";
    private static final BlockPos LOWER = new BlockPos(3, 2, 3);
    private static final int RISE = 7;
    private static final CarModel MODEL = CarModel.P11;
    /** バニラの衝突判定は面の手前にわずかなすき間を残すので、その分を許す。 */
    private static final double EPS = 1e-3;

    private record Elevator(ElevatorCarEntity car, LandingDoorEntity lowerDoor, LandingDoorEntity upperDoor) {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void carLandsExactlyOnUpperFloor(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, LOWER, SpeedSpec.NORMAL_LOW).car();
        int upperY = helper.absolutePos(LOWER).getY() + RISE;
        helper.assertTrue(car.call(1, CallType.CAR) == CallResult.ACCEPTED, "the call should be accepted");
        helper.assertTrue(car.call(0, CallType.CAR) == CallResult.BUSY, "a second call should wait");
        helper.succeedWhen(() -> {
            helper.assertTrue(!car.isMoving() && car.getY() == upperY, "not landed yet: " + car.getY());
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
        ElevatorCarEntity car = spawn(helper, start, speed).car();
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, start);
        stand.moveTo(car.getX(), car.getY(), car.getZ());

        // 防具立てが床に落ち着いてから発車する
        helper.runAtTickTime(5, () -> helper.assertTrue(car.call(up ? 1 : 0, CallType.CAR) == CallResult.ACCEPTED, "not accepted"));
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

    /** 着床すると戸が開き、行先階の乗場の扉だけが一緒に開く。開放待機のあと閉じる。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void doorsOpenOnArrivalTogetherWithThatLandingDoor(GameTestHelper helper) {
        Elevator elevator = spawn(helper, LOWER, SpeedSpec.NORMAL_HIGH);
        ElevatorCarEntity car = elevator.car();
        AtomicBoolean sawOpen = new AtomicBoolean();
        helper.assertTrue(car.call(1, CallType.CAR) == CallResult.ACCEPTED, "not accepted");
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            double carDoor = car.doorFractionAt(now);
            if (car.isMoving() && carDoor > 0) {
                helper.fail("the door opened while moving");
            }
            if (elevator.lowerDoor().fractionAt(now) > 0) {
                helper.fail("the landing door of the other floor opened");
            }
            if (Math.abs(elevator.upperDoor().fractionAt(now) - carDoor) > 1e-9) {
                helper.fail("the landing door does not follow the car door");
            }
            if (car.doorState() == DoorState.OPEN) {
                sawOpen.set(true);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(sawOpen.get() && car.doorState() == DoorState.CLOSED, "the door cycle is not finished"));
    }

    /** 戸が開いている間に呼びを受けても、全閉になるまで発車しない。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void departureWaitsForDoorsToClose(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, LOWER, SpeedSpec.NORMAL_HIGH).car();
        int upperY = helper.absolutePos(LOWER).getY() + RISE;
        helper.assertTrue(car.call(0, CallType.HALL) == CallResult.OPENED, "the door should open at the current floor");
        helper.assertTrue(car.call(1, CallType.CAR) == CallResult.ACCEPTED, "not accepted");
        helper.onEachTick(() -> {
            if (car.isMoving() && car.doorState() != DoorState.CLOSED) {
                helper.fail("departed with the door " + car.doorState());
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(car.getY() == upperY && !car.isMoving(), "not arrived yet"));
    }

    /** 閉まりかけの戸に物が触れると開き直し、取り除くと閉じる。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void safetyShoeReopensClosingDoors(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, LOWER, SpeedSpec.NORMAL_HIGH).car();
        helper.assertTrue(car.call(0, CallType.CAR) == CallResult.OPENED, "the door should open");

        // 戸が全開になってから、出入口の戸の位置に防具立てを置く
        double doorZ = CarGeometry.exterior(MODEL).maxZ() + DoorGeometry.PANEL_THICKNESS_PX / 16.0;
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, LOWER);
        stand.moveTo(car.getX(), car.getY() + 3, car.getZ() - 2);
        helper.runAtTickTime(45, () -> stand.moveTo(car.getX(), car.getY(), car.getZ() + doorZ));

        AtomicBoolean sawClosing = new AtomicBoolean();
        AtomicBoolean reversed = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (car.doorState() == DoorState.CLOSED && !reversed.get() && helper.getTick() > 45) {
                helper.fail("closed on the obstacle");
            }
            if (car.doorState() == DoorState.CLOSING) {
                sawClosing.set(true);
            } else if (sawClosing.get() && car.doorState() == DoorState.OPENING && !reversed.get()) {
                reversed.set(true);
                stand.discard();
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(reversed.get() && car.doorState() == DoorState.CLOSED, "not reversed and closed yet"));
    }

    private static Elevator spawn(GameTestHelper helper, BlockPos relative, SpeedSpec speed) {
        BlockPos lower = helper.absolutePos(LOWER);
        BlockPos pos = helper.absolutePos(relative);
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        if (car == null) {
            throw new IllegalStateException("failed to create the car");
        }
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        car.setup(MODEL, Direction.SOUTH, new int[] {lower.getY(), lower.getY() + RISE}, speed);
        helper.getLevel().addFreshEntity(car);
        return new Elevator(car, spawnLandingDoor(helper, car, lower.getY()), spawnLandingDoor(helper, car, lower.getY() + RISE));
    }

    private static LandingDoorEntity spawnLandingDoor(GameTestHelper helper, ElevatorCarEntity car, int floorY) {
        LandingDoorEntity door = ModEntities.LANDING_DOOR.get().create(helper.getLevel());
        if (door == null) {
            throw new IllegalStateException("failed to create the landing door");
        }
        door.moveTo(car.getX(), floorY, car.getZ(), 0, 0);
        door.setup(car);
        helper.getLevel().addFreshEntity(door);
        return door;
    }
}
