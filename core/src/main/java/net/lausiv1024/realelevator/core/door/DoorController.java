package net.lausiv1024.realelevator.core.door;

/**
 * 戸の状態遷移と開き具合。
 *
 * <p>開き具合は 0（全閉）〜 1（全開）で表し、状態・開始時刻・開始時の開き具合から決まる。
 * サーバーはこの3つだけを送り、クライアントは同じ式で開き具合を求める。
 * 状態遷移（{@link #tick}）はサーバーだけが行う。
 *
 * <p>フェーズ1で扱う遷移：
 * <ul>
 * <li>閉 → 開動作：{@link #open}（着床、開ボタン、現在階の呼び）</li>
 * <li>開動作 → 開放待機：全開</li>
 * <li>開放待機 → 閉動作：待機時間の終了、{@link #close}（閉ボタン）。待機時間が終わって自動で閉めるときは、
 * 戸閉アナウンスの時間（{@link #setCloseAnnouncementTicks}）だけ待ってから閉め始める。閉ボタンでは待たない</li>
 * <li>閉動作 → 開動作：{@link #obstruct}（セーフティシュー）、{@link #open}（開ボタン）。その時点の開き具合から開き直す</li>
 * <li>閉動作 → 閉：全閉</li>
 * </ul>
 *
 * <p>車椅子用のボタンで開いたときは、ゆっくり閉じる（{@link DoorTiming#wheelchairCloseTicks}）。閉まりきるまで続き、
 * その間に普通のボタンで開き直しても、ゆっくりのまま、開放待機も短くしない。
 */
public final class DoorController {
    /** {@link #tick} で起きたこと。 */
    public enum Event {
        NONE,
        /** 状態が変わった。 */
        STATE_CHANGED,
        /** 開放待機が終わり、戸閉アナウンスを流して閉め始めるのを待ち始めた。 */
        CLOSE_ANNOUNCED
    }

    private final DoorTiming timing;
    /** 自動で閉めるとき、戸閉アナウンスを流してから閉め始めるまでの時間 [tick]。0なら待たない。 */
    private int closeAnnouncementTicks;
    /** 戸閉アナウンスのあと閉め始めるゲーム時刻。待っていなければ −1。サーバーだけが使う。 */
    private long closeAt = -1;
    private DoorState state = DoorState.CLOSED;
    private long startTick;
    private double startFraction;
    private int dwellTicks;
    /** 車椅子用のボタンで開いたので、ゆっくり閉じるか。 */
    private boolean slowClose;

    public DoorController(DoorTiming timing) {
        this.timing = timing;
    }

    /** 戸を開く。開放待機中に呼ぶと、待機時間を数え直す。 */
    public void open(long now, int dwellTicks) {
        open(now, dwellTicks, false);
    }

    /**
     * 戸を開く。開放待機中に呼ぶと、待機時間を数え直す。
     *
     * @param slowClose 車椅子用のボタンで開いたか（ゆっくり閉じる）
     */
    public void open(long now, int dwellTicks, boolean slowClose) {
        switch (state) {
            case CLOSED -> {
                startFraction = 0;
                startTick = now;
                state = DoorState.OPENING;
                this.dwellTicks = dwellTicks;
                this.slowClose = slowClose;
            }
            case CLOSING -> {
                startFraction = fractionAt(now);
                startTick = now;
                state = DoorState.OPENING;
                this.dwellTicks = this.slowClose ? Math.max(this.dwellTicks, dwellTicks) : dwellTicks;
                this.slowClose |= slowClose;
            }
            case OPENING -> {
                this.dwellTicks = Math.max(this.dwellTicks, dwellTicks);
                this.slowClose |= slowClose;
            }
            case OPEN -> {
                startTick = now;
                this.dwellTicks = this.slowClose ? Math.max(this.dwellTicks, dwellTicks) : dwellTicks;
                this.slowClose |= slowClose;
                closeAt = -1;
            }
        }
    }

    /** 閉ボタン。開放待機中なら、待機時間を打ち切り、戸閉アナウンスも待たずにすぐに閉じ始める。 */
    public void close(long now) {
        if (state == DoorState.OPEN) {
            startClosing(now);
        }
    }

