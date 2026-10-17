package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry.Side;

class DoorGeometryTest {
    private static final double EPS = 1e-12;
    private static final double PX = 1.0 / 16;

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void entranceIsCenteredAndSplitIntoEqualPanels(CarModel model) {
        Box entrance = CarGeometry.entrance(model);
        assertEquals(-entrance.minX(), entrance.maxX(), EPS);
        assertEquals(0, model.entranceWidthPx() % 2);
        assertEquals(3.125, entrance.sizeY(), EPS);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void closedPanelsFillTheEntranceExactly(CarModel model) {
        Box entrance = CarGeometry.entrance(model);
        for (Side side : Side.values()) {
            List<Box> panels = DoorGeometry.panels(model, side, 0);
            assertEquals(entrance.minX(), panels.get(0).minX(), EPS);
            assertEquals(panels.get(0).maxX(), panels.get(1).minX(), EPS);
            assertEquals(entrance.maxX(), panels.get(1).maxX(), EPS);
            assertEquals(entrance.maxY(), panels.get(0).maxY(), EPS);
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void openPanelsClearTheEntrance(CarModel model) {
        Box entrance = CarGeometry.entrance(model);
        for (Side side : Side.values()) {
            List<Box> panels = DoorGeometry.panels(model, side, 1);
            assertEquals(entrance.minX(), panels.get(0).maxX(), EPS);
            assertEquals(entrance.maxX(), panels.get(1).minX(), EPS);
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void panelsSitOutsideTheFrontWallInOrder(CarModel model) {
        Box carPanel = DoorGeometry.panels(model, Side.CAR, 0.3).get(0);
        Box landingPanel = DoorGeometry.panels(model, Side.LANDING, 0.3).get(0);
        assertEquals(CarGeometry.exterior(model).maxZ(), carPanel.minZ(), EPS);
        assertEquals(PX, carPanel.sizeZ(), EPS);
        assertEquals(carPanel.maxZ() + PX, landingPanel.minZ(), EPS);
        for (CarGeometry.Part part : CarGeometry.parts(model)) {
            assertFalse(part.box().intersects(carPanel), "overlaps " + part);
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void travelBoundsCoverEveryPosition(CarModel model) {
        Box bounds = DoorGeometry.travelBounds(model, Side.LANDING);
        for (int i = 0; i <= 10; i++) {
            for (Box panel : DoorGeometry.panels(model, Side.LANDING, i / 10.0)) {
                assertTrue(panel.minX() >= bounds.minX() - EPS && panel.maxX() <= bounds.maxX() + EPS
                        && panel.minZ() >= bounds.minZ() - EPS && panel.maxZ() <= bounds.maxZ() + EPS);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void landingFrameFillsTheWallAroundTheEntrance(CarModel model) {
        Box entrance = CarGeometry.entrance(model);
        Box travel = DoorGeometry.travelBounds(model, Side.LANDING);
        List<Box> frame = DoorGeometry.landingFrame(model);
        double face = DoorGeometry.landingFace(model);
        // 乗場の壁の列は1ブロックの厚みで、乗場の戸はその昇降路側の面にある
        assertEquals(travel.minZ() + 1, face, EPS);
        double area = 0;
        for (Box box : frame) {
            assertEquals(travel.maxZ(), box.minZ(), EPS);
            assertEquals(face, box.maxZ(), EPS);
            // 戸（全閉から全開まで）とも出入口とも重ならない
            for (int i = 0; i <= 10; i++) {
                for (Box panel : DoorGeometry.panels(model, Side.LANDING, i / 10.0)) {
                    assertFalse(box.intersects(panel));
                }
            }
            assertFalse(box.intersects(new Box(entrance.minX(), 0, travel.maxZ(), entrance.maxX(), entrance.maxY(), face)));
            area += box.sizeX() * box.sizeY();
        }
        // 幅3ブロック・高さ4ブロックから出入口を除いた面積をちょうど埋める
        assertEquals(3 * 4 - entrance.sizeX() * entrance.maxY(), area, EPS);
    }
}
