package net.lausiv1024.realelevator.core.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
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
        assertEquals(List.of(Entrance.REAR), calls.arrive(2, false, TravelDirection.UP, BOTH));
        assertFalse(calls.isRegistered(2, TravelDirection.UP, Entrance.REAR), "the answered call should be cleared");
    }

    @Test
    void aCarCallOpensTheSideOfThePanelThatWasPressed() {
        EntranceCalls calls = new EntranceCalls();
        calls.pressCar(1, Entrance.REAR);
        assertTrue(calls.isCarRegistered(1, Entrance.REAR) && !calls.isCarRegistered(1, Entrance.FRONT));
        assertEquals(List.of(Entrance.REAR), calls.arrive(1, true, TravelDirection.NONE, BOTH));
        assertFalse(calls.isCarRegistered(1, Entrance.REAR));
    }

    @Test
    void aCarCallFromTheOtherPanelOpensTheDoorThatTheFloorHas() {
        EntranceCalls calls = new EntranceCalls();
        calls.pressCar(1, Entrance.REAR);
        assertEquals(List.of(Entrance.FRONT), calls.arrive(1, true, TravelDirection.NONE, EnumSet.of(Entrance.FRONT)));
    }

    @Test
    void bothSidesOpenOneAfterAnotherInTheOrderTheyWereCalled() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(3, TravelDirection.DOWN, Entrance.REAR);
        calls.pressCar(3, Entrance.FRONT);
        assertEquals(List.of(Entrance.REAR, Entrance.FRONT), calls.arrive(3, true, TravelDirection.DOWN, BOTH));
        // 先に開く奥の記録だけ消え、あとから開く正面の応答灯は点いたまま
        assertFalse(calls.isRegistered(3, TravelDirection.DOWN, Entrance.REAR));
        assertTrue(calls.isCarRegistered(3, Entrance.FRONT));
        calls.served(3, Entrance.FRONT, true, TravelDirection.DOWN, BOTH);
        assertFalse(calls.isCarRegistered(3, Entrance.FRONT));
        // 順番を忘れたあとは、新しく呼ばれた順
        calls.pressCar(3, Entrance.FRONT);
        calls.press(3, TravelDirection.DOWN, Entrance.REAR);
        assertEquals(List.of(Entrance.FRONT, Entrance.REAR), calls.arrive(3, true, TravelDirection.DOWN, BOTH));
    }

    @Test
    void callsInTheOtherDirectionStay() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(3, TravelDirection.UP, Entrance.FRONT);
        calls.press(3, TravelDirection.DOWN, Entrance.REAR);
        assertEquals(List.of(Entrance.FRONT), calls.arrive(3, false, TravelDirection.UP, BOTH));
        assertTrue(calls.isRegistered(3, TravelDirection.DOWN, Entrance.REAR));
    }

    @Test
    void withoutARecordTheFloorsDoorsOpen() {
        EntranceCalls calls = new EntranceCalls();
        assertEquals(List.of(Entrance.FRONT), calls.arrive(0, true, TravelDirection.NONE, EnumSet.of(Entrance.FRONT)));
    }

    @Test
    void snapshotsRoundTrip() {
        EntranceCalls calls = new EntranceCalls();
        calls.press(4, TravelDirection.DOWN, Entrance.REAR);
        calls.pressCar(4, Entrance.FRONT);
        EntranceCalls copy = new EntranceCalls();
        copy.restore(calls.snapshot());
        assertTrue(copy.isRegistered(4, TravelDirection.DOWN, Entrance.REAR) && copy.isCarRegistered(4, Entrance.FRONT));
        assertEquals(List.of(Entrance.REAR, Entrance.FRONT), copy.arrive(4, true, TravelDirection.DOWN, BOTH));
        assertTrue(copy.isCarRegistered(4, Entrance.FRONT), "the front waits with its lamp lit");
    }
}
