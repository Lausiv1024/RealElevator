package net.lausiv1024.realelevator.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
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
 * かご・扉・ボタンのサーバー側の動作確認。runGameTestServer で実行する。
 *
 * <p>空の 7×14×7 の空間に、かご（P-11、南向き）と各階の乗場の扉を置く。
 * 2階床のときは下階と7ブロック上の上階、3階床のときは4ブロックおき。
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

    private record Elevator(ElevatorCarEntity car, List<LandingDoorEntity> landingDoors) {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void carLandsExactlyOnUpperFloor(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_LOW, 0, RISE).car();
        int upperY = helper.absolutePos(LOWER).getY() + RISE;
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "the call should be registered");
        helper.assertTrue(car.pressCarCall(1) == PressResult.ALREADY_REGISTERED, "the same call should not be registered twice");
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
        ElevatorCarEntity car = spawn(helper, up ? 0 : 1, speed, 0, RISE).car();
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, LOWER);
        stand.moveTo(car.getX(), car.getY(), car.getZ());

        // 防具立てが床に落ち着いてから発車する
        helper.runAtTickTime(5, () -> helper.assertTrue(car.pressCarCall(up ? 1 : 0) == PressResult.REGISTERED, "not registered"));
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
        Elevator elevator = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE);
        ElevatorCarEntity car = elevator.car();
        LandingDoorEntity lowerDoor = elevator.landingDoors().get(0);
        LandingDoorEntity upperDoor = elevator.landingDoors().get(1);
        AtomicBoolean sawOpen = new AtomicBoolean();
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "not registered");
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            double carDoor = car.doorFractionAt(now);
            if (car.isMoving() && carDoor > 0) {
                helper.fail("the door opened while moving");
            }
            if (lowerDoor.fractionAt(now) > 0) {
                helper.fail("the landing door of the other floor opened");
            }
            if (Math.abs(upperDoor.fractionAt(now) - carDoor) > 1e-9) {
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
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        int upperY = helper.absolutePos(LOWER).getY() + RISE;
        helper.assertTrue(car.pressHallCall(0, TravelDirection.UP) == PressResult.SERVED_HERE, "the door should open at the current floor");
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "not registered");
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
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        helper.assertTrue(car.pressCarCall(0) == PressResult.SERVED_HERE, "the door should open");

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

    /**
     * 乗合全自動：1階から、2階の下り呼びと3階のかご呼びを登録すると、上昇中は2階を通過して3階に止まり、
     * 反転して2階に止まる。応えた呼びの応答灯は消える。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void collectiveControlServesCallsInTravelOrder(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.HIGH, 0, 4, 8).car();
        helper.assertTrue(car.pressHallCall(1, TravelDirection.DOWN) == PressResult.REGISTERED, "hall call not registered");
        helper.assertTrue(car.pressCarCall(2) == PressResult.REGISTERED, "car call not registered");
        helper.assertTrue(car.isHallCallLit(1, TravelDirection.DOWN) && car.isCarCallLit(2), "lamps should be lit");

        List<Integer> stops = new ArrayList<>();
        AtomicBoolean wasClosed = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            boolean closed = car.doorState() == DoorState.CLOSED;
            if (wasClosed.get() && !closed) {
                stops.add(car.floorIndexAt(car.getY()));
            }
            wasClosed.set(closed);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stops.equals(List.of(2, 1)), "stops so far: " + stops);
            helper.assertTrue(!car.isHallCallLit(1, TravelDirection.DOWN) && !car.isCarCallLit(2), "lamps should be off");
        });
    }

    /** 走行中に途中の階で呼ばれても、減速が間に合えばその階に止まる。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void stopsOnTheWayWhenThereIsRoomToBrake(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        car.pressCarCall(2);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(car.isMoving(), "should have departed");
            helper.assertTrue(car.pressHallCall(1, TravelDirection.UP) == PressResult.REGISTERED, "not registered");
        });
        List<Integer> stops = recordStops(helper, car);
        helper.succeedWhen(() -> helper.assertTrue(stops.equals(List.of(1, 2)), "stops so far: " + stops));
    }

    /** 減速が間に合わない位置で呼ばれたら通過し、あとで戻って応える。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void passesAFloorWhenItIsTooLateToBrake(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        double secondFloorY = car.floors()[1];
        car.pressCarCall(2);
        AtomicBoolean called = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!called.get() && car.getY() >= secondFloorY - 0.5) {
                called.set(true);
                helper.assertTrue(car.pressHallCall(1, TravelDirection.UP) == PressResult.REGISTERED, "not registered");
            }
        });
        List<Integer> stops = recordStops(helper, car);
        helper.succeedWhen(() -> helper.assertTrue(stops.equals(List.of(2, 1)), "stops so far: " + stops));
    }

    /** 戸が開き始めるたびに、その階を記録する。 */
    private static List<Integer> recordStops(GameTestHelper helper, ElevatorCarEntity car) {
        List<Integer> stops = new ArrayList<>();
        AtomicBoolean wasClosed = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            boolean closed = car.doorState() == DoorState.CLOSED;
            if (wasClosed.get() && !closed) {
                stops.add(car.floorIndexAt(car.getY()));
            }
            wasClosed.set(closed);
        });
        return stops;
    }

    /** 操作盤と乗場のボタンを押すと、呼びが登録されて応答灯がつく。開ボタンで戸が開く。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void buttonsRegisterCalls(GameTestHelper helper) {
        Elevator elevator = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE);
        ElevatorCarEntity car = elevator.car();
        LandingDoorEntity upperDoor = elevator.landingDoors().get(1);

        int secondFloor = indexOf(car.buttons(), ButtonLayout.Kind.CAR_CALL, 1);
        int down = indexOf(upperDoor.buttons(), ButtonLayout.Kind.HALL_DOWN, 1);
        helper.assertTrue(upperDoor.buttons().size() == 1, "the top floor should have only the down button");

        upperDoor.pressButton(down);
        helper.assertTrue(upperDoor.isButtonLit(down), "the hall button should light up");
        car.pressButton(secondFloor);
        helper.assertTrue(car.isButtonLit(secondFloor), "the car button should light up");

        // 発車前に開ボタンを押すと、1階で戸が開き直す
        car.pressButton(indexOf(car.buttons(), ButtonLayout.Kind.OPEN, -1));
        helper.assertTrue(car.doorState() == DoorState.OPENING, "the open button should open the door");
        helper.succeed();
    }

    private static int indexOf(List<ButtonLayout.Button> buttons, ButtonLayout.Kind kind, int floor) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).kind() == kind && buttons.get(i).floor() == floor) {
                return i;
            }
        }
        throw new IllegalStateException("no button " + kind + " " + floor);
    }

    /**
     * @param startFloor かごを置く階
     * @param rises      各階の、LOWER からの高さ
     */
    private static Elevator spawn(GameTestHelper helper, int startFloor, SpeedSpec speed, int... rises) {
        BlockPos lower = helper.absolutePos(LOWER);
        int[] floors = new int[rises.length];
        for (int i = 0; i < rises.length; i++) {
            floors[i] = lower.getY() + rises[i];
        }
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        if (car == null) {
            throw new IllegalStateException("failed to create the car");
        }
        car.moveTo(lower.getX() + 0.5, floors[startFloor], lower.getZ() + 0.5, 0, 0);
        car.setup(MODEL, Direction.SOUTH, floors, speed);
        helper.getLevel().addFreshEntity(car);

        List<LandingDoorEntity> doors = new ArrayList<>();
        for (int floorY : floors) {
            LandingDoorEntity door = ModEntities.LANDING_DOOR.get().create(helper.getLevel());
            if (door == null) {
                throw new IllegalStateException("failed to create the landing door");
            }
            door.moveTo(car.getX(), floorY, car.getZ(), 0, 0);
            door.setup(car);
            helper.getLevel().addFreshEntity(door);
            doors.add(door);
        }
        return new Elevator(car, doors);
    }
}
