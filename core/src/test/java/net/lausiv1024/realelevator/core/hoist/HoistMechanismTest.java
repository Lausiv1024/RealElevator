package net.lausiv1024.realelevator.core.hoist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Point;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Sheave;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;

class HoistMechanismTest {
    private static final double EPS = 1e-9;
    private static final int[] FLOORS = {64, 69, 73, 80};

    private static List<HoistMechanism> all() {
        List<HoistMechanism> list = new ArrayList<>();
        for (CarModel model : CarModel.values()) {
            for (PitMode pit : PitMode.values()) {
                list.add(new HoistMechanism(model, pit, FLOORS));
            }
        }
        return list;
    }

    private static double length(List<Point> rope) {
        double sum = 0;
        for (int i = 1; i < rope.size(); i++) {
            Point a = rope.get(i - 1);
            Point b = rope.get(i);
            sum += Math.sqrt(Math.pow(b.x() - a.x(), 2) + Math.pow(b.y() - a.y(), 2) + Math.pow(b.z() - a.z(), 2));
        }
        return sum;
    }

    @Test
    void ropeLengthStaysTheSameWhereverTheCarIs() {
        for (HoistMechanism hoist : all()) {
            List<List<Point>> atBottom = hoist.ropes(64);
            for (double y = 64; y <= 80; y += 0.37) {
                List<List<Point>> ropes = hoist.ropes(y);
                assertEquals(HoistMechanism.ROPE_COUNT, ropes.size());
                for (int i = 0; i < ropes.size(); i++) {
                    // 斜めの区間はわずかに長さが変わるので、1pxの数分の1まで許す
                    assertEquals(length(atBottom.get(i)), length(ropes.get(i)), 0.02, hoist.model() + " " + y);
                }
            }
        }
    }

    @Test
    void counterweightMovesOppositeToTheCarAtTheSameSpeed() {
        HoistMechanism hoist = new HoistMechanism(CarModel.P11, PitMode.REAL, FLOORS);
        assertEquals(-3.5, hoist.counterweightBottom(73.5) - hoist.counterweightBottom(70), EPS);
    }

