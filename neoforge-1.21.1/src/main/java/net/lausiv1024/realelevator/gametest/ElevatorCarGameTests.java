package net.lausiv1024.realelevator.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementScript;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.OperationTiming;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.door.DoorTiming;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
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

    /**
     * 高速で上昇中に、乗っている防具立てが床の中に入り込んでも（クライアントの時刻が飛んだときと同じ状況）、
     * 下へ抜けずに床の上へ戻る。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void entitySunkIntoTheFloorIsLiftedBack(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.HIGH, 0, RISE).car();
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, LOWER);
        stand.moveTo(car.getX(), car.getY(), car.getZ());
        helper.runAtTickTime(5, () -> car.pressCarCall(1));
        AtomicBoolean pushed = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!pushed.get() && car.isMoving() && car.getY() > car.floors()[0] + 2) {
                pushed.set(true);
                stand.setPos(stand.getX(), car.getY() - 0.3, stand.getZ());
            } else if (pushed.get() && stand.getY() < car.getY() - 0.31) {
                helper.fail("fell through the floor: " + (stand.getY() - car.getY()), stand);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pushed.get() && !car.isMoving(), "not arrived yet");
            helper.assertTrue(Math.abs(stand.getY() - car.getY()) < EPS, "not on the floor: " + (stand.getY() - car.getY()));
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
        AtomicLong closedAt = new AtomicLong(-1);
        AtomicBoolean departed = new AtomicBoolean();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (car.isMoving() && car.doorState() != DoorState.CLOSED) {
                helper.fail("departed with the door " + car.doorState());
            }
            if (car.doorState() == DoorState.CLOSED && closedAt.get() < 0) {
                closedAt.set(now);
            }
            if (car.isMoving() && !departed.getAndSet(true)) {
                // 戸が閉まってから少し遅れて動き始める
                helper.assertTrue(now - closedAt.get() >= OperationTiming.START_DELAY_TICKS,
                        "departed " + (now - closedAt.get()) + " ticks after the door closed");
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
        helper.runAtTickTime(OperationTiming.START_DELAY_TICKS + 5, () -> {
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

    /** 階床表示は、走行中は進行方向の矢印と通過中の階を出し、呼びがなくなると矢印を消す。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void indicatorFollowsTheCar(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        helper.assertTrue(car.indicatorDirection() == TravelDirection.NONE, "no arrow while idle");
        car.pressCarCall(2);
        List<Integer> shown = new ArrayList<>();
        AtomicBoolean arrived = new AtomicBoolean();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            int floor = car.indicatorFloor(now);
            if (shown.isEmpty() || shown.get(shown.size() - 1) != floor) {
                shown.add(floor);
            }
            if (car.isMoving() && car.indicatorDirection() != TravelDirection.UP) {
                helper.fail("the arrow should point up while moving up");
            }
            if (!car.isMoving() && car.doorState() != DoorState.CLOSED) {
                arrived.set(true);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(arrived.get() && car.doorState() == DoorState.CLOSED, "the door cycle is not finished");
            helper.assertTrue(shown.equals(List.of(0, 1, 2)), "shown floors: " + shown);
            helper.assertTrue(car.indicatorDirection() == TravelDirection.NONE, "the arrow should disappear");
            helper.assertTrue(car.floorLabel(2).equals("3"), "labels count from 1");
        });
    }

    /** 方向の決まっていないかごが止まっている階で乗場ボタンを押すと、その時点で矢印が出る。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void hallCallAtTheStoppedFloorShowsTheArrowImmediately(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        helper.assertTrue(car.indicatorDirection() == TravelDirection.NONE, "no arrow while idle");
        helper.assertTrue(car.pressHallCall(0, TravelDirection.UP) == PressResult.SERVED_HERE, "the door should open");
        helper.assertTrue(car.indicatorDirection() == TravelDirection.UP, "the arrow should point up right away");
        helper.assertTrue(car.syncedIndicatorDirection() == TravelDirection.UP, "the direction should be sent to clients");
        helper.succeed();
    }

    /** 操作盤を両側にすると左側のボタンでも呼べ、配置を変えても登録済みの呼びは消えない。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bothPanelsWorkAndConfigChangesKeepCalls(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        car.pressCarCall(2);
        int rightOnly = car.buttons().size();
        car.setFixtures(car.fixtures().withPanelSides(FixtureConfig.PanelSides.BOTH));
        helper.assertTrue(car.buttons().size() == rightOnly * 2, "the left panel should be added");
        helper.assertTrue(car.isCarCallLit(2), "registered calls should survive a config change");

        // 左側の操作盤の2階のボタン
        int leftSecondFloor = -1;
        for (int i = rightOnly; i < car.buttons().size(); i++) {
            if (car.buttons().get(i).kind() == ButtonLayout.Kind.CAR_CALL && car.buttons().get(i).floor() == 1) {
                leftSecondFloor = i;
            }
        }
        helper.assertTrue(leftSecondFloor >= 0, "no left button for the 2nd floor");
        car.pressButton(leftSecondFloor);
        helper.assertTrue(car.isCarCallLit(1) && car.isButtonLit(leftSecondFloor) && car.isButtonLit(indexOf(car.buttons(), ButtonLayout.Kind.CAR_CALL, 1)),
                "both panels should light up");
        helper.succeed();
    }

    /**
     * ホールランタンは、到着チャイムが鳴ってから扉が閉まるまで、中間階では進行方向の段、最上階では両方の段が点く。
     * 行先でもなく、かごがいない階は点かない。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void hallLanternLightsWhileTheDoorIsOpen(GameTestHelper helper) {
        Elevator elevator = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8);
        ElevatorCarEntity car = elevator.car();
        LandingDoorEntity middle = elevator.landingDoors().get(1);
        LandingDoorEntity top = elevator.landingDoors().get(2);
        car.pressHallCall(1, TravelDirection.UP);
        AtomicBoolean sawMiddle = new AtomicBoolean();
        AtomicBoolean sawMiddleBeforeArrival = new AtomicBoolean();
        AtomicBoolean pressed = new AtomicBoolean();
        AtomicBoolean sawTop = new AtomicBoolean();
        helper.onEachTick(() -> {
            for (LandingDoorEntity door : elevator.landingDoors()) {
                boolean here = door.isCarHere() && car.doorState() != DoorState.CLOSED;
                boolean approaching = car.isMoving() && car.announcedFloor() == door.floorIndex();
                if (!here && !approaching && !door.litLanternSections().isEmpty()) {
                    helper.fail("a lantern lit on a floor the car is neither at nor arriving at", door);
                }
            }
            if (!middle.litLanternSections().isEmpty()) {
                helper.assertTrue(middle.litLanternSections().equals(Set.of(TravelDirection.UP)), "middle: " + middle.litLanternSections());
                sawMiddle.set(true);
                if (car.isMoving()) {
                    sawMiddleBeforeArrival.set(true);
                } else if (!pressed.getAndSet(true)) {
                    // 乗った人が最上階を押した
                    car.pressCarCall(2);
                }
            }
            if (!top.litLanternSections().isEmpty()) {
                helper.assertTrue(top.litLanternSections().equals(Set.of(TravelDirection.UP, TravelDirection.DOWN)), "top: " + top.litLanternSections());
                sawTop.set(true);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(sawMiddle.get() && sawMiddleBeforeArrival.get() && sawTop.get()
                && car.doorState() == DoorState.CLOSED, "not finished yet"));
    }

    /**
     * 到着チャイムは走行中、到着が確定した時点（途中階で呼ばれても止まれなくなった時点）で鳴る。
     * その後に途中階で呼ばれても止まらない。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void arrivalIsAnnouncedOnceItCannotChange(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        car.pressCarCall(2);
        AtomicBoolean announced = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!announced.get() && car.isArrivalAnnounced()) {
                announced.set(true);
                helper.assertTrue(car.isMoving(), "should be announced before arriving");
                // 発車直後（まだ止まれるうち）には鳴らない
                helper.assertTrue(car.getY() > car.floors()[0], "announced before departing");
                car.pressHallCall(1, TravelDirection.UP);
            }
        });
        List<Integer> stops = recordStops(helper, car);
        helper.succeedWhen(() -> helper.assertTrue(announced.get() && stops.equals(List.of(2, 1)), "stops so far: " + stops));
    }

    /** 6秒より長い走行では、到着チャイムは到着の6秒前に鳴る（隣の階なので発車時点で確定しているが、それより待つ）。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void longTripAnnouncesSixSecondsBeforeArrival(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_LOW, 0, RISE).car();
        car.pressCarCall(1);
        int upperY = car.floors()[1];
        AtomicLong departedAt = new AtomicLong(-1);
        AtomicLong announcedAt = new AtomicLong(-1);
        AtomicLong arrivedAt = new AtomicLong(-1);
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (departedAt.get() < 0 && car.isMoving()) {
                departedAt.set(now);
            }
            if (announcedAt.get() < 0 && car.isArrivalAnnounced()) {
                announcedAt.set(now);
            }
            if (arrivedAt.get() < 0 && car.getY() == upperY) {
                arrivedAt.set(now);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(arrivedAt.get() >= 0 && announcedAt.get() >= 0, "not arrived yet");
            long lead = arrivedAt.get() - announcedAt.get();
            helper.assertTrue(announcedAt.get() > departedAt.get(), "should wait after departing");
            helper.assertTrue(lead >= 119 && lead <= 121, "announced " + lead + " ticks before arriving");
        });
    }

    /** 自動で閉めるときは、戸閉アナウンスの時間だけ待ってから閉め始める。設定で切ると待たない。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void automaticClosingWaitsForTheCloseAnnouncement(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        ElevatorCarEntity quiet = spawnCarOnly(helper, helper.absolutePos(LOWER).getX() + 3.5);
        quiet.setAnnouncements(quiet.announcements().withDoorClose(false));
        car.pressCarCall(0);
        quiet.pressCarCall(0);
        AtomicLong openedAt = new AtomicLong(-1);
        AtomicLong closingAt = new AtomicLong(-1);
        AtomicLong quietClosingAt = new AtomicLong(-1);
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (openedAt.get() < 0 && car.doorState() == DoorState.OPEN) {
                openedAt.set(now);
            }
            if (closingAt.get() < 0 && car.doorState() == DoorState.CLOSING) {
                closingAt.set(now);
            }
            if (quietClosingAt.get() < 0 && quiet.doorState() == DoorState.CLOSING) {
                quietClosingAt.set(now);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(closingAt.get() >= 0 && quietClosingAt.get() >= 0, "not closing yet");
            long dwell = DoorTiming.STANDARD.carDwellTicks();
            long waited = closingAt.get() - openedAt.get();
            helper.assertTrue(waited == dwell + AnnouncementScript.DOOR_CLOSE_WAIT_TICKS, "closed after " + waited + " ticks");
            helper.assertTrue(quietClosingAt.get() - openedAt.get() == dwell, "without the announcement it should not wait");
        });
    }

    /** 閉ボタンでは、戸閉アナウンスを待たずにすぐ閉め始める。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void closeButtonDoesNotWaitForTheAnnouncement(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        car.pressHallCall(0, TravelDirection.UP);
        AtomicBoolean pressed = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!pressed.get() && car.doorState() == DoorState.OPEN) {
                pressed.set(true);
                helper.assertTrue(car.closeDoor(), "the close button should work");
                helper.assertTrue(car.doorState() == DoorState.CLOSING, "should start closing right away");
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(pressed.get() && car.doorState() == DoorState.CLOSED, "not closed yet"));
    }

    /** 乗場の扉なしで、下階にかごだけを置く（扉どうしが重ならないよう、x をずらせる）。 */
    private static ElevatorCarEntity spawnCarOnly(GameTestHelper helper, double x) {
        BlockPos lower = helper.absolutePos(LOWER);
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        if (car == null) {
            throw new IllegalStateException("failed to create the car");
        }
        car.moveTo(x, lower.getY(), lower.getZ() + 0.5, 0, 0);
        car.setup(MODEL, Direction.SOUTH, new int[] {lower.getY(), lower.getY() + RISE}, SpeedSpec.NORMAL_HIGH);
        helper.getLevel().addFreshEntity(car);
        return car;
    }

    /** 到着すると、戸を開き始めるときに「◯階です。」、開ききったときに行先方向を流す。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void arrivalAnnouncesTheFloorThenTheDirection(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        car.pressHallCall(1, TravelDirection.UP);
        AtomicBoolean sawFloor = new AtomicBoolean();
        AtomicBoolean sawDirection = new AtomicBoolean();
        AtomicReference<DoorState> previous = new AtomicReference<>(DoorState.CLOSED);
        helper.onEachTick(() -> {
            DoorState state = car.doorState();
            if (previous.get() == DoorState.CLOSED && state == DoorState.OPENING && !car.isMoving()) {
                helper.assertTrue(car.recentAnnouncements().get(0).equals(Announcement.arrival("2")),
                        "the floor should be announced as the door starts opening: " + car.recentAnnouncements());
                sawFloor.set(true);
            }
            if (previous.get() == DoorState.OPENING && state == DoorState.OPEN && sawFloor.get() && !sawDirection.get()) {
                helper.assertTrue(car.recentAnnouncements().get(0).equals(Announcement.direction(TravelDirection.UP)),
                        "the direction should be announced once the door is open: " + car.recentAnnouncements());
                sawDirection.set(true);
            }
            previous.set(state);
        });
        helper.succeedWhen(() -> helper.assertTrue(sawFloor.get() && sawDirection.get(), "not announced yet"));
    }

    /** かごがいる階の乗場呼びでは、階の案内はせず、行先方向だけを流す。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void hallCallAtTheCarFloorAnnouncesOnlyTheDirection(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        car.pressHallCall(0, TravelDirection.UP);
        helper.succeedWhen(() -> {
            helper.assertTrue(car.doorState() == DoorState.OPEN, "not open yet");
            helper.assertTrue(car.recentAnnouncements().equals(List.of(Announcement.direction(TravelDirection.UP))),
                    "announcements: " + car.recentAnnouncements());
        });
    }

    /** 戸が開いている間に行先が決まったら、その場で行先方向を流す。同じ方向は繰り返さない。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void directionIsAnnouncedWhenItIsDecidedWhileTheDoorIsOpen(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 1, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        // かご呼びでその場の戸を開く（方向は決まらない）
        helper.assertTrue(car.pressCarCall(1) == PressResult.SERVED_HERE, "the door should open");
        AtomicBoolean pressed = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!pressed.get() && car.doorState() == DoorState.OPEN) {
                helper.assertTrue(car.recentAnnouncements().isEmpty(), "nothing should be announced yet: " + car.recentAnnouncements());
                pressed.set(true);
                // 乗った人が下の階を押した
                car.pressCarCall(0);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pressed.get(), "not pressed yet");
            helper.assertTrue(car.recentAnnouncements().equals(List.of(Announcement.direction(TravelDirection.DOWN))),
                    "announcements: " + car.recentAnnouncements());
        });
    }

    /** 行先方向を流している間に閉ボタンを押すと、戸閉アナウンスは流さない。流し終わってからなら流す。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void closeButtonDuringTheDirectionSkipsTheCloseAnnouncement(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE).car();
        ElevatorCarEntity later = spawnCarOnly(helper, helper.absolutePos(LOWER).getX() + 3.5);
        car.pressHallCall(0, TravelDirection.UP);
        later.pressHallCall(0, TravelDirection.UP);
        AtomicLong openedAt = new AtomicLong(-1);
        AtomicBoolean pressedEarly = new AtomicBoolean();
        AtomicBoolean pressedLate = new AtomicBoolean();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (openedAt.get() < 0 && car.doorState() == DoorState.OPEN) {
                openedAt.set(now);
            }
            if (openedAt.get() >= 0 && !pressedEarly.get()) {
                // 「上へ参ります。」を流し始めた直後
                pressedEarly.set(true);
                car.closeDoor();
            }
            if (openedAt.get() >= 0 && !pressedLate.get() && now >= openedAt.get() + AnnouncementScript.DIRECTION_TICKS + 2
                    && later.doorState() == DoorState.OPEN) {
                pressedLate.set(true);
                later.closeDoor();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pressedEarly.get() && pressedLate.get(), "not pressed yet");
            helper.assertTrue(car.recentAnnouncements().equals(List.of(Announcement.direction(TravelDirection.UP))),
                    "should skip the close announcement: " + car.recentAnnouncements());
            helper.assertTrue(later.recentAnnouncements().get(0).equals(Announcement.doorClose()),
                    "should announce after the direction: " + later.recentAnnouncements());
        });
    }

    /** 行先方向を流している間に開放待機が終わったら、戸閉アナウンスを流さず、待たずに閉め始める。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void automaticClosingDuringTheDirectionSkipsTheCloseAnnouncement(GameTestHelper helper) {
        ElevatorCarEntity car = spawn(helper, 1, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        car.pressCarCall(1);
        AtomicLong openedAt = new AtomicLong(-1);
        AtomicLong closingAt = new AtomicLong(-1);
        AtomicBoolean pressed = new AtomicBoolean();
        long dwell = DoorTiming.STANDARD.carDwellTicks();
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (openedAt.get() < 0 && car.doorState() == DoorState.OPEN) {
                openedAt.set(now);
            }
            // 開放待機が終わる少し前に行先が決まり、「下へ参ります。」が流れ始める
            if (openedAt.get() >= 0 && !pressed.get() && now >= openedAt.get() + dwell - 5) {
                pressed.set(true);
                car.pressCarCall(0);
            }
            if (closingAt.get() < 0 && car.doorState() == DoorState.CLOSING) {
                closingAt.set(now);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(closingAt.get() >= 0, "not closing yet");
            helper.assertTrue(closingAt.get() - openedAt.get() == dwell, "should close without waiting: " + (closingAt.get() - openedAt.get()));
            helper.assertTrue(car.recentAnnouncements().equals(List.of(Announcement.direction(TravelDirection.DOWN))),
                    "should skip the close announcement: " + car.recentAnnouncements());
        });
    }

    /** 停止階の間隔が5ブロック未満のかごでは、乗場の器具を出入口の上に付けられず、横へ移す。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void overDoorHallFixturesNeedFiveBlockFloors(GameTestHelper helper) {
        FixtureConfig overDoor = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_DOOR)
                .withHallLantern(FixtureConfig.HallLanternPlacement.OVER_DOOR);
        ElevatorCarEntity low = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, 4, 8).car();
        low.setFixtures(overDoor);
        helper.assertTrue(low.fixtures().hallIndicator() == FixtureConfig.HallIndicator.OVER_BUTTONS, "the indicator should move beside the door");
        helper.assertTrue(low.fixtures().hallLantern() == FixtureConfig.HallLanternPlacement.BESIDE_DOOR, "the lantern should move beside the door");
        ElevatorCarEntity tall = spawnCarOnly(helper, helper.absolutePos(LOWER).getX() + 3.5);
        tall.setFixtures(overDoor);
        helper.assertTrue(tall.fixtures().equals(overDoor), "with 7-block floors the door top is fine");
        helper.succeed();
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

    /**
     * 車椅子用操作盤の行先ボタンで着いた階では、開放待機が延びてゆっくり閉じる。
     * 普通の操作盤で押しても車椅子用操作盤は点かず、車椅子用操作盤で押すと両方点く。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void wheelchairCarCallsHoldTheDoorLonger(GameTestHelper helper) {
        Elevator elevator = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE);
        ElevatorCarEntity car = elevator.car();
        int usual = car.buttons().size();
        car.setFixtures(car.fixtures().withWheelchairPanel(true));
        helper.assertTrue(car.buttons().size() == usual + (2 + 2) * 2, "a wheelchair panel on each side wall");
        int wheelchairSecond = wheelchairIndexOf(car.buttons(), ButtonLayout.Kind.CAR_CALL, 1);
        int usualSecond = indexOf(car.buttons(), ButtonLayout.Kind.CAR_CALL, 1);
        car.pressButton(usualSecond);
        helper.assertTrue(car.isButtonLit(usualSecond) && !car.isButtonLit(wheelchairSecond), "only the usual panel should light up");
        car.pressButton(wheelchairSecond);
        helper.assertTrue(car.isButtonLit(wheelchairSecond) && car.isButtonLit(usualSecond), "both panels should light up");
        int top = car.floors()[1];
        helper.succeedWhen(() -> {
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorState() == DoorState.OPEN, "not open yet");
            helper.assertTrue(car.doorDwellTicks() == DoorTiming.STANDARD.wheelchairDwellTicks() && car.isDoorSlowClose(),
                    "the wheelchair stop should wait longer and close slowly");
        });
    }

    /** 乗場の車椅子用ボタンは▲▼の下にあり、押すと車椅子用と普通のボタンが点き、着いたら開放待機が延びる。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void wheelchairHallButtonsHoldTheDoorLonger(GameTestHelper helper) {
        Elevator elevator = spawn(helper, 0, SpeedSpec.NORMAL_HIGH, 0, RISE);
        ElevatorCarEntity car = elevator.car();
        LandingDoorEntity upper = elevator.landingDoors().get(1);
        helper.assertTrue(upper.buttons().size() == 1, "no wheelchair button before it is enabled");
        car.setFixtures(car.fixtures().withWheelchairPanel(true));
        helper.assertTrue(upper.buttons().size() == 2, "the top floor has the usual and the wheelchair down button");
        int usualDown = indexOf(upper.buttons(), ButtonLayout.Kind.HALL_DOWN, 1);
        int wheelchairDown = wheelchairIndexOf(upper.buttons(), ButtonLayout.Kind.HALL_DOWN, 1);
        // 普通のボタンで登録したあとに車椅子用を押しても、車椅子用として扱う
        upper.pressButton(usualDown);
        helper.assertTrue(upper.isButtonLit(usualDown) && !upper.isButtonLit(wheelchairDown), "only the usual button should light");
        upper.pressButton(wheelchairDown);
        helper.assertTrue(upper.isButtonLit(usualDown) && upper.isButtonLit(wheelchairDown), "both buttons should light");
        int top = car.floors()[1];
        helper.succeedWhen(() -> {
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorState() == DoorState.OPEN, "not open yet");
            helper.assertTrue(car.doorDwellTicks() == DoorTiming.STANDARD.wheelchairDwellTicks() && car.isDoorSlowClose(),
                    "the wheelchair stop should wait longer and close slowly");
            helper.assertTrue(!upper.isButtonLit(wheelchairDown) && !upper.isButtonLit(usualDown), "the lamps should go out");
        });
    }

    private static int wheelchairIndexOf(List<ButtonLayout.Button> buttons, ButtonLayout.Kind kind, int floor) {
        for (int i = 0; i < buttons.size(); i++) {
            ButtonLayout.Button button = buttons.get(i);
            if (button.wheelchair() && button.kind() == kind && button.floor() == floor) {
                return i;
            }
        }
        throw new IllegalStateException("no wheelchair button " + kind + " " + floor);
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
