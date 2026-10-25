package net.lausiv1024.realelevator.core.car;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarGeometry.PartType;

class CarGeometryTest {
    private static final double EPS = 1e-12;
    private static final double PX = 1.0 / 16;

    @Test
    void dimensionsMatchSpec() {
        assertEquals(2.125, CarGeometry.interior(CarModel.P9).sizeX());
        assertEquals(1.25, CarGeometry.interior(CarModel.P6).sizeZ());
        assertEquals(1.625, CarGeometry.interior(CarModel.P9).sizeZ());
        assertEquals(2.0, CarGeometry.interior(CarModel.P11).sizeZ());
        assertEquals(2.375, CarGeometry.interior(CarModel.P13).sizeX());
        assertEquals(2.0, CarGeometry.interior(CarModel.P13).sizeZ());
        assertEquals(3.4375, CarGeometry.interior(CarModel.P11).sizeY());
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void exteriorAddsWallFloorAndCeilingThickness(CarModel model) {
        Box interior = CarGeometry.interior(model);
        Box exterior = CarGeometry.exterior(model);
        assertEquals(interior.sizeX() + 2 * PX, exterior.sizeX(), EPS);
        assertEquals(interior.sizeZ() + 2 * PX, exterior.sizeZ(), EPS);
        assertEquals(-2 * PX, exterior.minY(), EPS);
        assertEquals(interior.maxY() + 2 * PX, exterior.maxY(), EPS);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void partsStayOnPixelGridAndInsideExterior(CarModel model) {
        Box exterior = CarGeometry.exterior(model);
        for (Part part : CarGeometry.parts(model)) {
            Box b = part.box();
            for (double v : new double[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}) {
                assertEquals(Math.round(v * 16), v * 16, EPS, "not on the pixel grid: " + part);
            }
            assertTrue(b.minX() >= exterior.minX() && b.maxX() <= exterior.maxX()
                    && b.minY() >= exterior.minY() && b.maxY() <= exterior.maxY()
                    && b.minZ() >= exterior.minZ() && b.maxZ() <= exterior.maxZ(), "outside: " + part);
            assertFalse(b.intersects(CarGeometry.interior(model)), "intrudes into the interior: " + part);
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void partsDoNotOverlap(CarModel model) {
        List<Part> parts = CarGeometry.parts(model);
        for (int i = 0; i < parts.size(); i++) {
            for (int j = i + 1; j < parts.size(); j++) {
                assertFalse(parts.get(i).box().intersects(parts.get(j).box()), parts.get(i) + " / " + parts.get(j));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void frontWallLeavesEntranceOpen(CarModel model) {
        List<Part> front = CarGeometry.parts(model).stream()
                .filter(p -> (p.type() == PartType.FRONT_WALL || p.type() == PartType.TRANSOM) && p.box().minZ() >= CarGeometry.interior(model).maxZ())
                .toList();
        assertEquals(3, front.size());

        double frontArea = CarGeometry.exterior(model).sizeX() * CarGeometry.interior(model).sizeY();
        double wallArea = front.stream().mapToDouble(p -> p.box().sizeX() * p.box().sizeY()).sum();
        double entranceArea = model.entranceWidthPx() * PX * CarModel.ENTRANCE_HEIGHT_PX * PX;
        assertEquals(frontArea - entranceArea, wallArea, EPS);

        // トランサムの高さは 5px（実機 200 mm）
        List<Part> transoms = front.stream().filter(p -> p.type() == PartType.TRANSOM).toList();
        assertEquals(1, transoms.size());
        Box transom = transoms.get(0).box();
        assertTrue(transom.minY() > 0);
        assertEquals(5 * PX, transom.sizeY(), EPS);
        assertEquals(model.entranceWidthPx() * PX, transom.sizeX(), EPS);
    }

    @Test
    void rotationTurnsFrontSouthWestNorthEast() {
        // 正面（+Z）に置いた小さな箱が、回転ごとに南→西→北→東を向く
        Box front = new Box(-0.5, 0, 1, 0.5, 1, 2);
        assertEquals(front, front.rotateQuarterTurns(0));
        assertEquals(new Box(-2, 0, -0.5, -1, 1, 0.5), front.rotateQuarterTurns(1));
        assertEquals(new Box(-0.5, 0, -2, 0.5, 1, -1), front.rotateQuarterTurns(2));
        assertEquals(new Box(1, 0, -0.5, 2, 1, 0.5), front.rotateQuarterTurns(3));
        assertEquals(front, front.rotateQuarterTurns(4));
        assertEquals(front.rotateQuarterTurns(3), front.rotateQuarterTurns(-1));
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void observationSplitsTheBackWallIntoWaistAndGlass(CarModel model) {
        CarSpec spec = CarSpec.of(model);
        List<Part> plain = CarGeometry.parts(spec, false);
        List<Part> observation = CarGeometry.parts(spec, false, true);
        assertEquals(plain.size() + 1, observation.size());
        List<Part> glass = observation.stream().filter(p -> p.type() == PartType.GLASS).toList();
        assertEquals(1, glass.size());
        Box back = plain.get(4).box();
        Box waist = observation.get(4).box();
        assertEquals(CarGeometry.WAIST_PX / 16.0, waist.maxY(), 1e-12);
        assertEquals(waist.maxY(), glass.get(0).box().minY(), 1e-12);
        assertEquals(back.maxY(), glass.get(0).box().maxY(), 1e-12);
        assertEquals(back.minZ(), glass.get(0).box().minZ(), 1e-12);
        // 二方向出入口（前後）とは組み合わせない
        assertEquals(CarGeometry.parts(spec, true), CarGeometry.parts(spec, true, true));
    }
}
