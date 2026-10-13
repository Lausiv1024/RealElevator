package net.lausiv1024.realelevator.core.motion;

/**
 * ジャーク制限付きのS字加減速による運転曲線。
 *
 * <p>停止状態から出発し、停止状態で到着する。加速側は「ジャーク一定で加速度を上げる → 加速度一定 →
 * ジャーク一定で加速度を下げる」の3区間、その後に定速走行、減速側は加速側の鏡像になる（計7区間）。
 * 行程が短く最高速度や最大加速度に届かない場合は、該当する区間の長さが0になる。
 *
 * <p>位置は時刻だけから決まるので、サーバーとクライアントが同じ発車情報から同じ位置を計算できる。
 * 到着時刻以降は行先の位置をそのまま返す（着床）。
 *
 * <p>単位はブロック（＝m）と秒。速度と加速度は、ワールドのY軸の向き（上が正）で返す。
 */
public final class TravelCurve {
    public static final double TICKS_PER_SECOND = 20.0;

    private final double from;
    private final double to;
    private final double direction;
    private final double distance;

    private final double jerk;
    private final double peakAcceleration;
    private final double peakVelocity;

    /** 加速側の区間の境界時刻（ジャーク正の終わり、加速度一定の終わり、加速の終わり）。 */
    private final double t1;
    private final double t2;
    private final double t3;
    private final double duration;

    /** 区間の境界での、出発位置からの距離と速さ。 */
    private final double s1;
    private final double v1;
    private final double s2;
    private final double v2;
    private final double s3;

    private TravelCurve(double from, double to, double jerk, double peakAcceleration, double peakVelocity,
                        double jerkTime, double constantAccelerationTime, double cruiseTime) {
        this.from = from;
        this.to = to;
        this.direction = Math.signum(to - from);
        this.distance = Math.abs(to - from);
        this.jerk = jerk;
        this.peakAcceleration = peakAcceleration;
        this.peakVelocity = peakVelocity;

        this.t1 = jerkTime;
        this.t2 = jerkTime + constantAccelerationTime;
        this.t3 = 2 * jerkTime + constantAccelerationTime;
        this.duration = 2 * t3 + cruiseTime;

        this.v1 = jerk * jerkTime * jerkTime / 2;
        this.s1 = jerk * jerkTime * jerkTime * jerkTime / 6;
        this.v2 = v1 + peakAcceleration * constantAccelerationTime;
        this.s2 = s1 + v1 * constantAccelerationTime
                + peakAcceleration * constantAccelerationTime * constantAccelerationTime / 2;
        this.s3 = s2 + v2 * jerkTime + peakAcceleration * jerkTime * jerkTime / 2
                - jerk * jerkTime * jerkTime * jerkTime / 6;
    }

    /**
     * 出発位置から行先位置までの運転曲線を求める。
     *
     * @param from   出発位置（かごの床面のY座標など）
     * @param to     行先位置
     * @param limits 速度・加速度・ジャークの上限
     */
    public static TravelCurve plan(double from, double to, TravelLimits limits) {
        if (!Double.isFinite(from) || !Double.isFinite(to)) {
            throw new IllegalArgumentException("positions must be finite: " + from + " -> " + to);
        }
        double distance = Math.abs(to - from);
        double jerk = limits.maxJerk();
        if (distance == 0) {
            return new TravelCurve(from, to, jerk, 0, 0, 0, 0, 0);
        }

        double velocity = limits.maxVelocity();
        double acceleration = limits.maxAcceleration();

        // 最大加速度に届く前に最高速度に達してしまう場合は、加速度の頂点を下げる
        if (velocity * jerk < acceleration * acceleration) {
            acceleration = Math.sqrt(velocity * jerk);
        }

        if (2 * accelerationDistance(velocity, acceleration, jerk) > distance) {
            // 最高速度に届かない。加速と減速だけで行程を埋める速度を求める。
            // 最大加速度に届く場合: d = v (v/a + a/j) を v について解く
            double velocityToReachAcceleration = acceleration * acceleration / jerk;
            velocity = (-velocityToReachAcceleration
                    + Math.sqrt(velocityToReachAcceleration * velocityToReachAcceleration
                    + 4 * acceleration * distance)) / 2;
            if (velocity < velocityToReachAcceleration) {
                // 最大加速度にも届かない。加速度は三角形になり、d = 2 j Tj³
                double jerkTime = Math.cbrt(distance / (2 * jerk));
                acceleration = jerk * jerkTime;
                velocity = jerk * jerkTime * jerkTime;
            }
        }

        double jerkTime = acceleration / jerk;
        double constantAccelerationTime = Math.max(0, velocity / acceleration - jerkTime);
        double cruiseTime = Math.max(0, (distance - 2 * accelerationDistance(velocity, acceleration, jerk)) / velocity);
        return new TravelCurve(from, to, jerk, acceleration, velocity, jerkTime, constantAccelerationTime, cruiseTime);
    }

