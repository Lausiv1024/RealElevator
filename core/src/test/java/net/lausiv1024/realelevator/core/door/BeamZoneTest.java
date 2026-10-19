package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;

/** 多光軸ビームセンサーの検知領域は、かごの戸と乗場の戸の間の敷居のすき間に、出入口の幅と全高で置く。 */
class BeamZoneTest {
    private static final double EPS = 1e-9;

    static Stream<CarSpec> specs() {
        return DoorTypeGeometryTest.specs();
    }

    @ParameterizedTest
    @MethodSource("specs")
    void theBeamFillsTheSillGapAcrossTheEntrance(CarSpec spec) {
        Box beam = DoorGeometry.beamZone(spec);
        Box entrance = CarGeometry.entrance(spec);
        assertEquals(entrance.minX(), beam.minX(), EPS);
        assertEquals(entrance.maxX(), beam.maxX(), EPS);
        assertEquals(0, beam.minY(), EPS);
        assertEquals(entrance.maxY(), beam.maxY(), EPS);
        assertEquals(DoorGeometry.SILL_GAP_PX / (double) CarModel.PIXELS_PER_BLOCK, beam.sizeZ(), EPS);
        // かごの戸と乗場の戸のどちらにも重ならず、その間にある
        for (double fraction : new double[] {0, 0.5, 1}) {
            for (Box panel : DoorGeometry.panels(spec, DoorGeometry.Side.CAR, fraction)) {
                assertTrue(panel.maxZ() <= beam.minZ() + EPS, spec + " car door " + panel);
            }
            for (Box panel : DoorGeometry.panels(spec, DoorGeometry.Side.LANDING, fraction)) {
                assertTrue(panel.minZ() >= beam.maxZ() - EPS, spec + " landing door " + panel);
            }
        }
    }
}