    @Test
    void sheavesTurnByTheRopeThatPassesOverThem() {
        HoistMechanism hoist = new HoistMechanism(CarModel.P11, PitMode.REAL, FLOORS);
        List<Sheave> at0 = hoist.sheaves(64);
        List<Sheave> at1 = hoist.sheaves(65);
        double[] ropePerBlock = {1, 1, -2, -2, -1};
        for (int i = 0; i < at0.size(); i++) {
            double ropeRadius = at0.get(i).radius() + HoistMechanism.ROPE_WIDTH_PX / CarModel.PIXELS_PER_BLOCK / 2;
            assertEquals(ropePerBlock[i], (at1.get(i).angle() - at0.get(i).angle()) * ropeRadius, EPS);
        }
        // 巻上機の綱車は、かごの2倍の周速で回る
        assertEquals(0, at0.get(2).angle(), EPS);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void nothingHitsTheCarOrLeavesTheShaft(CarModel model) {
        for (PitMode pit : PitMode.values()) {
            HoistMechanism hoist = new HoistMechanism(model, pit, FLOORS);
            Box shaft = hoist.shaftBounds();
            List<Box> fixed = new ArrayList<>();
            fixed.add(hoist.motor());
            fixed.addAll(hoist.counterweightRails());
            fixed.addAll(hoist.hitches());
            for (double y = 64; y <= 80; y += 0.25) {
                Box car = CarGeometry.exterior(model).move(0, y, 0);
                Box cw = hoist.counterweight(y);
                List<Box> moving = new ArrayList<>(hoist.counterweightSheaveBrackets(y));
                moving.add(cw);
                for (Box box : moving) {
                    assertFalse(box.intersects(car), model + " " + pit + " counterweight hits the car at " + y);
                    assertTrue(inside(box, shaft), model + " " + pit + " counterweight leaves the shaft at " + y);
                }
                for (Box box : fixed) {
                    assertFalse(box.intersects(car), model + " " + pit + " " + box + " hits the car at " + y);
                    assertFalse(box.intersects(cw), model + " " + pit + " " + box + " hits the counterweight at " + y);
                    assertTrue(inside(box, shaft), model + " " + pit + " " + box + " leaves the shaft");
                }
                for (Sheave sheave : hoist.sheaves(y)) {
                    Box b = bounds(sheave);
                    assertTrue(inside(b, shaft), model + " " + pit + " sheave leaves the shaft at " + y);
                    if (sheave.centre().y() > y) {
                        // かご下以外のシーブはかごに当たらない
                        assertFalse(b.intersects(car), model + " " + pit + " sheave hits the car at " + y);
                    }
                }
                for (List<Point> rope : hoist.ropes(y)) {
                    for (Point p : rope) {
                        assertTrue(p.x() > shaft.minX() && p.x() < shaft.maxX() && p.z() > shaft.minZ()
                                && p.y() >= shaft.minY() && p.y() <= shaft.maxY(), model + " rope leaves the shaft");
                    }
                }
            }
        }
    }

    @Test
    void ropesPassOutsideTheCarSides() {
        for (HoistMechanism hoist : all()) {
            Box car = CarGeometry.exterior(hoist.model());
            for (List<Point> rope : hoist.ropes(70)) {
                // 固定端からかご下のシーブまで（左）と、かご下のシーブから巻上機まで（右）の縦の区間
                Point leftTop = rope.get(0);
                assertTrue(Math.abs(leftTop.x()) - HoistMechanism.ROPE_WIDTH_PX / 32 > car.maxX());
            }
        }
    }

    @Test
    void carSheavesSitUnderTheMiddleOfTheCar() {
        for (HoistMechanism hoist : all()) {
            List<Sheave> sheaves = hoist.sheaves(70);
            assertEquals(0, sheaves.get(0).centre().z(), EPS);
            assertEquals(0, sheaves.get(1).centre().z(), EPS);
            assertEquals(-sheaves.get(0).centre().x(), sheaves.get(1).centre().x(), EPS);
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void ropesNeverPassThroughTheCar(CarModel model) {
        double half = HoistMechanism.ROPE_WIDTH_PX / CarModel.PIXELS_PER_BLOCK / 2;
        for (PitMode pit : PitMode.values()) {
            HoistMechanism hoist = new HoistMechanism(model, pit, FLOORS);
            for (double y = 64; y <= 80; y += 0.5) {
                Box e = CarGeometry.exterior(model).move(0, y, 0);
                Box car = new Box(e.minX() - half, e.minY() - half, e.minZ() - half, e.maxX() + half, e.maxY() + half, e.maxZ() + half);
                for (List<Point> rope : hoist.ropes(y)) {
                    for (int i = 1; i < rope.size(); i++) {
                        Point a = rope.get(i - 1);
                        Point b = rope.get(i);
                        for (int k = 0; k <= 64; k++) {
                            double t = k / 64.0;
                            double px = a.x() + (b.x() - a.x()) * t;
                            double py = a.y() + (b.y() - a.y()) * t;
                            double pz = a.z() + (b.z() - a.z()) * t;
                            boolean in = px > car.minX() + EPS && px < car.maxX() - EPS && py > car.minY() + EPS
                                    && py < car.maxY() - EPS && pz > car.minZ() + EPS && pz < car.maxZ() - EPS;
                            assertFalse(in, model + " " + pit + " rope passes through the car at y=" + y + " (" + px + ", " + py + ", " + pz + ")");
                        }
                    }
                }
            }
        }
    }

    private static boolean inside(Box box, Box shaft) {
        return box.minX() >= shaft.minX() - EPS && box.maxX() <= shaft.maxX() + EPS
                && box.minY() >= shaft.minY() - EPS && box.maxY() <= shaft.maxY() + EPS
                && box.minZ() >= shaft.minZ() - EPS && box.maxZ() <= shaft.maxZ() + EPS;
    }

    private static Box bounds(Sheave s) {
        Point c = s.centre();
        double r = s.radius();
        double w = s.width() / 2;
        return s.axis() == HoistMechanism.Axis.X
                ? new Box(c.x() - w, c.y() - r, c.z() - r, c.x() + w, c.y() + r, c.z() + r)
                : new Box(c.x() - r, c.y() - r, c.z() - w, c.x() + r, c.y() + r, c.z() + w);
    }
}
