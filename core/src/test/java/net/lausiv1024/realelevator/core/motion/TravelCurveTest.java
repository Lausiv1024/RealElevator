package net.lausiv1024.realelevator.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class TravelCurveTest {
    private static final double EPS = 1e-9;
    private static final int SAMPLES = 20_000;

    /** 最高速度・最大加速度とも到達し、定速走行がある。 */
    private static final TravelLimits CRUISE = new TravelLimits(1.5, 1.5, 3.0);

    static Stream<Arguments> cases() {
        return Stream.of(
                // 定速走行あり
                Arguments.of("cruise", 0.0, 10.0, CRUISE),
                // 最大加速度には届くが最高速度に届かない
                Arguments.of("no cruise", 0.0, 2.0, CRUISE),
                // 最大加速度にも届かない
                Arguments.of("triangular acceleration", 0.0, 0.2, CRUISE),
                // 最高速度が低く、最大加速度に届く前に最高速度に達する
                Arguments.of("velocity limited", 0.0, 10.0, new TravelLimits(0.5, 1.5, 1.0)),
                // 下り、超高速
                Arguments.of("down, very fast", 64.0, -59.0, new TravelLimits(15.0, 1.5, 3.0)));
    }

    @Test
    void durationOfTripWithCruiseMatchesAnalyticalValue() {
        TravelCurve curve = TravelCurve.plan(0, 10, CRUISE);
        // ジャーク区間 0.5s ×2 ＋ 加速度一定 0.5s で 1.5s かけて 1.125 ブロック進む。加速と減速で 2.25 ブロック。
        double expected = 2 * 1.5 + (10 - 2.25) / 1.5;
        assertEquals(expected, curve.duration(), EPS);
        assertEquals(1.5, curve.peakVelocity(), EPS);
        assertEquals(1.5, curve.peakAcceleration(), EPS);
    }

    @Test
    void longTripInSpecTakesAbout19Seconds() {
        // 仕様書の例：Y64→−59（約123ブロック）を超高速で運転しても最高速度に届かない。
        // ジャークを無視すると約18秒、高速仕様のジャーク（2.0）を含めると約19秒
        TravelCurve curve = TravelCurve.plan(64, -59, SpeedSpec.ULTRA_HIGH.limits());
        assertTrue(curve.duration() > 18.5 && curve.duration() < 19.5, "duration = " + curve.duration());
        assertTrue(curve.peakVelocity() < SpeedSpec.ULTRA_HIGH.limits().maxVelocity());
    }

    @Test
    void speedSpecsUseSpecValues() {
        assertEquals(1.5, SpeedSpec.NORMAL_LOW.limits().maxVelocity());
        assertEquals(2.6, SpeedSpec.NORMAL_HIGH.limits().maxVelocity());
        assertEquals(7.5, SpeedSpec.HIGH.limits().maxVelocity());
        assertEquals(15.0, SpeedSpec.ULTRA_HIGH.limits().maxVelocity());
        for (SpeedSpec spec : SpeedSpec.values()) {
            assertEquals(1.5, spec.limits().maxAcceleration());
            assertEquals(spec.isHighSpeed() ? 2.0 : 1.5, spec.limits().maxJerk());
        }
        assertFalse(SpeedSpec.NORMAL_HIGH.isHighSpeed());
        assertTrue(SpeedSpec.HIGH.isHighSpeed());
    }

    @ParameterizedTest
    @EnumSource(SpeedSpec.class)
    void oneFloorTripWithEachSpecLandsExactly(SpeedSpec spec) {
        // 階高4.5ブロック（実機3m×1.5）の1階分
        TravelCurve curve = TravelCurve.plan(64, 68.5, spec.limits());
        assertEquals(68.5, curve.position(curve.duration()));
        assertTrue(curve.peakVelocity() <= spec.limits().maxVelocity());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void startsAndLandsExactly(String name, double from, double to, TravelLimits limits) {
        TravelCurve curve = TravelCurve.plan(from, to, limits);
        assertEquals(from, curve.position(-1));
        assertEquals(from, curve.position(0));
        assertEquals(to, curve.position(curve.duration()));
        assertEquals(to, curve.position(curve.duration() + 100));
        assertEquals(0, curve.velocity(0));
        assertEquals(0, curve.velocity(curve.duration()));
        assertFalse(curve.isArrived(curve.duration() / 2));
        assertTrue(curve.isArrived(curve.duration()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void respectsLimits(String name, double from, double to, TravelLimits limits) {
        TravelCurve curve = TravelCurve.plan(from, to, limits);
        double dt = curve.duration() / SAMPLES;
        for (int i = 0; i <= SAMPLES; i++) {
            double t = i * dt;
            assertTrue(Math.abs(curve.velocity(t)) <= limits.maxVelocity() + EPS, "velocity at " + t);
            assertTrue(Math.abs(curve.acceleration(t)) <= limits.maxAcceleration() + EPS, "acceleration at " + t);
            if (i < SAMPLES) {
                double jerk = (curve.acceleration(t + dt) - curve.acceleration(t)) / dt;
                assertTrue(Math.abs(jerk) <= limits.maxJerk() * (1 + 1e-6), "jerk at " + t + " = " + jerk);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void movesMonotonicallyWithoutOvershoot(String name, double from, double to, TravelLimits limits) {
        TravelCurve curve = TravelCurve.plan(from, to, limits);
        double sign = Math.signum(to - from);
        double dt = curve.duration() / SAMPLES;
        double previous = from;
        for (int i = 1; i <= SAMPLES; i++) {
            double position = curve.position(i * dt);
            assertTrue((position - previous) * sign >= -EPS, "moved backwards at " + i * dt);
            assertTrue((to - position) * sign >= -EPS, "overshot at " + i * dt);
            previous = position;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void positionVelocityAndAccelerationAreConsistent(String name, double from, double to, TravelLimits limits) {
        TravelCurve curve = TravelCurve.plan(from, to, limits);
        double h = 1e-5;
        int steps = 997; // 区間の境界ちょうどを避けるため素数で刻む
        for (int i = 1; i < steps; i++) {
            double t = curve.duration() * i / steps;
            double velocity = (curve.position(t + h) - curve.position(t - h)) / (2 * h);
            double acceleration = (curve.velocity(t + h) - curve.velocity(t - h)) / (2 * h);
            assertEquals(curve.velocity(t), velocity, 1e-6, "velocity at " + t);
            assertEquals(curve.acceleration(t), acceleration, limits.maxJerk() * h, "acceleration at " + t);
        }
    }

    @Test
    void reachesTheWholeDistanceWhenStoppingShort() {
        TravelCurve curve = TravelCurve.plan(0, 2, CRUISE);
        assertTrue(curve.peakVelocity() < CRUISE.maxVelocity());
        assertEquals(CRUISE.maxAcceleration(), curve.peakAcceleration(), EPS);
        assertEquals(curve.peakVelocity(), curve.velocity(curve.duration() / 2), EPS);
        assertEquals(1.0, curve.position(curve.duration() / 2), EPS);

        TravelCurve tiny = TravelCurve.plan(0, 0.2, CRUISE);
        assertTrue(tiny.peakAcceleration() < CRUISE.maxAcceleration());
        assertEquals(0.1, tiny.position(tiny.duration() / 2), EPS);
    }

    @Test
    void downwardTripMirrorsUpwardTrip() {
        TravelCurve up = TravelCurve.plan(10, 30, CRUISE);
        TravelCurve down = TravelCurve.plan(30, 10, CRUISE);
        assertEquals(up.duration(), down.duration(), EPS);
        for (int i = 0; i <= 100; i++) {
            double t = up.duration() * i / 100;
            assertEquals(up.position(t) - 10, 30 - down.position(t), EPS);
            assertEquals(up.velocity(t), -down.velocity(t), EPS);
            assertEquals(up.acceleration(t), -down.acceleration(t), EPS);
        }
    }

    @Test
    void zeroDistanceStaysInPlace() {
        TravelCurve curve = TravelCurve.plan(5, 5, CRUISE);
        assertEquals(0, curve.duration());
        assertEquals(5, curve.position(0));
        assertEquals(5, curve.position(1));
        assertEquals(0, curve.velocity(0.5));
        assertTrue(curve.isArrived(0));
    }

    @Test
    void positionAtTickUsesTwentyTicksPerSecond() {
        TravelCurve curve = TravelCurve.plan(0, 10, CRUISE);
        assertEquals(curve.position(1.0), curve.positionAtTick(20));
        assertEquals(curve.position(2.525), curve.positionAtTick(50.5));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new TravelLimits(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TravelLimits(1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TravelLimits(1, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TravelLimits(1, 1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> TravelCurve.plan(0, Double.NaN, CRUISE));
    }
}
