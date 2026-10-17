package net.lausiv1024.realelevator.core.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

class ShaftLayoutTest {
    private static final double EPS = 1e-9;

    @Test
    void floorChecks() {
        assertEquals(FloorProblem.TOO_FEW, ShaftLayout.check(new int[] {0}));
        assertEquals(FloorProblem.TOO_CLOSE, ShaftLayout.check(new int[] {0, 3}));
        assertEquals(FloorProblem.BELOW_PANEL, ShaftLayout.check(new int[] {-5, 0}));
        assertEquals(FloorProblem.NONE, ShaftLayout.check(new int[] {9, 0, 4}));
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void landingDoorSitsOnTheBackOfTheWallRow(CarModel model) {
        ShaftLayout layout = new ShaftLayout(model, PitMode.REAL, new int[] {0, 5});
        double landingDoorBack = layout.carZ() + DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING).minZ();
        assertEquals(-0.5, landingDoorBack, EPS);
        // 三方枠の乗場側の面は、壁の列（制御盤の列）の乗場側の面とそろう
        assertEquals(0.5, layout.carZ() + DoorGeometry.landingFace(model), EPS);
        assertEquals(ShaftLayout.CAR_COLUMN, layout.carX(), EPS);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void theCarFitsInsideTheClearShaftAtEveryFloor(CarModel model) {
        for (PitMode pit : PitMode.values()) {
            ShaftLayout layout = new ShaftLayout(model, pit, new int[] {0, 5, 12});
            Set<Cell> clear = new HashSet<>(layout.requiredClearCells());
            Box exterior = CarGeometry.exterior(model);
            for (int floor : layout.floors()) {
                Box car = exterior.move(layout.carX(), floor, layout.carZ());
                for (int x = (int) Math.floor(car.minX() + 0.5); x <= (int) Math.floor(car.maxX() - 1e-9 + 0.5); x++) {
                    for (int y = (int) Math.floor(car.minY()); y < Math.ceil(car.maxY()); y++) {
                        for (int z = (int) Math.floor(car.minZ() + 0.5); z <= (int) Math.floor(car.maxZ() - 1e-9 + 0.5); z++) {
                            assertTrue(clear.contains(new Cell(x, y, z)), "the car overlaps " + new Cell(x, y, z) + " at " + floor);
                        }
                    }
                }
            }
            // 釣合い重りの分、かごの奥にもう1列ある
            double carBack = layout.carZ() + exterior.minZ();
            assertTrue(layout.backRow() + 0.5 < carBack);
            assertEquals(-pit.pit, layout.bottom());
            assertEquals(12 + pit.overhead, layout.top());
            assertTrue(layout.top() - 12 > exterior.maxY());
        }
    }

    @Test
    void frameCellsAreInTheWallRowOfEveryFloor() {
        ShaftLayout layout = new ShaftLayout(CarModel.P11, PitMode.COMPACT, new int[] {0, 5});
        List<Cell> cells = layout.requiredClearCells();
        for (int floor : new int[] {0, 5}) {
            for (int y = floor; y < floor + 4; y++) {
                for (int x = -3; x <= -1; x++) {
                    assertTrue(cells.contains(new Cell(x, y, 0)));
                }
            }
        }
        // 制御盤の場所と、三方枠の上は空いていなくてよい
        assertFalse(cells.contains(new Cell(0, 0, 0)));
        assertFalse(cells.contains(new Cell(-2, 4, 0)));
    }

    @Test
    void railsRunTheWholeHeightOnBothSides() {
        ShaftLayout layout = new ShaftLayout(CarModel.P11, PitMode.REAL, new int[] {0, 5});
        List<ShaftLayout.Rail> rails = layout.rails();
        // 右のレールは天井の1ブロック下で止める
        assertEquals((layout.top() - layout.bottom()) * 2 - 1, rails.size());
        Set<Cell> clear = new HashSet<>(layout.requiredClearCells());
        for (ShaftLayout.Rail rail : rails) {
            assertTrue(clear.contains(rail.cell()));
            assertEquals(rail.left() ? -3 : -1, rail.cell().x());
        }
    }

    @Test
    void overDoorHallFixturesNeedFiveBlockFloors() {
        assertTrue(ShaftLayout.allowsOverDoorHallFixtures(new int[] {0, 5, 10}));
        assertFalse(ShaftLayout.allowsOverDoorHallFixtures(new int[] {0, 5, 9}));
        FixtureConfig overDoor = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_DOOR)
                .withHallLantern(FixtureConfig.HallLanternPlacement.OVER_DOOR);
        assertEquals(overDoor, ShaftLayout.fitFixtures(overDoor, new int[] {0, 5}));
        FixtureConfig fitted = ShaftLayout.fitFixtures(overDoor, new int[] {0, 4});
        assertEquals(FixtureConfig.HallIndicator.OVER_BUTTONS, fitted.hallIndicator());
        assertEquals(FixtureConfig.HallLanternPlacement.BESIDE_DOOR, fitted.hallLantern());
        // 付けないランタンはそのまま
        assertEquals(FixtureConfig.HallLanternPlacement.NONE,
                ShaftLayout.fitFixtures(FixtureConfig.DEFAULT, new int[] {0, 4}).hallLantern());
    }

    @Test
    void defaultsFollowTheJapaneseStandardSpecification() {
        assertEquals(FixtureConfig.HallIndicator.OVER_BUTTONS, FixtureConfig.DEFAULT.hallIndicator());
        assertEquals(FixtureConfig.HallLanternPlacement.NONE, FixtureConfig.DEFAULT.hallLantern());
        assertFalse(AnnouncementConfig.DEFAULT.chime());
        assertTrue(AnnouncementConfig.DEFAULT.arrival());
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void theControllerSitsInTheBackLeftCornerUnderTheCeiling(CarModel model) {
        for (PitMode pit : PitMode.values()) {
            ShaftLayout layout = new ShaftLayout(model, pit, new int[] {0, 5, 12});
            List<Cell> cells = layout.controllerCells();
            assertEquals(ShaftLayout.CONTROLLER_HEIGHT, cells.size());
            Set<Cell> clear = new HashSet<>(layout.requiredClearCells());
            Set<Cell> rails = new HashSet<>();
            layout.rails().forEach(rail -> rails.add(rail.cell()));
            for (Cell cell : cells) {
                assertTrue(clear.contains(cell), "the controller should be inside the shaft: " + cell);
                assertFalse(rails.contains(cell), "the controller should not replace a rail: " + cell);
                assertEquals(ShaftLayout.CAR_COLUMN - 1, cell.x());
                assertEquals(layout.backRow(), cell.z());
            }
            assertEquals(layout.top() - 1, cells.get(cells.size() - 1).y());
        }
    }
}
