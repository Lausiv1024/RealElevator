package net.lausiv1024.realelevator.core.motion;

/**
 * 1回の走行の発車情報。サーバーからクライアントへはこれだけを送り、位置は両側で同じ曲線から求める。
 *
 * <p>時刻はワールドのゲーム時刻（tick）で表す。
 */
public final class Trip {
    /** 2つの曲線が同じ状態にあるとみなす誤差。同じ式で計算した区間は一致するので、丸め誤差だけを許す。 */
    private static final double STATE_TOLERANCE = 1e-9;

    private final long startTick;
    private final double from;
    private final double to;
    private final TravelLimits limits;
    private final TravelCurve curve;

    public Trip(long startTick, double from, double to, TravelLimits limits) {
        this.startTick = startTick;
        this.from = from;
        this.to = to;
        this.limits = limits;
        this.curve = TravelCurve.plan(from, to, limits);
    }

    /** 速度の仕様の標準の加速度とジャークで走る。 */
    public Trip(long startTick, double from, double to, SpeedSpec speedSpec) {
        this(startTick, from, to, speedSpec.limits());
    }

    /** ゲーム時刻 tick（partialTickを含めてよい）でのかごの位置。 */
    public double positionAt(double tick) {
        return curve.positionAtTick(tick - startTick);
    }

    /** 到着するゲーム時刻（partialTickを含む）。 */
    public double arrivalTick() {
        return startTick + curve.duration() * TravelCurve.TICKS_PER_SECOND;
    }

    /** ゲーム時刻 tick の時点で到着しているかどうか。 */
    public boolean isArrivedAt(double tick) {
        return curve.isArrived((tick - startTick) / TravelCurve.TICKS_PER_SECOND);
    }

    /**
     * 走行中に行先を手前の階 newTo へ変える。ゲーム時刻 tick の時点から減速して newTo に止まれるときだけ、新しい発車情報を返す。
     *
     * <p>運転曲線は、加速をできるだけ長く続けてから減速する形なので、出発時点から newTo まで改めて計画した曲線が、
     * 今の位置・速度・加速度と一致していれば、そのまま減速して止まれる。一致しなければ、減速が間に合わない（すでに
     * newTo 向けの減速を始めるべき時点を過ぎている）ので通過する。新しい発車情報は出発時刻と出発位置が同じなので、
     * 位置は途切れずにつながる。
     *
     * @return 止まれるなら新しい発車情報、止まれなければ null
     */
    public Trip retarget(double tick, double newTo) {
        double direction = Math.signum(to - from);
        double current = positionAt(tick);
        // 進行方向でまだ通過していない、元の行先より手前の位置だけ
        if (direction == 0 || (newTo - current) * direction <= 0 || (to - newTo) * direction <= 0) {
            return null;
        }
        Trip candidate = new Trip(startTick, from, newTo, limits);
        double t = (tick - startTick) / TravelCurve.TICKS_PER_SECOND;
        boolean sameState = Math.abs(candidate.curve.position(t) - curve.position(t)) < STATE_TOLERANCE
                && Math.abs(candidate.curve.velocity(t) - curve.velocity(t)) < STATE_TOLERANCE
                && Math.abs(candidate.curve.acceleration(t) - curve.acceleration(t)) < STATE_TOLERANCE;
        return sameState ? candidate : null;
    }

    public long startTick() {
        return startTick;
    }

    public double from() {
        return from;
    }

    public double to() {
        return to;
    }

    /** この走行の運転曲線の上限値。 */
    public TravelLimits limits() {
        return limits;
    }

    public TravelCurve curve() {
        return curve;
    }
}