    /** 停止状態から速度 v まで、加速度の頂点 a・ジャーク j で加速するのに要する距離。 */
    private static double accelerationDistance(double v, double a, double j) {
        return v / 2 * (v / a + a / j);
    }

    /** 発車からの経過時間 t [s] でのかごの位置。 */
    public double position(double t) {
        return from + direction * distanceAt(t);
    }

    /** 発車からの経過時間 t [s] でのかごの速度（上が正）。 */
    public double velocity(double t) {
        if (t <= 0 || t >= duration) {
            return 0;
        }
        double speed = t <= duration / 2 ? forwardVelocity(t) : forwardVelocity(duration - t);
        return direction * speed;
    }

    /** 発車からの経過時間 t [s] でのかごの加速度（上が正）。 */
    public double acceleration(double t) {
        if (t <= 0 || t >= duration) {
            return 0;
        }
        double accel = t <= duration / 2 ? forwardAcceleration(t) : -forwardAcceleration(duration - t);
        return direction * accel;
    }

    /** 発車からの経過tick数（partialTickを含めてよい）でのかごの位置。 */
    public double positionAtTick(double ticks) {
        return position(ticks / TICKS_PER_SECOND);
    }

    /** 経過時間 t [s] の時点で到着しているかどうか。 */
    public boolean isArrived(double t) {
        return t >= duration;
    }

    private double distanceAt(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t >= duration) {
            return distance;
        }
        // 減速側は加速側の鏡像として計算し、到着位置に誤差が出ないようにする
        return t <= duration / 2 ? forwardDistance(t) : distance - forwardDistance(duration - t);
    }

    /** 加速側（0 ≤ t ≤ 所要時間の半分）での、出発位置からの距離。 */
    private double forwardDistance(double t) {
        if (t < t1) {
            return jerk * t * t * t / 6;
        }
        if (t < t2) {
            double u = t - t1;
            return s1 + v1 * u + peakAcceleration * u * u / 2;
        }
        if (t < t3) {
            double u = t - t2;
            return s2 + v2 * u + peakAcceleration * u * u / 2 - jerk * u * u * u / 6;
        }
        return s3 + peakVelocity * (t - t3);
    }

    private double forwardVelocity(double t) {
        if (t < t1) {
            return jerk * t * t / 2;
        }
        if (t < t2) {
            return v1 + peakAcceleration * (t - t1);
        }
        if (t < t3) {
            double u = t - t2;
            return v2 + peakAcceleration * u - jerk * u * u / 2;
        }
        return peakVelocity;
    }

    private double forwardAcceleration(double t) {
        if (t < t1) {
            return jerk * t;
        }
        if (t < t2) {
            return peakAcceleration;
        }
        if (t < t3) {
            return peakAcceleration - jerk * (t - t2);
        }
        return 0;
    }

    public double from() {
        return from;
    }

    public double to() {
        return to;
    }

    /** 走行距離（向きによらず正）。 */
    public double distance() {
        return distance;
    }

    /** 発車から到着までの所要時間 [s]。 */
    public double duration() {
        return duration;
    }

    /** この行程で実際に到達する最高速度（向きによらず正）。 */
    public double peakVelocity() {
        return peakVelocity;
    }

    /** この行程で実際に到達する最大加速度（向きによらず正）。 */
    public double peakAcceleration() {
        return peakAcceleration;
    }
}
