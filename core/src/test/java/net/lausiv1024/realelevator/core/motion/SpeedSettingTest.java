package net.lausiv1024.realelevator.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeedSettingTest {
    private static final double EPS = 1e-12;

    @Test
    void normalSpeedsKeepTheStandardAccelerationAndJerk() {
        SpeedSetting setting = new SpeedSetting(SpeedSpec.NORMAL_HIGH, 2.0, 3.0);
        assertFalse(setting.isAdjustable());
        assertEquals(SpeedSpec.DEFAULT_ACCELERATION, setting.acceleration(), EPS);
        assertEquals(SpeedSpec.DEFAULT_JERK, setting.jerk(), EPS);
        assertEquals(SpeedSpec.NORMAL_HIGH.limits(), setting.limits());
    }

    @Test
    void highSpeedsCanBeRaisedUpToTheLimits() {
        SpeedSetting setting = SpeedSetting.standard(SpeedSpec.ULTRA_HIGH).withAcceleration(1.8).withJerk(2.6);
        assertTrue(setting.isAdjustable());
        assertEquals(new TravelLimits(15.0, 1.8, 2.6), setting.limits());
        // 上限を超える値と、標準より下げる値は直す
        assertEquals(SpeedSetting.MAX_ACCELERATION, setting.withAcceleration(9).acceleration(), EPS);
        assertEquals(SpeedSetting.MAX_JERK, setting.withJerk(9).jerk(), EPS);
        assertEquals(SpeedSpec.DEFAULT_ACCELERATION, setting.withAcceleration(0.5).acceleration(), EPS);
        assertEquals(SpeedSpec.HIGH_SPEED_JERK, setting.withJerk(Double.NaN).jerk(), EPS);
    }

    @Test
    void valuesSnapToTheStep() {
        SpeedSetting setting = SpeedSetting.standard(SpeedSpec.HIGH).withAcceleration(1.6400001).withJerk(2.25);
        assertEquals(1.6, setting.acceleration(), EPS);
        assertEquals(2.3, setting.jerk(), EPS);
    }

    @Test
    void switchingToANormalSpeedDropsTheRaisedValues() {
        SpeedSetting raised = SpeedSetting.standard(SpeedSpec.HIGH).withAcceleration(2.0);
        assertEquals(SpeedSpec.DEFAULT_ACCELERATION, raised.withSpec(SpeedSpec.NORMAL_LOW).acceleration(), EPS);
    }

    @Test
    void aFasterSettingArrivesSooner() {
        Trip standard = new Trip(0, 0, 40, SpeedSetting.standard(SpeedSpec.HIGH).limits());
        Trip raised = new Trip(0, 0, 40, SpeedSetting.standard(SpeedSpec.HIGH).withAcceleration(2.0).withJerk(3.0).limits());
        assertTrue(raised.arrivalTick() < standard.arrivalTick());
        assertEquals(40, raised.positionAt(raised.arrivalTick()), 1e-9);
    }
}
