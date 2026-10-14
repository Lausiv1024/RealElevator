package net.lausiv1024.realelevator.core.control;

import static net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult.ALREADY_REGISTERED;
import static net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult.INVALID;
import static net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult.REGISTERED;
import static net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult.SERVED_HERE;
import static net.lausiv1024.realelevator.core.control.TravelDirection.DOWN;
import static net.lausiv1024.realelevator.core.control.TravelDirection.NONE;
import static net.lausiv1024.realelevator.core.control.TravelDirection.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CollectiveControlTest {
    /** 呼びがなくなるまで運転し、止まった階を順に返す。 */
    private static List<Integer> runUntilIdle(CollectiveControl control, int startFloor) {
        List<Integer> stops = new ArrayList<>();
        int floor = startFloor;
        for (int i = 0; i < 50; i++) {
            int next = control.nextStop(floor);
            if (next < 0) {
                return stops;
            }
            control.arrive(next);
            stops.add(next);
            floor = next;
        }
        throw new AssertionError("did not become idle: " + stops);
    }

    @Test
    void needsAtLeastTwoFloors() {
        assertThrows(IllegalArgumentException.class, () -> new CollectiveControl(1));
    }

    @Test
    void idleWithoutCalls() {
        CollectiveControl control = new CollectiveControl(5);
        assertEquals(-1, control.nextStop(2));
        assertEquals(NONE, control.direction());
    }

    @Test
    void registersCallsAndRejectsInvalidOnes() {
        CollectiveControl control = new CollectiveControl(3);
        assertEquals(REGISTERED, control.pressCar(2, 0, true));
        assertEquals(ALREADY_REGISTERED, control.pressCar(2, 0, true));
        assertTrue(control.hasCarCall(2));
        assertEquals(INVALID, control.pressCar(3, 0, true));
        assertEquals(INVALID, control.pressHall(2, UP, 0, true));
        assertEquals(INVALID, control.pressHall(0, DOWN, 0, true));
        assertEquals(INVALID, control.pressHall(1, NONE, 0, true));
        assertEquals(REGISTERED, control.pressHall(1, DOWN, 0, true));
        assertTrue(control.hasHallCall(1, DOWN));
        assertFalse(control.hasHallCall(1, UP));
    }

    @Test
    void callAtTheStoppedFloorIsServedImmediately() {
        CollectiveControl control = new CollectiveControl(3);
        assertEquals(SERVED_HERE, control.pressCar(1, 1, true));
        assertEquals(SERVED_HERE, control.pressHall(1, UP, 1, true));
        assertEquals(UP, control.direction());
        // 走行中なら、今いる（最後に止まった）階でも登録する
        assertEquals(REGISTERED, control.pressCar(1, 1, false));
    }

    @Test
    void oppositeHallCallAtTheStoppedFloorWaitsWhileCallsRemainAhead() {
        CollectiveControl control = new CollectiveControl(4);
        control.pressCar(3, 1, true);
        assertEquals(3, control.nextStop(1));
        assertEquals(UP, control.direction());
        // 上昇しようとしている途中で、同じ階の下りの乗場呼び
        assertEquals(REGISTERED, control.pressHall(1, DOWN, 1, true));
        assertEquals(List.of(3, 1), runUntilIdle(control, 1));
    }

    @Test
    void servesCallsInTravelDirectionOrder() {
        CollectiveControl control = new CollectiveControl(6);
        control.pressCar(4, 2, true);
        control.pressCar(0, 2, true);
        control.pressHall(3, UP, 2, true);
        control.pressHall(5, DOWN, 2, true);
        control.pressHall(1, DOWN, 2, true);
        // 先に近い上の呼びがあるので上昇から。上り呼びとかご呼びに順に止まり、最上の下り呼びで反転する
        assertEquals(UP, nextDirectionFrom(control, 2));
        assertEquals(List.of(3, 4, 5, 1, 0), runUntilIdle(control, 2));
        assertEquals(NONE, control.direction());
    }

    @Test
    void passesHallCallsOfTheOppositeDirectionAndServesThemOnTheWayBack() {
        CollectiveControl control = new CollectiveControl(5);
        control.pressHall(2, DOWN, 0, true);
        control.pressCar(4, 0, true);
        assertEquals(List.of(4, 2), runUntilIdle(control, 0));
    }

    @Test
    void reversesAtTheFarthestOppositeCall() {
        CollectiveControl control = new CollectiveControl(5);
        control.pressHall(2, DOWN, 0, true);
        control.pressHall(3, DOWN, 0, true);
        // 上には下り呼びしかないので、最も上の3階まで行ってから下りながら2階に止まる
        assertEquals(List.of(3, 2), runUntilIdle(control, 0));
    }

    @Test
    void arrivalReportsWhetherAHallCallWasServed() {
        CollectiveControl control = new CollectiveControl(3);
        control.pressCar(2, 0, true);
        assertEquals(2, control.nextStop(0));
        assertEquals(CallType.CAR, control.arrive(2));

        control.pressHall(0, UP, 2, true);
        assertEquals(0, control.nextStop(2));
        assertEquals(CallType.HALL, control.arrive(0));
        assertFalse(control.hasHallCall(0, UP));
        // 乗る人の方向を保ち、戸が閉じて呼びがなければ方向なしに戻る
        assertEquals(UP, control.direction());
        assertEquals(-1, control.nextStop(0));
        assertEquals(NONE, control.direction());
    }

    @Test
    void directionIsDecidedAsSoonAsACallIsRegisteredWhileStopped() {
        CollectiveControl control = new CollectiveControl(5);
        // 2階に止まって戸が開いている（方向なし）ときに、4階の乗場呼びが登録された
        assertEquals(REGISTERED, control.pressHall(4, DOWN, 2, true));
        assertEquals(UP, control.direction());

        CollectiveControl other = new CollectiveControl(5);
        assertEquals(REGISTERED, other.pressCar(0, 2, true));
        assertEquals(DOWN, other.direction());

        // 走行中の登録では、方向はそのまま
        CollectiveControl moving = new CollectiveControl(5);
        assertEquals(REGISTERED, moving.pressCar(0, 2, false));
        assertEquals(NONE, moving.direction());
    }

    @Test
    void oppositeHallCallAfterServingAHallCallIsRegistered() {
        CollectiveControl control = new CollectiveControl(5);
        control.pressHall(2, DOWN, 0, true);
        assertEquals(2, control.nextStop(0));
        control.arrive(2);
        assertEquals(DOWN, control.direction());
        // 下りの人を乗せている間に、同じ階で上りが押された。下りの呼びがこの先にないので、すぐに応える
        assertEquals(SERVED_HERE, control.pressHall(2, UP, 2, true));
        // 下りの先に呼びがあれば、上りは登録して後で応える
        control.pressHall(2, DOWN, 4, false);
        control.nextStop(4);
        control.arrive(2);
        control.pressCar(0, 2, true);
        assertEquals(REGISTERED, control.pressHall(2, UP, 2, true));
    }

    @Test
    void twoFloorRoundTrip() {
        CollectiveControl control = new CollectiveControl(2);
        assertEquals(REGISTERED, control.pressHall(1, DOWN, 0, true));
        assertEquals(1, control.nextStop(0));
        assertEquals(CallType.HALL, control.arrive(1));
        assertEquals(DOWN, control.direction());
        assertEquals(REGISTERED, control.pressCar(0, 1, true));
        assertEquals(0, control.nextStop(1));
        assertEquals(CallType.CAR, control.arrive(0));
        assertEquals(-1, control.nextStop(0));
    }

    @Test
    void snapshotRoundTrip() {
        CollectiveControl control = new CollectiveControl(70);
        control.pressCar(65, 0, true);
        control.pressHall(3, UP, 0, true);
        control.pressHall(40, DOWN, 0, true);
        control.nextStop(0);
        long[][] snapshot = control.snapshot();

        CollectiveControl copy = new CollectiveControl(70);
        copy.restore(snapshot[0], snapshot[1], snapshot[2], control.direction());
        assertTrue(copy.hasCarCall(65));
        assertTrue(copy.hasHallCall(3, UP));
        assertTrue(copy.hasHallCall(40, DOWN));
        assertEquals(runUntilIdle(control, 0), runUntilIdle(copy, 0));
    }

    private static TravelDirection nextDirectionFrom(CollectiveControl control, int floor) {
        long[][] s = control.snapshot();
        CollectiveControl probe = new CollectiveControl(control.floorCount());
        probe.restore(s[0], s[1], s[2], control.direction());
        probe.nextStop(floor);
        return probe.direction();
    }
}
