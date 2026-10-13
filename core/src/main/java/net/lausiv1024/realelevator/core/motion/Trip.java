package net.lausiv1024.realelevator.core.motion;

/**
 * 1回の走行の発車情報。サーバーからクライアントへはこれだけを送り、位置は両側で同じ曲線から求める。
 *
 * <p>時刻はワールドのゲーム時刻（tick）で表す。
 */
public final class Trip {
    private final long startTick;
    private final double from;
    private final double to;
    private final SpeedSpec speedSpec;
    private final TravelCurve curve;

    public Trip(long startTick, double from, double to, SpeedSpec speedSpec) {
        this.startTick = startTick;
        this.from = from;
        this.to = to;
        this.speedSpec = speedSpec;
        this.curve = TravelCurve.plan(from, to, speedSpec.limits());
    }

    /** ゲーム時刻 tick（partialTickを含めてよい）でのかごの位置。 */
    public double positionAt(double tick) {
        return curve.positionAtTick(tick - startTick);
    }

    /** ゲーム時刻 tick の時点で到着しているかどうか。 */
    public boolean isArrivedAt(double tick) {
        return curve.isArrived((tick - startTick) / TravelCurve.TICKS_PER_SECOND);
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

    public SpeedSpec speedSpec() {
        return speedSpec;
    }

    public TravelCurve curve() {
        return curve;
    }
}
