package net.lausiv1024.realelevator.core.display;

import static net.lausiv1024.realelevator.core.control.TravelDirection.DOWN;
import static net.lausiv1024.realelevator.core.control.TravelDirection.NONE;
import static net.lausiv1024.realelevator.core.control.TravelDirection.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.HallLantern.Section;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

class HallLanternTest {
    private static final double EPS = 1e-9;

    @Test
    void middleFloorsLightTheTravelDirection() {
        assertEquals(Set.of(UP), HallLantern.litSections(2, 5, UP, true));
        assertEquals(Set.of(DOWN), HallLantern.litSections(2, 5, DOWN, true));
        assertEquals(Set.of(), HallLantern.litSections(2, 5, NONE, true));
    }

    @Test
    void terminalFloorsLightBothSections() {
        assertEquals(Set.of(UP, DOWN), HallLantern.litSections(0, 5, UP, true));
        assertEquals(Set.of(UP, DOWN), HallLantern.litSections(4, 5, NONE, true));
    }

    @Test
    void lanternIsOffWhileTheDoorIsClosed() {
        for (TravelDirection direction : TravelDirection.values()) {
            assertEquals(Set.of(), HallLantern.litSections(0, 5, direction, false));
            assertEquals(Set.of(), HallLantern.litSections(2, 5, direction, false));
        }
    }

    @Test
    void blinksOnForThreeQuartersAndOffForHalfASecond() {
        assertEquals(15, HallLantern.BLINK_ON_TICKS);
        assertEquals(10, HallLantern.BLINK_OFF_TICKS);
        assertTrue(HallLantern.isBlinkOn(0));
        assertTrue(HallLantern.isBlinkOn(14.9));
        assertFalse(HallLantern.isBlinkOn(15));
        assertFalse(HallLantern.isBlinkOn(24.9));
        assertTrue(HallLantern.isBlinkOn(25));
        assertTrue(HallLantern.isBlinkOn(39.5));
        assertFalse(HallLantern.isBlinkOn(40));
    }

