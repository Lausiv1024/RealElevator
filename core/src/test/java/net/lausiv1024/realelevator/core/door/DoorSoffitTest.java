package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;

/** 戸の上の張り出し。 */
class DoorSoffitTest {
    private static final double EPS = 1e-9;

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

    @ParameterizedTest
    @MethodSource("specs")
    void soffitsReachEachClosedDoorAboveTheEntrance(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        for (DoorGeometry.Side side : DoorGeometry.Side.values()) {
            List<Box> soffits = DoorGeometry.soffits(spec, side);
            List<Box> closed = DoorGeometry.panels(spec, side, 0);
            double width = 0;
            for (int i = 0; i < soffits.size(); i++) {
                Box soffit = soffits.get(i);
                width += soffit.sizeX();
                assertEquals(entrance.maxY(), soffit.minY(), EPS);
                // 区間に重なる戸板のうち、いちばん奥のものの奥行きまで
                double edge = closed.stream().filter(p -> p.minX() < soffit.maxX() - EPS && p.maxX() > soffit.minX() + EPS)
                        .mapToDouble(p -> side == DoorGeometry.Side.CAR ? p.maxZ() : -p.minZ()).max().orElseThrow();
                assertEquals(edge, side == DoorGeometry.Side.CAR ? soffit.maxZ() : -soffit.minZ(), EPS, side + " " + soffit);
                for (int j = i + 1; j < soffits.size(); j++) {
                    assertFalse(soffit.intersects(soffits.get(j)));
                }
                for (double fraction : new double[] {0, 0.5, 1}) {
                    for (Box moving : DoorGeometry.panels(spec, side, fraction)) {
                        assertFalse(soffit.intersects(moving), side + " " + moving);
                    }
                }
            }
            assertEquals(entrance.sizeX(), width, EPS);
        }
        // かごの張り出しは前壁の外面から。乗場の張り出しとは敷居のすき間だけ離れる（かごが動いてもぶつからない）
        double carFront = DoorGeometry.soffits(spec, DoorGeometry.Side.CAR).stream().mapToDouble(Box::maxZ).max().orElseThrow();
        double landingBack = DoorGeometry.soffits(spec, DoorGeometry.Side.LANDING).stream().mapToDouble(Box::minZ).min().orElseThrow();
        assertTrue(landingBack > carFront);
        assertTrue(DoorGeometry.soffits(spec, DoorGeometry.Side.CAR).stream().allMatch(b -> b.minZ() == CarGeometry.exterior(spec).maxZ()));
    }

    @ParameterizedTest
    @MethodSource("specs")
    void sideOpeningDoorsGetAJambReturnOnTheClosingSide(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        boolean side = spec.door() == DoorType.SIDE_LEFT || spec.door() == DoorType.SIDE_RIGHT;
        for (DoorGeometry.Side s : DoorGeometry.Side.values()) {
            List<Box> returns = DoorGeometry.jambReturns(spec, s);
            assertEquals(side ? 1 : 0, returns.size(), s.toString());
            for (Box r : returns) {
                // 戸袋と反対側の縁の外
                boolean left = spec.door() == DoorType.SIDE_RIGHT;
                assertEquals(left ? entrance.minX() : entrance.maxX(), left ? r.maxX() : r.minX(), EPS);
                assertEquals(0, r.minY(), EPS);
                assertEquals(entrance.maxY(), r.maxY(), EPS);
                for (double fraction : new double[] {0, 0.25, 0.5, 0.75, 1}) {
                    for (Box panel : DoorGeometry.panels(spec, s, fraction)) {
                        assertFalse(r.intersects(panel), s + " " + fraction + " " + panel);
                    }
                }
            }
        }
    }
}
