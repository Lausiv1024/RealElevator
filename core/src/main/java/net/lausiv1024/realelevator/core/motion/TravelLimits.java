package net.lausiv1024.realelevator.core.motion;

/**
 * 運転曲線の上限値。単位はブロック（＝m）と秒。
 *
 * @param maxVelocity     最高速度 [ブロック/s]
 * @param maxAcceleration 最大加速度 [ブロック/s²]
 * @param maxJerk         最大ジャーク（加加速度） [ブロック/s³]
 */
public record TravelLimits(double maxVelocity, double maxAcceleration, double maxJerk) {
    public TravelLimits {
        requirePositive("maxVelocity", maxVelocity);
        requirePositive("maxAcceleration", maxAcceleration);
        requirePositive("maxJerk", maxJerk);
    }

    private static void requirePositive(String name, double value) {
        if (!(value > 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be a positive finite number: " + value);
        }
    }
}
