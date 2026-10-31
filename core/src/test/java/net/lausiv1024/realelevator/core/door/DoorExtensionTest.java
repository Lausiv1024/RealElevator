package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.control.CallType;

/** 開延長（{@link DoorController#extend}）。 */
class DoorExtensionTest {
    private static final DoorTiming TIMING = DoorTiming.STANDARD;
    private static final int DWELL = TIMING.dwellTicks(CallType.CAR);

    private static void run(DoorController door, long start, long end) {
        for (long t = start; t <= end; t++) {
            door.tick(t);
        }
    }

    /** 時刻 0 に開き始めて、時刻 openTicks に全開になった戸。 */
    private static DoorController opened() {
        DoorController door = new DoorController(TIMING);
        door.open(0, DWELL);
        run(door, 1, TIMING.openTicks());
        assertEquals(DoorState.OPEN, door.state());
        return door;
    }

    @Test
    void stayOpenForTheExtensionAndThenCloseWithoutNudging() {
        DoorController door = opened();
        long start = TIMING.openTicks();
        door.extend(start);
        assertTrue(door.isExtended());

        run(door, start + 1, start + DoorController.EXTENSION_TICKS - 1);
        assertEquals(DoorState.OPEN, door.state());

        // 延長が終わると通常どおり閉める（開いてから60秒を超えていても強制戸閉にしない）
        door.tick(start + DoorController.EXTENSION_TICKS);
        assertFalse(door.isExtended());
        assertEquals(DoorState.CLOSING, door.state());
        assertFalse(door.isNudging());
    }

    @Test
    void extensionCanOutlastTheNudgingTimer() {
        DoorController door = opened();
        // 強制戸閉の直前に延長しても、延長の時間いっぱいまで開いている
        long start = DoorController.NUDGE_OPEN_TICKS - 10;
        for (long t = TIMING.openTicks() + 1; t < start; t++) {
            door.hold(t);
            door.tick(t);
        }
        door.extend(start);
        run(door, start + 1, start + DoorController.EXTENSION_TICKS - 1);
        assertEquals(DoorState.OPEN, door.state());
        assertFalse(door.isNudging());
    }

    @Test
    void closeButtonCancelsTheExtension() {
        DoorController door = opened();
        long start = TIMING.openTicks();
        door.extend(start);
        door.close(start + 5);
        assertFalse(door.isExtended());
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void cancelWhileOpeningFallsBackToTheUsualDwell() {
        DoorController door = new DoorController(TIMING);
        door.open(0, DWELL);
        door.extend(0);
        door.cancelExtension();
        run(door, 1, TIMING.openTicks() + DWELL);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void theExtensionCountsFromFullyOpen() {
        DoorController door = new DoorController(TIMING);
        door.open(0, DWELL);
        door.extend(0);
        run(door, 1, TIMING.openTicks() + DoorController.EXTENSION_TICKS - 1);
        assertEquals(DoorState.OPEN, door.state());
        door.tick(TIMING.openTicks() + DoorController.EXTENSION_TICKS);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void openAndBeamDoNotChangeTheExtension() {
        DoorController door = opened();
        long start = TIMING.openTicks();
        door.extend(start);
        door.open(start + 100, DWELL);
        door.hold(start + 200);
        run(door, start + 1, start + DoorController.EXTENSION_TICKS - 1);
        assertEquals(DoorState.OPEN, door.state());
        door.tick(start + DoorController.EXTENSION_TICKS);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void extensionIsRefusedWhileNudging() {
        DoorController door = opened();
        door.restore(DoorState.CLOSING, 100, 1, DWELL, false, true, false);
        door.extend(101);
        assertFalse(door.isExtended());
    }
}
