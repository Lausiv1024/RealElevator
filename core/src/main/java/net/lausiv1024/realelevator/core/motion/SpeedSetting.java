package net.lausiv1024.realelevator.core.motion;

/**
 * 速度の設定。速度の仕様と、加速度・ジャーク。
 *
 * <p>加速度とジャークは、高速仕様（高速・超高速）だけ標準の値から上げられる。上限は実機の高速機の上限付近を1.5倍した値で、
 * 加速度 {@value #MAX_ACCELERATION}、ジャーク {@value #MAX_JERK}。値は {@value #STEP} 刻みにそろえる。
 * 普通仕様の加速度とジャークは標準の値に固定する。
 */
public record SpeedSetting(SpeedSpec spec, double acceleration, double jerk) {
    /** 高速仕様で上げられる加速度の上限 [ブロック/s²]。 */
    public static final double MAX_ACCELERATION = 2.0;
    /** 高速仕様で上げられるジャークの上限 [ブロック/s³]。 */
    public static final double MAX_JERK = 3.0;
    /** 加速度とジャークの刻み。 */
    public static final double STEP = 0.1;

    /** 範囲外の値や、普通仕様で標準と違う値は、使える値に直す。 */
    public SpeedSetting {
        TravelLimits standard = spec.limits();
        if (spec.isHighSpeed()) {
            acceleration = snap(acceleration, standard.maxAcceleration(), MAX_ACCELERATION);
            jerk = snap(jerk, standard.maxJerk(), MAX_JERK);
        } else {
            acceleration = standard.maxAcceleration();
            jerk = standard.maxJerk();
        }
    }

    /** 標準の加速度とジャークでの設定。 */
    public static SpeedSetting standard(SpeedSpec spec) {
        return new SpeedSetting(spec, spec.limits().maxAcceleration(), spec.limits().maxJerk());
    }

    private static double snap(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        double clamped = Math.min(max, Math.max(min, value));
        // 刻みにそろえる。浮動小数の誤差を残さないよう、刻みの数を整数にしてから戻す
        double snapped = Math.round(clamped / STEP) * STEP;
        return Math.round(Math.min(max, Math.max(min, snapped)) * 1000) / 1000.0;
    }

    /** 加速度とジャークを上げられる仕様か。 */
    public boolean isAdjustable() {
        return spec.isHighSpeed();
    }

    /** 加速度の下限（標準の値）。 */
    public double minAcceleration() {
        return spec.limits().maxAcceleration();
    }

    /** ジャークの下限（標準の値）。 */
    public double minJerk() {
        return spec.limits().maxJerk();
    }

    /** 運転曲線の上限値。 */
    public TravelLimits limits() {
        return new TravelLimits(spec.limits().maxVelocity(), acceleration, jerk);
    }

    /** 仕様を変える。加速度とジャークは、変えた先で使える値に直す。 */
    public SpeedSetting withSpec(SpeedSpec spec) {
        return new SpeedSetting(spec, acceleration, jerk);
    }

    public SpeedSetting withAcceleration(double acceleration) {
        return new SpeedSetting(spec, acceleration, jerk);
    }

    public SpeedSetting withJerk(double jerk) {
        return new SpeedSetting(spec, acceleration, jerk);
    }
}