    /** 戸閉アナウンスを流して、閉め始めるのを待っているところか。 */
    public boolean isWaitingToClose() {
        return closeAt >= 0;
    }

    /** 自動で閉めるとき、戸閉アナウンスを流してから閉め始めるまでの時間 [tick]。0なら待たない。 */
    public void setCloseAnnouncementTicks(int ticks) {
        this.closeAnnouncementTicks = Math.max(0, ticks);
    }

    /** セーフティシューが物に触れた。閉動作中なら開き直す。待機時間は元の呼びのものを使う。 */
    public void obstruct(long now) {
        if (state == DoorState.CLOSING) {
            open(now, dwellTicks, slowClose);
        }
    }

    /** 時間の経過による遷移を進める。 */
    public Event tick(long now) {
        switch (state) {
            case OPENING -> {
                if (now - startTick >= openingDuration()) {
                    state = DoorState.OPEN;
                    startTick = now;
                    startFraction = 1;
                    return Event.STATE_CHANGED;
                }
            }
            case OPEN -> {
                if (closeAt >= 0) {
                    if (now >= closeAt) {
                        startClosing(now);
                        return Event.STATE_CHANGED;
                    }
                } else if (now - startTick >= dwellTicks) {
                    if (closeAnnouncementTicks > 0) {
                        closeAt = now + closeAnnouncementTicks;
                        return Event.CLOSE_ANNOUNCED;
                    }
                    startClosing(now);
                    return Event.STATE_CHANGED;
                }
            }
            case CLOSING -> {
                if (now - startTick >= closingDuration()) {
                    state = DoorState.CLOSED;
                    startTick = now;
                    startFraction = 0;
                    return Event.STATE_CHANGED;
                }
            }
            case CLOSED -> {
            }
        }
        return Event.NONE;
    }

    /** ゲーム時刻 tick（partialTickを含めてよい）での開き具合。0 が全閉、1 が全開。 */
    public double fractionAt(double tick) {
        return switch (state) {
            case CLOSED -> 0;
            case OPEN -> 1;
            case OPENING -> startFraction + (1 - startFraction) * ease(progress(tick, openingDuration()));
            case CLOSING -> startFraction * (1 - ease(progress(tick, closingDuration())));
        };
    }

    /** サーバーから届いた状態をそのまま反映する（クライアント用、読み込み用）。 */
    public void restore(DoorState state, long startTick, double startFraction, int dwellTicks) {
        restore(state, startTick, startFraction, dwellTicks, false);
    }

    /** サーバーから届いた状態をそのまま反映する（クライアント用、読み込み用）。 */
    public void restore(DoorState state, long startTick, double startFraction, int dwellTicks, boolean slowClose) {
        this.slowClose = slowClose;
        this.state = state;
        this.startTick = startTick;
        this.startFraction = startFraction;
        this.dwellTicks = dwellTicks;
        this.closeAt = -1;
    }

    private void startClosing(long now) {
        closeAt = -1;
        state = DoorState.CLOSING;
        startTick = now;
        startFraction = 1;
    }

    /** 残りの開き具合に比例した時間で開く。途中から開き直すときは、その分早く全開になる。 */
    private double openingDuration() {
        return (1 - startFraction) * timing.openTicks();
    }

    private double closingDuration() {
        return startFraction * (slowClose ? timing.wheelchairCloseTicks() : timing.closeTicks());
    }

    private double progress(double tick, double duration) {
        if (duration <= 0) {
            return 1;
        }
        return Math.clamp((tick - startTick) / duration, 0.0, 1.0);
    }

    /** 動き始めと止まる直前をゆっくりにする。 */
    private static double ease(double u) {
        return (1 - Math.cos(Math.PI * u)) / 2;
    }

    public DoorState state() {
        return state;
    }

    public boolean isClosed() {
        return state == DoorState.CLOSED;
    }

    public long startTick() {
        return startTick;
    }

    public double startFraction() {
        return startFraction;
    }

    public int dwellTicks() {
        return dwellTicks;
    }

    /** 車椅子用のボタンで開いたので、ゆっくり閉じるか。 */
    public boolean isSlowClose() {
        return slowClose;
    }

    public DoorTiming timing() {
        return timing;
    }
}
