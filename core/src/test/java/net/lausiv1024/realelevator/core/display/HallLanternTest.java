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
}
