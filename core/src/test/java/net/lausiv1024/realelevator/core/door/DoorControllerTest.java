package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.control.CallType;

class DoorControllerTest {
    private static final DoorTiming TIMING = DoorTiming.STANDARD;
    private static final double EPS = 1e-9;

    /** start から end まで1tickずつ進める。 */
    private static void run(DoorController door, long start, long end) {
        for (long t = start; t <= end; t++) {
            door.tick(t);
        }
    }

    @Test
    void standardTimingMatchesSpec() {
        assertEquals(40, TIMING.openTicks());
        assertEquals(50, TIMING.closeTicks());
        assertEquals(80, TIMING.dwellTicks(CallType.HALL));
        assertEquals(40, TIMING.dwellTicks(CallType.CAR));
    }

    @Test
    void fullCycleOpensWaitsAndCloses() {
        DoorController door = new DoorController(TIMING);
        assertTrue(door.isClosed());
        assertEquals(0, door.fractionAt(0));

        door.open(100, TIMING.dwellTicks(CallType.CAR));
        assertEquals(DoorState.OPENING, door.state());
        assertEquals(0.5, door.fractionAt(120), EPS);

        run(door, 101, 139);
        assertEquals(DoorState.OPENING, door.state());
        assertTrue(door.tick(140));
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(1, door.fractionAt(140));

        run(door, 141, 179);
        assertEquals(DoorState.OPEN, door.state());
        assertTrue(door.tick(180));
        assertEquals(DoorState.CLOSING, door.state());
        assertEquals(0.5, door.fractionAt(205), EPS);

        run(door, 181, 229);
        assertEquals(DoorState.CLOSING, door.state());
        assertTrue(door.tick(230));
        assertTrue(door.isClosed());
        assertEquals(0, door.fractionAt(230));
    }

    @Test
    void hallCallDwellsLongerThanCarCall() {
        DoorController hall = new DoorController(TIMING);
        DoorController car = new DoorController(TIMING);
        hall.open(0, TIMING.dwellTicks(CallType.HALL));
        car.open(0, TIMING.dwellTicks(CallType.CAR));
        run(hall, 1, 100);
        run(car, 1, 100);
        assertEquals(DoorState.OPEN, hall.state());
        assertEquals(DoorState.CLOSING, car.state());
    }

    @Test
    void closeButtonCutsDwellShort() {
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.HALL));
        run(door, 1, 45);
        assertEquals(DoorState.OPEN, door.state());
        door.close(45);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void closeButtonIsIgnoredWhileOpening() {
        DoorController door = new DoorController(TIMING);
        door.open(0, 40);
        door.close(10);
        assertEquals(DoorState.OPENING, door.state());
    }

    @Test
    void openButtonWhileOpenRestartsDwell() {
        DoorController door = new DoorController(TIMING);
        door.open(0, 40);
        run(door, 1, 70);
        door.open(70, 40);
        run(door, 71, 109);
        assertEquals(DoorState.OPEN, door.state());
        run(door, 110, 110);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void obstructionReversesFromCurrentPosition() {
        DoorController door = new DoorController(TIMING);
        door.open(0, 40);
        run(door, 1, 80);
        assertEquals(DoorState.CLOSING, door.state());

        double before = door.fractionAt(95);
        door.obstruct(95);
        assertEquals(DoorState.OPENING, door.state());
        // 反転の瞬間に戸が飛ばない
        assertEquals(before, door.fractionAt(95), EPS);
        assertTrue(door.fractionAt(96) > before);

        // 残りの分だけの時間で全開になり、元の待機時間で待つ
        long reopenTicks = (long) Math.ceil((1 - before) * TIMING.openTicks());
        run(door, 96, 95 + reopenTicks);
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(40, door.dwellTicks());
    }

    @Test
    void obstructionIsIgnoredUnlessClosing() {
        DoorController door = new DoorController(TIMING);
        door.obstruct(0);
        assertTrue(door.isClosed());
        door.open(0, 40);
        door.obstruct(10);
        assertEquals(DoorState.OPENING, door.state());
    }

    @Test
    void fractionMovesSmoothlyAndMonotonically() {
        DoorController door = new DoorController(TIMING);
        door.open(0, 40);
        double previous = 0;
        for (int i = 1; i <= 400; i++) {
            double fraction = door.fractionAt(i / 10.0);
            assertTrue(fraction >= previous, "moved backwards at " + i);
            assertTrue(fraction - previous < 0.01, "jumped at " + i);
            previous = fraction;
        }
        assertEquals(1, previous, EPS);
        // 動き始めはゆっくり
        assertTrue(door.fractionAt(1) < 1.0 / TIMING.openTicks());
    }

    @Test
    void restoreReproducesFraction() {
        DoorController server = new DoorController(TIMING);
        server.open(0, 40);
        run(server, 1, 90);
        DoorController client = new DoorController(TIMING);
        client.restore(server.state(), server.startTick(), server.startFraction(), server.dwellTicks());
        for (double t = 90; t < 140; t += 0.25) {
            assertEquals(server.fractionAt(t), client.fractionAt(t), EPS);
        }
        assertFalse(client.isClosed());
    }
}
