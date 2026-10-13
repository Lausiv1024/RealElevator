package net.lausiv1024.realelevator.core.motion;

/**
 * 速度の仕様。速度・加速度・ジャークとも実機の値の1.5倍。
 *
 * <p>加速度は実機 1.0 m/s²、ジャークは実機 1.0 m/s³ を基準にする。
 * 高速仕様（高速・超高速）は、短い行程でも速度を出せるようジャークを上げる。
 */
public enum SpeedSpec {
    /** 普通（低）：実機 60 m/min。 */
    NORMAL_LOW(1.5, SpeedSpec.DEFAULT_JERK),
    /** 普通（高）：実機 105 m/min。 */
    NORMAL_HIGH(2.6, SpeedSpec.DEFAULT_JERK),
    /** 高速：実機 300 m/min。 */
    HIGH(7.5, SpeedSpec.HIGH_SPEED_JERK),
    /** 超高速：実機 600 m/min。 */
    ULTRA_HIGH(15.0, SpeedSpec.HIGH_SPEED_JERK);

    public static final double DEFAULT_ACCELERATION = 1.5;
    public static final double DEFAULT_JERK = 1.5;
    public static final double HIGH_SPEED_JERK = 2.0;

    private final TravelLimits limits;

    SpeedSpec(double maxVelocity, double maxJerk) {
        this.limits = new TravelLimits(maxVelocity, DEFAULT_ACCELERATION, maxJerk);
    }

    /** 標準の加速度とジャークでの上限値。 */
    public TravelLimits limits() {
        return limits;
    }

    /** ネザースターで解放される高速仕様かどうか。 */
    public boolean isHighSpeed() {
        return this == HIGH || this == ULTRA_HIGH;
    }
}
