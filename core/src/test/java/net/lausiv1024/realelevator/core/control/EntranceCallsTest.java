package net.lausiv1024.realelevator.core.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.car.Entrance;

class EntranceCallsTest {
    private static final Set<Entrance> BOTH = EnumSet.allOf(Entrance.class);

    @Test
    void aHallCallOpensOnlyTheSideWhereItWasPressed() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(2, TravelDirection.UP, Entrance.REAR);
        assertTrue(calls.isRegistered(2, TravelDirection.UP, Entrance.REAR));
        assertFalse(calls.isRegistered(2, TravelDirection.UP, Entrance.FRONT));
        assertEquals(EnumSet.of(Entrance.REAR), calls.arrive(2, false, TravelDirection.UP, BOTH));
        assertFalse(calls.isRegistered(2, TravelDirection.UP, Entrance.REAR), "the answered call should be cleared");
    }

    @Test
    void aCarCallOpensEveryDoorOfTheFloor() {
        EntranceCalls calls = new EntranceCalls();
        assertEquals(BOTH, calls.arrive(1, true, TravelDirection.NONE, BOTH));
        assertEquals(EnumSet.of(Entrance.FRONT), calls.arrive(1, true, TravelDirection.NONE, EnumSet.of(Entrance.FRONT)));
    }

    @Test
    void callsInTheOtherDirectionStay() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(3, TravelDirection.UP, Entrance.FRONT);
        calls.press(3, TravelDirection.DOWN, Entrance.REAR);
        assertEquals(EnumSet.of(Entrance.FRONT), calls.arrive(3, false, TravelDirection.UP, BOTH));
        assertTrue(calls.isRegistered(3, TravelDirection.DOWN, Entrance.REAR));
    }

    @Test
    void snapshotsRoundTrip() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(4, TravelDirection.DOWN, Entrance.REAR);
        EntranceCalls copy = new EntranceCalls();
        copy.restore(calls.snapshot());
        assertTrue(copy.isRegistered(4, TravelDirection.DOWN, Entrance.REAR));
    }
}
