package net.lausiv1024.realelevator.core.hoist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.hoist.CarFrame.Part;
import net.lausiv1024.realelevator.core.hoist.CarFrame.Type;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Point;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Sheave;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.CounterweightPosition;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;

class CarFrameTest {
    /** たて枠を外壁の面に埋めるとき、ちらつかないよう 0.002 だけ外へ出すので、それより小さい重なりは数えない。 */
    private static final double EPS = 0.003;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    private static final int[] FLOORS = {0, 8};

    record Setup(CarSpec spec, PitMode pit, CounterweightPosition counterweight, boolean through) {
        HoistMechanism hoist() {
            return new HoistMechanism(spec, pit, counterweight, through, FLOORS);
        }
    }

    static Stream<Setup> setups() {
        List<Setup> setups = new ArrayList<>();
        for (CarModel model : CarModel.values()) {
            for (DoorType door : DoorType.values()) {
                if (!door.isAvailable(model)) {
                    continue;
                }
                for (PitMode pit : PitMode.values()) {
                    CarSpec spec = new CarSpec(model, door);
                    setups.add(new Setup(spec, pit, CounterweightPosition.REAR, false));
                    setups.add(new Setup(spec, pit, CounterweightPosition.SIDE, false));
                    setups.add(new Setup(spec, pit, CounterweightPosition.SIDE, true));
                }
            }
        }
        return setups.stream();
    }

