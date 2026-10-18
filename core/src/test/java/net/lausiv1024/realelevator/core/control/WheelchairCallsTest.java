package net.lausiv1024.realelevator.core.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WheelchairCallsTest {
    @Test
    void aWheelchairCarCallExtendsTheStopAndIsCleared() {
        WheelchairCalls calls = new WheelchairCalls();
        calls.pressCar(2);
        assertFalse(calls.arrive(1, true, TravelDirection.NONE), "another floor");
        assertTrue(calls.arrive(2, true, TravelDirection.NONE));
        assertFalse(calls.isCarRegistered(2));
        assertFalse(calls.arrive(2, true, TravelDirection.NONE), "an ordinary call afterwards");
    }

    @Test
    void onlyTheAnsweredHallDirectionCounts() {
        WheelchairCalls calls = new WheelchairCalls();
        calls.press(3, TravelDirection.DOWN);
        assertFalse(calls.arrive(3, false, TravelDirection.UP));
        assertTrue(calls.isRegistered(3, TravelDirection.DOWN));
        assertTrue(calls.arrive(3, false, TravelDirection.DOWN));
        assertFalse(calls.isRegistered(3, TravelDirection.DOWN));
    }

    @Test
    void snapshotsRoundTrip() {
        WheelchairCalls calls = new WheelchairCalls();
        calls.pressCar(1);
        calls.press(4, TravelDirection.UP);
        WheelchairCalls copy = new WheelchairCalls();
        long[][] snapshot = calls.snapshot();
        copy.restore(snapshot[0], snapshot[1], snapshot[2]);
        assertTrue(copy.isCarRegistered(1) && copy.isRegistered(4, TravelDirection.UP));
    }
}
