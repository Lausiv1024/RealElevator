package net.lausiv1024.realelevator.core.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorGeometry.Side;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Point;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.core.install.ShaftLayout.CounterweightPosition;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;

/** 二方向出入口（前後・スルー型）の昇降路の配置。 */
class ThroughLayoutTest {
    private static final double EPS = 1e-9;
    private static final int[] FLOORS = {0, 5, 10};
    private static final FloorEntrances[] ENTRANCES = {FloorEntrances.FRONT, FloorEntrances.REAR, FloorEntrances.BOTH};

    static Stream<CarSpec> specs() {
        List<CarSpec> specs = new ArrayList<>();
        for (CarModel model : CarModel.values()) {
            for (DoorType door : DoorType.values()) {
                if (door.isAvailable(model)) {
                    specs.add(new CarSpec(model, door));
                }
            }
        }
        return specs.stream();
    }

    /** 囲う壁は、昇降路と前後の三方枠の場所にかからず、昇降路のまわりを1列で囲む。 */
    @ParameterizedTest
    @MethodSource("specs")
    void theEnclosureLeavesTheFramesOpen(CarSpec spec) {
        ShaftLayout layout = through(spec, PitMode.REAL);
        Set<ShaftLayout.Cell> clear = new HashSet<>(layout.requiredClearCells());
        List<ShaftLayout.Cell> walls = layout.enclosureCells();
        for (ShaftLayout.Cell cell : walls) {
            assertFalse(clear.contains(cell), "a wall in the shaft or a frame: " + cell);
        }
        // 奥を使う階の奥の三方枠と、前を使う階の三方枠の場所は空く
        assertFalse(walls.contains(new ShaftLayout.Cell(-1, FLOORS[1], layout.rearWallRow())));
        assertFalse(walls.contains(new ShaftLayout.Cell(-1, FLOORS[0], 0)));
        // 奥を使わない階の奥の壁の列はふさぐ
        assertTrue(walls.contains(new ShaftLayout.Cell(-1, FLOORS[0], layout.rearWallRow())));
    }