    @Test
    void chimeWaitsUntilSixSecondsBeforeArrivalAndCertainty() {
        assertEquals(120, HallLantern.CHIME_LEAD_TICKS);
        assertFalse(HallLantern.shouldChime(true, 121));
        assertTrue(HallLantern.shouldChime(true, 120));
        assertTrue(HallLantern.shouldChime(true, 3));
        assertFalse(HallLantern.shouldChime(false, 3));
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void besideTheDoorIsOneBarAtTerminalFloors(CarModel model) {
        FixtureConfig config = FixtureConfig.DEFAULT.withHallLantern(FixtureConfig.HallLanternPlacement.BESIDE_DOOR);
        List<Section> middle = HallLantern.sections(model, config, false);
        List<Section> terminal = HallLantern.sections(model, config, true);
        assertEquals(1, terminal.size());
        Box bar = terminal.get(0).box();
        // 上下の段と段の間をすべて覆う1本
        assertEquals(middle.get(1).box().minY(), bar.minY(), EPS);
        assertEquals(middle.get(0).box().maxY(), bar.maxY(), EPS);
        assertTrue(terminal.get(0).isLit(Set.of(DOWN)) && terminal.get(0).isLit(Set.of(UP)));
        assertFalse(terminal.get(0).isLit(Set.of()));
        // 扉の上の横長は、最上階・最下階でも2段のまま
        assertEquals(2, HallLantern.sections(model, FixtureConfig.DEFAULT.withHallLantern(FixtureConfig.HallLanternPlacement.OVER_DOOR), true).size());
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void besideTheDoorIsAVerticalBarOppositeTheButtons(CarModel model) {
        FixtureConfig config = FixtureConfig.DEFAULT.withHallLantern(FixtureConfig.HallLanternPlacement.BESIDE_DOOR);
        List<Section> sections = HallLantern.sections(model, config, false);
        Box up = sections.get(0).box();
        Box down = sections.get(1).box();
        assertEquals(Set.of(UP), sections.get(0).directions());
        // 縦長で、上の段が上り
        assertTrue(up.sizeY() > up.sizeX() && down.sizeY() > down.sizeX());
        assertTrue(up.minY() > down.maxY());
        assertEquals(up.minX(), down.minX(), EPS);
        // 左側（ボタンと反対側）の三方枠の脇
        assertTrue(up.minX() >= -DoorGeometry.LANDING_FRAME_HALF_WIDTH && up.maxX() <= CarGeometry.entrance(model).minX());
        assertTrue(up.maxX() < ButtonLayout.hallButtons(model, 1, 3).get(0).box().minX());
        assertEquals(DoorGeometry.landingFace(model), up.minZ(), EPS);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void overTheDoorSpansTheEntranceAboveTheIndicator(CarModel model) {
        for (FixtureConfig.HallIndicator placement : FixtureConfig.HallIndicator.values()) {
            FixtureConfig config = FixtureConfig.DEFAULT.withHallIndicator(placement)
                    .withHallLantern(FixtureConfig.HallLanternPlacement.OVER_DOOR);
            List<Section> sections = HallLantern.sections(model, config, false);
            Box up = sections.get(0).box();
            Box down = sections.get(1).box();
            assertEquals(Set.of(UP), sections.get(0).directions());
            assertEquals(Set.of(DOWN), sections.get(1).directions());
            Box entrance = CarGeometry.entrance(model);
            assertEquals(entrance.minX(), up.minX(), EPS);
            assertEquals(entrance.maxX(), up.maxX(), EPS);
            assertEquals(down.maxY(), up.minY(), EPS);
            assertTrue(down.minY() > entrance.maxY());
            if (placement == FixtureConfig.HallIndicator.OVER_DOOR) {
                assertTrue(down.minY() > FloorIndicator.hall(model, config, "", NONE).face().maxY());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void everyHallFixtureFitsOnTheFrameFace(CarModel model) {
        double half = DoorGeometry.LANDING_FRAME_HALF_WIDTH;
        double top = DoorGeometry.LANDING_FRAME_HEIGHT;
        for (FixtureConfig.HallIndicator indicator : FixtureConfig.HallIndicator.values()) {
            for (FixtureConfig.HallLanternPlacement lantern : FixtureConfig.HallLanternPlacement.values()) {
                FixtureConfig config = FixtureConfig.DEFAULT.withHallIndicator(indicator).withHallLantern(lantern);
                List<Box> boxes = new ArrayList<>();
                boxes.add(FloorIndicator.hall(model, config, "12", UP).face());
                HallLantern.sections(model, config, false).forEach(s -> boxes.add(s.box()));
                HallLantern.sections(model, config, true).forEach(s -> boxes.add(s.box()));
                ButtonLayout.hallButtons(model, 1, 3).forEach(b -> boxes.add(b.box()));
                for (Box box : boxes) {
                    assertTrue(box.minX() >= -half - EPS && box.maxX() <= half + EPS && box.minY() >= 0 && box.maxY() <= top + EPS,
                            indicator + " / " + lantern + ": " + box);
                    // 出入口の前にはかからない
                    Box entrance = CarGeometry.entrance(model);
                    boolean overEntrance = box.maxX() > entrance.minX() && box.minX() < entrance.maxX() && box.minY() < entrance.maxY();
                    assertFalse(overEntrance, indicator + " / " + lantern + ": " + box);
                }
            }
        }
    }

    @Test
    void noLanternHasNoSections() {
        FixtureConfig config = FixtureConfig.DEFAULT.withHallLantern(FixtureConfig.HallLanternPlacement.NONE);
        assertTrue(HallLantern.sections(CarModel.P11, config, false).isEmpty());
        assertTrue(HallLantern.sections(CarModel.P11, config, true).isEmpty());
    }
}
