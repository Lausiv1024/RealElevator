package net.lausiv1024.realelevator.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TripTest {
    @Test
    void positionIsMeasuredFromStartTick() {
        Trip trip = new Trip(1000, 64, 68.5, SpeedSpec.NORMAL_LOW);
        TravelCurve curve = trip.curve();
        assertEquals(64, trip.positionAt(500));
        assertEquals(64, trip.positionAt(1000));
        assertEquals(curve.positionAtTick(30.5), trip.positionAt(1030.5));
        assertEquals(68.5, trip.positionAt(1000 + curve.duration() * 20));
    }

    @Test
    void retargetEarlyStopsAtTheNearerFloorWithoutAJump() {
        Trip trip = new Trip(0, 0, 9, SpeedSpec.NORMAL_HIGH);
        Trip shorter = trip.retarget(10, 4.5);
        assertNotNull(shorter);
        assertEquals(4.5, shorter.to());
        assertEquals(trip.startTick(), shorter.startTick());
        // 切り替えた時点までは同じ位置
        for (double tick = 0; tick <= 10; tick += 0.5) {
            assertEquals(trip.positionAt(tick), shorter.positionAt(tick), 1e-12);
        }
        assertEquals(4.5, shorter.positionAt(1000));
    }

    @Test
    void retargetWhileCruisingDependsOnTheBrakingDistance() {
        SpeedSpec spec = SpeedSpec.NORMAL_LOW;
        Trip trip = new Trip(0, 0, 40, spec);
        TravelCurve full = trip.curve();
        // 最高速度で定速走行している間は、残りの距離が減速に要する距離以上なら止まれる
        double brakingDistance = TravelCurve.plan(0, 40, spec.limits()).position(full.duration())
                - full.position(full.duration() - accelerationTime(full));
        for (long tick = 100; tick < 300; tick++) {
            double position = trip.positionAt(tick);
            double floor = 20;
            boolean canStop = floor - position >= brakingDistance;
            Trip retargeted = trip.retarget(tick, floor);
            if (Math.abs(floor - position - brakingDistance) > spec.limits().maxVelocity() / 20) {
                assertEquals(canStop, retargeted != null, "tick " + tick + ", remaining " + (floor - position));
            }
        }
    }

    @Test
    void retargetIsRejectedForPassedOrFartherOrOppositeFloors() {
        Trip trip = new Trip(0, 10, 30, SpeedSpec.NORMAL_HIGH);
        long tick = 100;
        double position = trip.positionAt(tick);
        assertNull(trip.retarget(tick, position - 1));
        assertNull(trip.retarget(tick, 30));
        assertNull(trip.retarget(tick, 35));
        assertNull(trip.retarget(tick, 5));
    }

    @Test
    void retargetIsRejectedOnceDecelerationIsTooLate() {
        Trip trip = new Trip(0, 0, 9, SpeedSpec.NORMAL_HIGH);
        // 4.5 ブロック手前まで来てからでは止まれない
        long tick = 0;
        while (trip.positionAt(tick) < 4.0) {
            tick++;
        }
        assertNull(trip.retarget(tick, 4.5));
    }

    @Test
    void extendBeforeDeceleratingContinuesToTheFartherFloorWithoutAJump() {
        Trip trip = new Trip(0, 0, 40, SpeedSpec.NORMAL_LOW);
        Trip longer = trip.extend(100, 60);
        assertNotNull(longer);
        assertEquals(60, longer.to());
        for (double tick = 0; tick <= 100; tick += 0.5) {
            assertEquals(trip.positionAt(tick), longer.positionAt(tick), 1e-12);
        }
        assertEquals(60, longer.positionAt(100000));
    }

    @Test
    void extendIsRejectedOnceDecelerating() {
        Trip trip = new Trip(0, 0, 40, SpeedSpec.NORMAL_LOW);
        long tick = 0;
        while (trip.positionAt(tick) < 39) {
            tick++;
        }
        assertNull(trip.extend(tick, 60));
        // 手前や逆向きには延ばさない
        assertNull(new Trip(0, 0, 40, SpeedSpec.NORMAL_LOW).extend(100, 30));
        assertNull(new Trip(0, 0, 40, SpeedSpec.NORMAL_LOW).extend(100, -10));
    }

    /** 加速（＝減速）にかかる時間。定速走行の始まりの時刻。 */
    private static double accelerationTime(TravelCurve curve) {
        double t = 0;
        while (curve.acceleration(t) != 0 || t == 0) {
            t += 1e-4;
        }
        return t;
    }

    @Test
    void arrivalIsJudgedByGameTime() {
        Trip trip = new Trip(200, 68.5, 64, SpeedSpec.NORMAL_HIGH);
        double arrival = 200 + trip.curve().duration() * 20;
        assertFalse(trip.isArrivedAt(200));
        assertFalse(trip.isArrivedAt(arrival - 0.01));
        assertTrue(trip.isArrivedAt(arrival));
    }
}
