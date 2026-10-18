package net.lausiv1024.realelevator.core.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.motion.Trip;

class CancelReplanTest {
    /** 5ブロックおきの11階床。 */
    private static final int[] FLOORS = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50};

    private static CollectiveControl control(int... carCalls) {
        CollectiveControl control = new CollectiveControl(FLOORS.length);
        for (int floor : carCalls) {
            control.pressCar(floor, 0, true);
        }
        return control;
    }

    private static Trip tripTo8() {
        return new Trip(0, 0, 40, SpeedSpec.NORMAL_LOW);
    }

    private static long tickAt(Trip trip, double position) {
        long tick = 0;
        while (trip.positionAt(tick) < position) {
            tick++;
        }
        return tick;
    }

    @Test
    void anotherCallAtTheTargetKeepsTheStop() {
        CollectiveControl control = control(8);
        control.pressHall(8, TravelDirection.UP, 0, false);
        control.cancelCar(8, false);
        Trip trip = tripTo8();
        CancelReplan.Outcome outcome = CancelReplan.replan(control, trip, 100, FLOORS, 0, 8);
        assertEquals(8, outcome.targetFloor());
        assertSame(trip, outcome.trip());
        assertTrue(outcome.openDoors());
    }

    @Test
    void beforeBrakingTheTripExtendsToTheNextCall() {
        CollectiveControl control = control(8, 10);
        control.cancelCar(8, false);
        CancelReplan.Outcome outcome = CancelReplan.replan(control, tripTo8(), 100, FLOORS, 0, 8);
        assertEquals(10, outcome.targetFloor());
        assertEquals(50, outcome.trip().to());
        assertTrue(outcome.openDoors());
    }

    @Test
    void whileBrakingTheCarStopsThereWithoutOpening() {
        CollectiveControl control = control(8, 10);
        control.cancelCar(8, false);
        Trip trip = tripTo8();
        CancelReplan.Outcome outcome = CancelReplan.replan(control, trip, tickAt(trip, 39), FLOORS, 0, 8);
        assertEquals(8, outcome.targetFloor());
        assertSame(trip, outcome.trip());
        assertFalse(outcome.openDoors());
    }

    @Test
    void withNoCallsLeftTheCarStopsAtTheNearestFloorItCanWithoutOpening() {
        CollectiveControl control = control(8);
        control.cancelCar(8, false);
        Trip trip = tripTo8();
        long tick = tickAt(trip, 12);
        CancelReplan.Outcome outcome = CancelReplan.replan(control, trip, tick, FLOORS, 0, 8);
        assertFalse(outcome.openDoors());
        assertTrue(outcome.targetFloor() > 2 && outcome.targetFloor() < 8, "stopped at " + outcome.targetFloor());
        assertEquals(FLOORS[outcome.targetFloor()], outcome.trip().to());
        // 止まれる最初の階：その手前の階では止まれない
        assertEquals(null, trip.retarget(tick, FLOORS[outcome.targetFloor() - 1]));
    }

    @Test
    void cancellingWhileStoppedClearsTheDirectionOnlyWhenNothingIsLeft() {
        CollectiveControl control = control(3, 5);
        assertTrue(control.cancelCar(3, true));
        assertEquals(TravelDirection.UP, control.direction());
        assertFalse(control.cancelCar(3, true), "already cancelled");
        assertTrue(control.cancelCar(5, true));
        assertEquals(TravelDirection.NONE, control.direction());
    }
}