    private static ShaftLayout through(CarSpec spec, PitMode pit) {
        return new ShaftLayout(spec, pit, CounterweightPosition.REAR, FLOORS, ENTRANCES);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void theRearWallIsTheFrontWallTurnedAround(CarSpec spec) {
        List<Part> parts = CarGeometry.parts(spec, true);
        // 奥壁の代わりに、袖壁とトランサムが3つ増える
        assertEquals(CarGeometry.parts(spec).size() + 2, parts.size());
        Box rearEntrance = CarGeometry.entrance(spec).rotateQuarterTurns(Entrance.REAR.quarterTurns());
        Box inside = new Box(rearEntrance.minX() + EPS, 0.1, rearEntrance.minZ(), rearEntrance.maxX() - EPS, 1, rearEntrance.maxZ());
        for (Part part : parts) {
            assertFalse(part.box().intersects(inside), spec + " the rear entrance should be open");
        }
    }

    @ParameterizedTest
    @MethodSource("specs")
    void theRearFrameReachesTheNextBlockFace(CarSpec spec) {
        ShaftLayout layout = through(spec, PitMode.REAL);
        assertEquals(CounterweightPosition.SIDE, layout.counterweight(), "the counterweight goes to the side");
        double offset = layout.rearFaceOffset();
        assertTrue(offset >= -EPS && offset < 1, spec + " offset " + offset);
        // 奥の三方枠（正面の形を180°回したもの）の乗場側の面が、奥の壁の列の乗場側の面にそろう
        double rearFaceLocal = Double.MAX_VALUE;
        for (Box box : DoorGeometry.landingFrame(spec, offset)) {
            rearFaceLocal = Math.min(rearFaceLocal, box.rotateQuarterTurns(Entrance.REAR.quarterTurns()).minZ());
        }
        assertEquals(layout.rearWallRow() - 0.5, layout.carZ() + rearFaceLocal, EPS, spec.toString());
        // 奥の乗場の戸のかご側の面は、奥の壁の列の昇降路側の面か、それより手前（正面の乗場の戸と同じ）
        Box rearDoors = DoorGeometry.travelBounds(spec, Side.LANDING).rotateQuarterTurns(Entrance.REAR.quarterTurns());
        double carSide = layout.carZ() + rearDoors.maxZ();
        assertTrue(carSide >= layout.rearWallRow() + 0.5 - EPS && carSide < layout.rearWallRow() + 1.5 - EPS,
                spec + " the rear doors should start at the rear wall row: " + carSide);
        assertEquals(layout.rearWallRow() + 1, layout.backRow());
    }

    @Test
    void rearFramesAreClearedOnlyOnFloorsWithARearEntrance() {
        ShaftLayout layout = through(CarSpec.of(CarModel.P11), PitMode.REAL);
        Set<Cell> clear = new HashSet<>(layout.requiredClearCells());
        int rear = layout.rearWallRow();
        int left = layout.leftColumn();
        // 0階は前だけ、5階は後だけ、10階は両方
        assertTrue(clear.contains(new Cell(left, 0, 0)));
        assertFalse(clear.contains(new Cell(left, 0, rear)));
        assertFalse(clear.contains(new Cell(left, 5, 0)));
        assertTrue(clear.contains(new Cell(left, 5, rear)));
        assertTrue(clear.contains(new Cell(left, 10, 0)));
        assertTrue(clear.contains(new Cell(left, 10, rear)));
        // 横落としの重りの列も
        assertTrue(clear.contains(new Cell(layout.counterweightColumn(), 3, -1)));
    }

    @ParameterizedTest
    @MethodSource("specs")
    void theHoistwayStaysClearOfTheRearLanding(CarSpec spec) {
        for (PitMode pit : PitMode.values()) {
            ShaftLayout layout = through(spec, pit);
            int[] absolute = {64, 69, 74};
            HoistMechanism hoist = new HoistMechanism(spec, pit, CounterweightPosition.REAR, true, absolute);
            Box rearDoors = DoorGeometry.travelBounds(spec, Side.LANDING).rotateQuarterTurns(Entrance.REAR.quarterTurns());
            // 奥の乗場の戸の、かご側の面より手前（三方枠の幅の中）、重りの列では昇降路の奥の壁より手前
            double rearLimit = rearDoors.maxZ();
            double frameHalf = DoorGeometry.landingFrameHalfWidth(spec);
            double backWall = layout.backRow() - 0.5 - layout.carZ();
            for (double y = 64; y <= 74; y += 0.5) {
                List<Box> moving = new ArrayList<>(hoist.counterweightSheaveBrackets(y));
                moving.add(hoist.counterweight(y));
                moving.add(hoist.motor());
                moving.addAll(hoist.counterweightRails());
                moving.addAll(hoist.hitches());
                for (Box box : moving) {
                    double limit = box.minX() < frameHalf - EPS ? rearLimit : backWall;
                    assertTrue(box.minZ() >= limit - EPS, spec + " " + pit + " " + box + " reaches the rear landing");
                }
                for (List<Point> rope : hoist.ropes(y)) {
                    for (Point p : rope) {
                        double limit = p.x() < frameHalf - EPS ? rearLimit : backWall;
                        assertTrue(p.z() >= limit, spec + " " + pit + " a rope reaches the rear landing");
                    }
                }
            }
            // 頂部の制御盤は、設置用ブロックの真後ろの壁に付き、機構のどれとも重ならない
            assertEquals(ShaftLayout.ControllerMount.FRONT_WALL, layout.controllerMount());
            List<Box> controller = new ArrayList<>();
            for (Box box : layout.controllerBoxes()) {
                controller.add(box.move(-layout.carX(), 64, -layout.carZ()));
            }
            for (double y = 64; y <= 74; y += 0.5) {
                List<Box> parts = new ArrayList<>(hoist.counterweightSheaveBrackets(y));
                parts.add(hoist.counterweight(y));
                parts.add(hoist.motor());
                parts.addAll(hoist.counterweightRails());
                parts.addAll(hoist.hitches());
                parts.add(CarGeometry.exterior(spec).move(0, y, 0));
                for (HoistMechanism.Sheave sheave : hoist.sheaves(y)) {
                    Point c = sheave.centre();
                    double r = sheave.radius();
                    double w = sheave.width() / 2;
                    parts.add(sheave.axis() == HoistMechanism.Axis.X
                            ? new Box(c.x() - w, c.y() - r, c.z() - r, c.x() + w, c.y() + r, c.z() + r)
                            : new Box(c.x() - r, c.y() - r, c.z() - w, c.x() + r, c.y() + r, c.z() + w));
                }
                for (Box part : parts) {
                    for (Box ctrl : controller) {
                        assertFalse(part.intersects(ctrl), spec + " " + pit + " " + part + " hits the controller at " + y);
                    }
                }
                for (List<Point> rope : hoist.ropes(y)) {
                    for (Point p : rope) {
                        for (Box ctrl : controller) {
                            assertFalse(p.x() > ctrl.minX() && p.x() < ctrl.maxX() && p.y() > ctrl.minY() && p.y() < ctrl.maxY()
                                    && p.z() > ctrl.minZ() && p.z() < ctrl.maxZ(), spec + " " + pit + " a rope passes through the controller");
                        }
                    }
                }
            }
        }
    }
}
