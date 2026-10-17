package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
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
import net.lausiv1024.realelevator.core.door.DoorGeometry.Side;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

/** 扉の方式（CO・2S・4CO）ごとの戸の形。選べるすべての機種と方式の組み合わせで確かめる。 */
class DoorTypeGeometryTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;

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

    @Test
    void fourCenterIsOnlyForLargeModelsAndWidensTheEntrance() {
        assertFalse(DoorType.FOUR_CENTER.isAvailable(CarModel.P15));
        assertFalse(DoorType.FOUR_CENTER.isAvailable(CarModel.P20));
        assertEquals(DoorType.CENTER, new CarSpec(CarModel.P13, DoorType.FOUR_CENTER).door());
        // 実機 1400 mm
        for (CarModel model : new CarModel[] {CarModel.P24, CarModel.P27, CarModel.P30}) {
            CarSpec spec = new CarSpec(model, DoorType.FOUR_CENTER);
            assertEquals(34, spec.entranceWidthPx());
            assertEquals(9, spec.panelWidthPx());
        }
    }

    @Test
    void sideOpeningShiftsTheEntranceAwayFromThePocket() {
        // 仕様の表：P-11（34px、出入口20px）は戸袋の側の袖壁11px、反対側3px
        CarSpec left = new CarSpec(CarModel.P11, DoorType.SIDE_LEFT);
        assertEquals(-17 + 11, left.entranceMinXPx());
        assertEquals(17 - 3, left.entranceMaxXPx());
        CarSpec right = new CarSpec(CarModel.P11, DoorType.SIDE_RIGHT);
        assertEquals(-17 + 3, right.entranceMinXPx());
        assertEquals(17 - 11, right.entranceMaxXPx());
        // P-17（44px、出入口24px）は13pxと7px
        CarSpec wide = new CarSpec(CarModel.P17, DoorType.SIDE_LEFT);
        assertEquals(-22 + 13, wide.entranceMinXPx());
        assertEquals(22 - 7, wide.entranceMaxXPx());
    }

    @ParameterizedTest
    @MethodSource("specs")
    void closedPanelsCloseTheWholeEntrance(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        for (Side side : Side.values()) {
            List<Box> panels = new ArrayList<>(DoorGeometry.panels(spec, side, 0));
            panels.sort(Comparator.comparingDouble(Box::minX));
            assertEquals(entrance.minX(), panels.get(0).minX(), EPS, spec + " " + side);
            assertEquals(entrance.maxX(), panels.get(panels.size() - 1).maxX(), EPS, spec + " " + side);
            // すき間なく並ぶ（重なりは許す）
            for (int i = 1; i < panels.size(); i++) {
                assertTrue(panels.get(i).minX() <= panels.get(i - 1).maxX() + EPS, spec + " " + side + " gap at " + i);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("specs")
    void openPanelsHideBehindTheJambsWithinTheCarWidth(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        Box exterior = CarGeometry.exterior(spec);
        for (Side side : Side.values()) {
            for (Box panel : DoorGeometry.panels(spec, side, 1)) {
                assertTrue(panel.maxX() <= entrance.minX() + EPS || panel.minX() >= entrance.maxX() - EPS,
                        spec + " " + side + " a panel stays in the entrance: " + panel);
                // CO・4COは戸が袖壁より広く、開くと少しかごの横へ出る（実機でも同じ）。2Sは戸袋の側の袖壁を戸1枚分にしてある
                if (spec.door().isSideOpening()) {
                    assertTrue(panel.minX() >= exterior.minX() - EPS && panel.maxX() <= exterior.maxX() + EPS,
                            spec + " " + side + " a panel sticks out of the car: " + panel);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("specs")
    void panelsOnTheSameTrackNeverOverlapAndTheCarAndLandingStayApart(CarSpec spec) {
        for (int i = 0; i <= 20; i++) {
            double f = i / 20.0;
            List<Box> car = DoorGeometry.panels(spec, Side.CAR, f);
            List<Box> landing = DoorGeometry.panels(spec, Side.LANDING, f);
            for (List<Box> panels : List.of(car, landing)) {
                for (int a = 0; a < panels.size(); a++) {
                    for (int b = a + 1; b < panels.size(); b++) {
                        assertFalse(panels.get(a).intersects(panels.get(b)), spec + " panels collide at " + f);
                    }
                }
            }
            double carFront = car.stream().mapToDouble(Box::maxZ).max().orElseThrow();
            double landingBack = landing.stream().mapToDouble(Box::minZ).min().orElseThrow();
            assertEquals(DoorGeometry.SILL_GAP_PX * PX, landingBack - carFront, EPS, spec + " sill gap");
        }
        assertEquals(spec.door().tracks() * PX, DoorGeometry.travelBounds(spec, Side.CAR).sizeZ(), EPS);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void theFastPanelMovesAboutTwiceAsFar(CarSpec spec) {
        if (spec.door() == DoorType.CENTER) {
            return;
        }
        List<Box> closed = DoorGeometry.panels(spec, Side.CAR, 0);
        List<Box> open = DoorGeometry.panels(spec, Side.CAR, 1);
        double slow = Math.abs(open.get(0).minX() - closed.get(0).minX());
        double fast = Math.abs(open.get(1).minX() - closed.get(1).minX());
        assertTrue(fast > slow * 1.7 && fast < slow * 2.1, spec + " slow " + slow + " fast " + fast);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void landingFrameFillsTheWallAroundTheEntrance(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        double area = 0;
        for (Box box : DoorGeometry.landingFrame(spec)) {
            for (int i = 0; i <= 10; i++) {
                for (Box panel : DoorGeometry.panels(spec, Side.LANDING, i / 10.0)) {
                    assertFalse(box.intersects(panel), spec + " the frame hits a panel");
                }
            }
            area += box.sizeX() * box.sizeY();
        }
        assertEquals(spec.shaftWidth() * 4 - entrance.sizeX() * entrance.maxY(), area, EPS);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void carPanelsFitOnTheirJambs(CarSpec spec) {
        for (FixtureConfig.PanelSides sides : FixtureConfig.PanelSides.values()) {
            List<Boolean> jambs = ButtonLayout.panelJambs(spec, sides);
            if (spec.door().isSideOpening()) {
                assertEquals(1, jambs.size(), spec + " side opening cars have one panel");
            }
            for (boolean right : jambs) {
                assertTrue(ButtonLayout.jambWidthPx(spec, right) >= 2 * ButtonLayout.BUTTON_PX + ButtonLayout.GAP_PX,
                        spec + " the panel does not fit on the jamb");
            }
            Box entrance = CarGeometry.entrance(spec);
            for (ButtonLayout.Button button : ButtonLayout.carPanel(spec, 6, sides)) {
                assertTrue(button.box().maxX() <= entrance.minX() + EPS || button.box().minX() >= entrance.maxX() - EPS,
                        spec + " a button is in the entrance");
            }
        }
    }
}
