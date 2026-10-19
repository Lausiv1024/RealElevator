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
        assertEquals(DoorController.Event.STATE_CHANGED, door.tick(140));
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(1, door.fractionAt(140));

        run(door, 141, 179);
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(DoorController.Event.STATE_CHANGED, door.tick(180));
        assertEquals(DoorState.CLOSING, door.state());
        assertEquals(0.5, door.fractionAt(205), EPS);

        run(door, 181, 229);
        assertEquals(DoorState.CLOSING, door.state());
        assertEquals(DoorController.Event.STATE_CHANGED, door.tick(230));
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

    @Test
    void automaticClosingWaitsForTheCloseAnnouncement() {
        DoorController door = new DoorController(TIMING);
        door.setCloseAnnouncementTicks(30);
        door.open(0, 40);
        run(door, 1, 79);
        assertEquals(DoorController.Event.CLOSE_ANNOUNCED, door.tick(80));
        // アナウンスの間は開いたまま
        run(door, 81, 109);
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(DoorController.Event.STATE_CHANGED, door.tick(110));
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void closeButtonDoesNotWaitForTheAnnouncement() {
        DoorController door = new DoorController(TIMING);
        door.setCloseAnnouncementTicks(30);
        door.open(0, 80);
        run(door, 1, 50);
        door.close(50);
        assertEquals(DoorState.CLOSING, door.state());
    }

    @Test
    void openButtonDuringTheAnnouncementCancelsClosing() {
        DoorController door = new DoorController(TIMING);
        door.setCloseAnnouncementTicks(30);
        door.open(0, 40);
        run(door, 1, 90);
        door.open(90, 40);
        // 待っていた閉め始めは取り消され、待機時間を数え直す
        run(door, 91, 129);
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(DoorController.Event.CLOSE_ANNOUNCED, door.tick(130));
    }

    @Test
    void wheelchairCallsWaitLongerAndCloseSlowly() {
        assertEquals(200, TIMING.dwellTicks(CallType.CAR, true));
        assertEquals(200, TIMING.dwellTicks(CallType.HALL, true));
        assertEquals(80, TIMING.dwellTicks(CallType.HALL, false));
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.CAR, true), true);
        run(door, 1, 40);
        assertEquals(DoorState.OPEN, door.state());
        run(door, 41, 239);
        assertEquals(DoorState.OPEN, door.state(), "the dwell should last 10 seconds");
        run(door, 240, 240);
        assertEquals(DoorState.CLOSING, door.state());
        // 普通なら50tickで閉まるところを75tickかける
        run(door, 241, 290);
        assertEquals(DoorState.CLOSING, door.state());
        assertTrue(door.fractionAt(290) > 0);
        run(door, 291, 315);
        assertEquals(DoorState.CLOSED, door.state());
    }

    @Test
    void anOrdinaryOpenButtonKeepsTheWheelchairDwellAndSlowClose() {
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.CAR, true), true);
        run(door, 1, 100);
        door.open(100, TIMING.dwellTicks(CallType.CAR));
        assertTrue(door.isSlowClose());
        assertEquals(200, door.dwellTicks());
        // 閉まりかけを普通のボタンで開き直しても、ゆっくりのまま
        door.close(150);
        door.open(160, TIMING.dwellTicks(CallType.CAR));
        assertTrue(door.isSlowClose());
        assertEquals(200, door.dwellTicks());
        // 閉まりきったあとに普通に開けば、普通に戻る
        DoorController plain = new DoorController(TIMING);
        plain.open(0, TIMING.dwellTicks(CallType.CAR, true), true);
        run(plain, 1, 40);
        plain.close(41);
        run(plain, 41, 200);
        assertTrue(plain.isClosed());
        plain.open(300, TIMING.dwellTicks(CallType.CAR));
        assertFalse(plain.isSlowClose());
        assertEquals(40, plain.dwellTicks());
    }

    @Test
    void holdingWhileOpenRestartsTheDwellAndSkipsTheAnnouncementWait() {
        DoorController door = new DoorController(TIMING);
        door.setCloseAnnouncementTicks(30);
        door.open(0, TIMING.dwellTicks(CallType.CAR));
        run(door, 1, 40);
        assertEquals(DoorState.OPEN, door.state());
        // 待機時間が過ぎても、検知しているあいだは閉めず、戸閉アナウンスも流さない
        for (long t = 41; t <= 200; t++) {
            door.hold(t);
            assertEquals(DoorController.Event.NONE, door.tick(t));
        }
        assertEquals(DoorState.OPEN, door.state());
        assertFalse(door.isWaitingToClose());
        // 検知がなくなってから待機時間がたつと、戸閉アナウンスを流す
        run(door, 201, 239);
        assertEquals(DoorController.Event.CLOSE_ANNOUNCED, door.tick(240));
        // アナウンスを待っているあいだに検知したら、待つのをやめて待機し直す
        door.hold(245);
        assertFalse(door.isWaitingToClose());
        run(door, 246, 284);
        assertEquals(DoorState.OPEN, door.state());
        assertEquals(DoorController.Event.CLOSE_ANNOUNCED, door.tick(285));
    }

    /** 開いて閉まり始めるまで進め、閉まり始めたゲーム時刻を返す。 */
    private static long runUntilClosing(DoorController door, long from) {
        long t = from;
        while (door.state() != DoorState.CLOSING) {
            door.tick(++t);
        }
        return t;
    }

    @Test
    void fiveReversalsInARowStartNudging() {
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.CAR));
        long t = 0;
        for (int i = 1; i <= DoorController.NUDGE_REVERSALS; i++) {
            t = runUntilClosing(door, t);
            assertFalse(door.isNudging(), "not yet after " + (i - 1) + " reversals");
            door.obstruct(++t);
            assertEquals(DoorState.OPENING, door.state());
        }
        assertTrue(door.isNudging());
        // ナッジング中は開く操作を受け付けない
        long closing = runUntilClosing(door, t);
        door.open(closing + 1, TIMING.dwellTicks(CallType.HALL));
        assertEquals(DoorState.CLOSING, door.state(), "the open button should be ignored");
        // 低速で閉める（普通なら50tickのところ100tick）
        run(door, closing + 1, closing + 60);
        assertEquals(DoorState.CLOSING, door.state());
        run(door, closing + 61, closing + DoorController.NUDGE_CLOSE_TICKS);
        assertEquals(DoorState.CLOSED, door.state());
        assertFalse(door.isNudging(), "nudging ends once closed");
    }

    @Test
    void reversalsAreCountedOnlyUntilTheDoorCloses() {
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.CAR));
        long t = 0;
        for (int i = 0; i < DoorController.NUDGE_REVERSALS - 1; i++) {
            t = runUntilClosing(door, t);
            door.obstruct(++t);
        }
        t = runUntilClosing(door, t);
        while (!door.isClosed()) {
            door.tick(++t);
        }
        door.open(++t, TIMING.dwellTicks(CallType.CAR));
        t = runUntilClosing(door, t);
        door.obstruct(++t);
        assertFalse(door.isNudging(), "the count starts over after the door closed");
    }

    @Test
    void aSafetyShoeDuringNudgingReopensAndClosesAgainRightAway() {
        DoorController door = new DoorController(TIMING);
        door.open(0, TIMING.dwellTicks(CallType.CAR));
        long t = 0;
        for (int i = 0; i < DoorController.NUDGE_REVERSALS; i++) {
            t = runUntilClosing(door, t);
            door.obstruct(++t);
        }
        t = runUntilClosing(door, t);
        door.obstruct(++t);
        assertEquals(DoorState.OPENING, door.state());
        assertTrue(door.isNudging());
        long opened = t;
        while (door.state() != DoorState.OPEN) {
            door.tick(++t);
        }
        // 開放待機もアナウンスも待たずに、次のtickで閉め始める
        assertEquals(DoorController.Event.STATE_CHANGED, door.tick(++t));
        assertEquals(DoorState.CLOSING, door.state());
        assertTrue(t - opened < TIMING.openTicks() + 3);
    }

    @Test
    void sixtySecondsOpenStartNudgingEvenWhileHeld() {
        DoorController door = new DoorController(TIMING);
        door.setCloseAnnouncementTicks(30);
        door.open(0, TIMING.dwellTicks(CallType.CAR));
        long t = 0;
        DoorController.Event event = DoorController.Event.NONE;
        while (door.state() != DoorState.CLOSING) {
            t++;
            door.hold(t);
            event = door.tick(t);
        }
        assertEquals(DoorController.NUDGE_OPEN_TICKS, t);
        assertEquals(DoorController.Event.NUDGING_STARTED, event);
        assertTrue(door.isNudging());
        door.hold(t + 1);
        assertEquals(DoorState.CLOSING, door.state(), "the beam no longer holds the door");
    }

    @Test
    void nudgingIsRestoredOnTheClient() {
        DoorController server = new DoorController(TIMING);
        server.open(0, 40);
        long t = 0;
        while (server.state() != DoorState.CLOSING || !server.isNudging()) {
            t++;
            server.hold(t);
            server.tick(t);
        }
        DoorController client = new DoorController(TIMING);
        client.restore(server.state(), server.startTick(), server.startFraction(), server.dwellTicks(), server.isSlowClose(), server.isNudging());
        assertEquals(server.fractionAt(t + 70), client.fractionAt(t + 70), EPS);
        assertTrue(client.fractionAt(t + 70) > 0, "still closing slowly");
    }

    @Test
    void slowCloseIsRestoredOnTheClient() {
        DoorController server = new DoorController(TIMING);
        server.open(0, 200, true);
        run(server, 1, 40);
        server.close(50);
        DoorController client = new DoorController(TIMING);
        client.restore(server.state(), server.startTick(), server.startFraction(), server.dwellTicks(), server.isSlowClose());
        assertEquals(server.fractionAt(100), client.fractionAt(100), EPS);
    }
}