    /** ガイドレールの T 形（右側。左は左右に映したもの）の、かごのローカル座標での断面を高さ方向に伸ばしたもの。 */
    private static List<Box> rails(HoistMechanism hoist, double railZ) {
        double half = hoist.shaftBounds().maxX();
        if (hoist.shaftBounds().maxX() > -hoist.shaftBounds().minX() + EPS) {
            // 横落としでは右に重りの列があるので、かごの昇降路の幅は左右対称の側から求める
            half = -hoist.shaftBounds().minX();
        }
        double w = ShaftLayout.RAIL_WIDTH_PX * PX;
        List<Box> boxes = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            double outer = side * half;
            double flangeInner = outer - side * PX;
            double tip = outer - side * w;
            boxes.add(new Box(Math.min(outer, flangeInner), -100, railZ - 2 * PX, Math.max(outer, flangeInner), 100, railZ + 2 * PX));
            boxes.add(new Box(Math.min(flangeInner, tip), -100, railZ - PX / 2, Math.max(flangeInner, tip), 100, railZ + PX / 2));
        }
        return boxes;
    }

    private static Box bounds(Sheave sheave) {
        Point c = sheave.centre();
        double r = sheave.radius();
        double w = sheave.width() / 2;
        return sheave.axis() == HoistMechanism.Axis.X
                ? new Box(c.x() - w, c.y() - r, c.z() - r, c.x() + w, c.y() + r, c.z() + r)
                : new Box(c.x() - r, c.y() - r, c.z() - w, c.x() + r, c.y() + r, c.z() + w);
    }

    /** かご側の機構（かご下のシーブとその金具、かごの横を通るロープ）。かごが最下階（Y=0）にいるとき。 */
    private static List<Box> hoistNearTheCar(HoistMechanism hoist) {
        List<Box> boxes = new ArrayList<>(hoist.carSheaveBrackets(0));
        List<Sheave> sheaves = hoist.sheaves(0);
        boxes.add(bounds(sheaves.get(0)));
        boxes.add(bounds(sheaves.get(1)));
        double half = HoistMechanism.ROPE_WIDTH_PX * PX / 2;
        for (List<Point> rope : hoist.ropes(0)) {
            for (int i = 1; i < rope.size(); i++) {
                Point a = rope.get(i - 1);
                Point b = rope.get(i);
                boxes.add(new Box(Math.min(a.x(), b.x()) - half, Math.min(a.y(), b.y()) - half, Math.min(a.z(), b.z()) - half,
                        Math.max(a.x(), b.x()) + half, Math.max(a.y(), b.y()) + half, Math.max(a.z(), b.z()) + half));
            }
        }
        return boxes;
    }

    private static boolean overlaps(Box a, Box b) {
        return a.minX() < b.maxX() - EPS && a.maxX() > b.minX() + EPS && a.minY() < b.maxY() - EPS && a.maxY() > b.minY() + EPS
                && a.minZ() < b.maxZ() - EPS && a.maxZ() > b.minZ() + EPS;
    }

    @ParameterizedTest
    @MethodSource("setups")
    void nothingHitsTheCarTheRailsOrTheHoist(Setup setup) {
        HoistMechanism hoist = setup.hoist();
        CarFrame frame = hoist.carFrame();
        ShaftLayout layout = new ShaftLayout(setup.spec(), setup.pit(), setup.counterweight(), FLOORS,
                setup.through() ? new net.lausiv1024.realelevator.core.car.FloorEntrances[] {
                        net.lausiv1024.realelevator.core.car.FloorEntrances.BOTH, net.lausiv1024.realelevator.core.car.FloorEntrances.BOTH} : null);
        double railZ = layout.railRowZ() - layout.carZ();
        List<Box> rails = rails(hoist, railZ);
        List<Box> mechanism = hoistNearTheCar(hoist);
        Box exterior = CarGeometry.exterior(setup.spec());
        List<Box> all = new ArrayList<>(frame.parts().stream().map(Part::box).toList());
        frame.rollers(0).forEach(r -> all.add(bounds(r)));
        Box belt = frame.parts().stream().filter(p -> p.type() == Type.BELT).findFirst().orElseThrow().box();
        for (double fraction : new double[] {0, 0.5, 1}) {
            List<Box> hangers = frame.hangers(fraction, Entrance.FRONT);
            all.addAll(hangers);
            // 吊り金具はベルトから外れない
            for (Box hanger : hangers) {
                assertTrue(hanger.minX() >= belt.minX() - EPS && hanger.maxX() <= belt.maxX() + EPS, setup + " " + fraction + " " + hanger);
            }
        }
        for (Part part : frame.parts()) {
            Box b = part.box();
            String name = setup + " " + part;
            // ピットの底と頂部の天井（かごが最下階・最上階のとき）に収まる
            assertTrue(b.minY() >= -setup.pit().pit + EPS, name);
            assertTrue(b.maxY() <= setup.pit().overhead - EPS, name);
            if (part.type() != Type.FRAME) {
                assertFalse(overlaps(b, exterior), "outside the car body: " + name);
                for (Part other : frame.parts()) {
                    if (other != part && other.type() != part.type()) {
                        assertFalse(overlaps(b, other.box()), name + " / " + other);
                    }
                }
            }
        }
        for (Box b : all) {
            for (Box rail : rails) {
                assertFalse(overlaps(b, rail), setup + " rail: " + b);
            }
            for (Box m : mechanism) {
                assertFalse(overlaps(b, m), setup + " hoist: " + b + " / " + m);
            }
        }
    }

    @Test
    void stilesSitInTheGapOrOnTheWall() {
        // P-11 は外壁とレールの先端のあいだに 2px あるので、たて枠はその中。P-13 はすき間がないので外壁の面に埋める
        for (CarModel model : new CarModel[] {CarModel.P11, CarModel.P13}) {
            HoistMechanism hoist = new HoistMechanism(CarSpec.of(model), PitMode.REAL, FLOORS);
            double outer = CarGeometry.exterior(model).maxX();
            Box stile = hoist.carFrame().parts().stream().filter(p -> p.type() == Type.FRAME && p.box().sizeY() > 2 && p.box().minX() > 0)
                    .findFirst().orElseThrow().box();
            if (model == CarModel.P11) {
                assertEquals(outer, stile.minX(), EPS);
                assertEquals(PX, stile.sizeX(), EPS);
            } else {
                assertTrue(stile.minX() < outer && stile.maxX() > outer);
            }
        }
    }

    @Test
    void apronAndFenceFitThePitAndTheOverhead() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        Box realApron = apron(new HoistMechanism(spec, PitMode.REAL, FLOORS).carFrame());
        Box compactApron = apron(new HoistMechanism(spec, PitMode.COMPACT, FLOORS).carFrame());
        assertEquals(CarFrame.APRON_PX * PX, realApron.sizeY(), EPS);
        assertTrue(compactApron.sizeY() < realApron.sizeY());
        double roof = (CarModel.CEILING_HEIGHT_PX + CarGeometry.CEILING_PX) * PX;
        double realTop = new HoistMechanism(spec, PitMode.REAL, FLOORS).carFrame().fence().stream().mapToDouble(Box::maxY).max().orElseThrow();
        assertEquals(roof + CarFrame.FENCE_PX * PX, realTop, EPS);
        // 出入口の側には安全柵を付けない
        Box exterior = CarGeometry.exterior(spec);
        CarFrame frame = new HoistMechanism(spec, PitMode.REAL, FLOORS).carFrame();
        assertTrue(frame.fence().stream().noneMatch(b -> b.minZ() >= exterior.maxZ() - PX - EPS && b.sizeX() > 2 * PX));
        assertTrue(frame.fence().stream().anyMatch(b -> b.minZ() <= exterior.minZ() + EPS && b.sizeX() > 2 * PX));
    }

    @Test
    void hangersFollowTheDoors() {
        CarFrame frame = new HoistMechanism(CarSpec.of(CarModel.P11), PitMode.REAL, FLOORS).carFrame();
        List<Box> closed = frame.hangers(0, Entrance.FRONT);
        List<Box> open = frame.hangers(1, Entrance.FRONT);
        assertEquals(2, closed.size());
        assertTrue(open.get(0).minX() < closed.get(0).minX() && open.get(1).minX() > closed.get(1).minX());
    }

    private static Box apron(CarFrame frame) {
        return frame.parts().stream().filter(p -> p.type() == Type.APRON).findFirst().orElseThrow().box();
    }
}
