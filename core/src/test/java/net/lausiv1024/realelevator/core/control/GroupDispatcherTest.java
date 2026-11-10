package net.lausiv1024.realelevator.core.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.control.GroupDispatcher.CarState;

class GroupDispatcherTest {
    private static final double SPEED = 1.5;

    private static CarState idle(double y) {
        return new CarState(y, TravelDirection.NONE, SPEED, 0, List.of(), true);
    }

    @Test
    void theNearestIdleCarAnswers() {
        List<CarState> cars = List.of(idle(0), idle(40), idle(20));
        assertEquals(2, GroupDispatcher.choose(cars, 24, TravelDirection.UP));
        assertEquals(0, GroupDispatcher.choose(cars, 4, TravelDirection.DOWN));
    }

    /** 見積もりが同じかご（同じ階で待っているかご）なら、乗場のボタンを押した場所にいちばん近いかごにする。 */
    @Test
    void amongEqualCarsTheOneNearestToThePressedButtonAnswers() {
        List<CarState> cars = List.of(idle(0), idle(0), idle(0));
        assertEquals(2, GroupDispatcher.choose(cars, 12, TravelDirection.DOWN, List.of(6.0, 3.0, 0.0)));
        assertEquals(1, GroupDispatcher.choose(cars, 12, TravelDirection.DOWN, List.of(6.0, 0.0, 3.0)));
        // 距離を渡さなければ、並びの先のかご
        assertEquals(0, GroupDispatcher.choose(cars, 12, TravelDirection.DOWN));
        // 早く着くかごがあれば、遠くてもそちら
        List<CarState> uneven = List.of(idle(0), idle(8));
        assertEquals(1, GroupDispatcher.choose(uneven, 12, TravelDirection.DOWN, List.of(0.0, 9.0)));
    }

    @Test
    void aCarGoingTheSameWayPicksUpOnTheWay() {
        // 上りで 10 にいるかごは、20 の上りの呼びに途中で止まれる。0 で止まっているかごより早い
        CarState passing = new CarState(10, TravelDirection.UP, SPEED, 0, List.of(40.0), true);
        CarState waiting = idle(0);
        assertEquals(0, GroupDispatcher.choose(List.of(passing, waiting), 20, TravelDirection.UP));
        // 下りの呼びには、40 まで行って折り返すので、止まっているかごのほうが早い
        assertEquals(1, GroupDispatcher.choose(List.of(passing, waiting), 20, TravelDirection.DOWN));
    }

    @Test
    void stopsAndOpenDoorsAddTime() {
        CarState clear = new CarState(0, TravelDirection.UP, SPEED, 0, List.of(40.0), true);
        CarState busy = new CarState(0, TravelDirection.UP, SPEED, 0, List.of(8.0, 12.0, 16.0, 40.0), true);
        double withStops = GroupDispatcher.estimate(busy, 20, TravelDirection.UP);
        double without = GroupDispatcher.estimate(clear, 20, TravelDirection.UP);
        assertEquals(3 * GroupDispatcher.STOP_SECONDS, withStops - without, 1e-9);
        CarState openDoors = new CarState(0, TravelDirection.NONE, SPEED, 6, List.of(), true);
        assertEquals(6, GroupDispatcher.estimate(openDoors, 20, TravelDirection.UP) - GroupDispatcher.estimate(idle(0), 20, TravelDirection.UP),
                1e-9);
    }

    @Test
    void aCarGoingDownWorksTheSameWayMirrored() {
        CarState down = new CarState(40, TravelDirection.DOWN, SPEED, 0, List.of(0.0), true);
        // 下りの途中の 20 の下りの呼び
        double onTheWay = GroupDispatcher.estimate(down, 20, TravelDirection.DOWN);
        assertEquals(GroupDispatcher.START_SECONDS + 20 / SPEED, onTheWay, 1e-9);
        // 上りの呼びは、0 まで行って折り返す
        double back = GroupDispatcher.estimate(down, 20, TravelDirection.UP);
        assertEquals(GroupDispatcher.START_SECONDS + (40 + 20) / SPEED + GroupDispatcher.STOP_SECONDS, back, 1e-9);
    }

    @Test
    void unavailableCarsAreSkipped() {
        CarState resting = new CarState(20, TravelDirection.NONE, SPEED, 0, List.of(), false);
        assertEquals(1, GroupDispatcher.choose(List.of(resting, idle(0)), 20, TravelDirection.UP));
        assertEquals(-1, GroupDispatcher.choose(List.of(resting), 20, TravelDirection.UP));
        assertTrue(GroupDispatcher.estimate(idle(0), 0, TravelDirection.UP) > 0);
    }
}
