package net.lausiv1024.realelevator.core.control;

import java.util.BitSet;

/**
 * 乗合全自動（セレクティブコレクティブ）の運行制御。1台のかごについて、呼びの登録と、次に向かう階の決定を行う。
 *
 * <p>かごは進行方向にある呼びを近い順に処理する。上昇中は、かご呼びと上り方向の乗場呼びがある階に止まり、
 * それより上に止まる階がなければ、最も上にある下り方向の乗場呼びまで行って反転する。下降中も同様。
 *
 * <p>階は0始まりの番号（下から）で表す。かごの位置や走行は呼び出し側が管理し、
 * 停止中で戸が閉じたときに {@link #nextStop}、停止階に着いたときに {@link #arrive} を呼ぶ。
 *
 * <p>走行中に途中の階で呼びが登録されたときは、呼び出し側が {@link #isStopOnTheWay} で止まるべき階かを調べ、
 * 減速が間に合えば行先をその階に変える。間に合わなければ通過し、呼びは残して後で応える。
 */
public final class CollectiveControl {
    public enum PressResult {
        /** 呼びを登録した（応答灯がつく）。 */
        REGISTERED,
        /** すでに登録されていた。 */
        ALREADY_REGISTERED,
        /** かごがその階に止まっていて、すぐに応えられる。登録はせず、呼び出し側が戸を開く。 */
        SERVED_HERE,
        /** 存在しない階、または最上階の上り・最下階の下り。 */
        INVALID
    }

    private final int floorCount;
    private final BitSet carCalls = new BitSet();
    private final BitSet upCalls = new BitSet();
    private final BitSet downCalls = new BitSet();
    private TravelDirection direction = TravelDirection.NONE;

    public CollectiveControl(int floorCount) {
        if (floorCount < 2) {
            throw new IllegalArgumentException("an elevator needs at least 2 floors: " + floorCount);
        }
        this.floorCount = floorCount;
    }

    /**
     * かご呼び（行先ボタン）。
     *
     * @param carFloor かごが今いる（止まっている、または最後に止まった）階
     * @param stopped  かごが停止中かどうか
     */
    public PressResult pressCar(int floor, int carFloor, boolean stopped) {
        if (floor < 0 || floor >= floorCount) {
            return PressResult.INVALID;
        }
        if (stopped && floor == carFloor) {
            return PressResult.SERVED_HERE;
        }
        return register(carCalls, floor, carFloor, stopped);
    }

    /**
     * 乗場呼び（▲▼ボタン）。かごがその階に止まっていて、その方向へ進めるならすぐに応える。
     */
    public PressResult pressHall(int floor, TravelDirection callDirection, int carFloor, boolean stopped) {
        if (floor < 0 || floor >= floorCount
                || callDirection == TravelDirection.NONE
                || (callDirection == TravelDirection.UP && floor == floorCount - 1)
                || (callDirection == TravelDirection.DOWN && floor == 0)) {
            return PressResult.INVALID;
        }
        if (stopped && floor == carFloor
                && (direction == TravelDirection.NONE || direction == callDirection || !hasCallsBeyond(floor, direction))) {
            direction = callDirection;
            return PressResult.SERVED_HERE;
        }
        return register(callDirection == TravelDirection.UP ? upCalls : downCalls, floor, carFloor, stopped);
    }

    /**
     * 行先ボタンの2度押しキャンセル。floor のかご呼びを取り消す。停止中に呼びがなくなったら、進行方向も消す。
     * 走行中は進行方向を残す（着いたときに {@link #arrive} が決め直す）。
     *
     * @return 取り消したか（登録されていなければ false）
     */
    public boolean cancelCar(int floor, boolean stopped) {
        if (floor < 0 || floor >= floorCount || !carCalls.get(floor)) {
            return false;
        }
        carCalls.clear(floor);
        if (stopped && !hasAnyCall()) {
            direction = TravelDirection.NONE;
        }
        return true;
    }

    /** すべての呼びを消し、進行方向もなくす（休止）。 */
    public void clearAll() {
        carCalls.clear();
        upCalls.clear();
        downCalls.clear();
        direction = TravelDirection.NONE;
    }

    /** floor に、かご呼びか、どちらかの向きの乗場呼びがあるか。 */
    public boolean hasAnyCallAt(int floor) {
        return carCalls.get(floor) || upCalls.get(floor) || downCalls.get(floor);
    }

    /**
     * floor から dir の向きに進んだとき、次に止まる階（進行方向を変えずに見る）。なければ −1。
     */
    public int nextStopAfter(int floor, TravelDirection dir) {
        return dir == TravelDirection.NONE ? -1 : nextStopBeyond(floor, dir);
    }

    /**
     * かごが停止中で戸が閉じたときに、次に止まる階を決める。進行方向も更新する。
     *
     * @return 次に止まる階。今いる階で応える呼びがあれば carFloor、呼びがなければ −1
     */
    public int nextStop(int carFloor) {
        if (direction == TravelDirection.NONE) {
            direction = hasCallsBeyond(carFloor, TravelDirection.UP) ? TravelDirection.UP
                    : hasCallsBeyond(carFloor, TravelDirection.DOWN) ? TravelDirection.DOWN
                    : TravelDirection.NONE;
        }
        for (int attempt = 0; attempt < 2 && direction != TravelDirection.NONE; attempt++) {
            if (hallCall(carFloor, direction)) {
                return carFloor;
            }
            int stop = nextStopBeyond(carFloor, direction);
            if (stop >= 0) {
                return stop;
            }
            direction = hasCallsBeyond(carFloor, opposite(direction)) ? opposite(direction) : TravelDirection.NONE;
        }
        if (carCalls.get(carFloor) || upCalls.get(carFloor) || downCalls.get(carFloor)) {
            return carFloor;
        }
        return -1;
    }

    /**
     * かごが floor に止まったときに呼ぶ。この階で応える呼びを消す。
     *
     * @return 応えた呼びの種類。乗場呼びに応えたなら {@link CallType#HALL}（開放待機を長めにする）
     */
    public CallType arrive(int floor) {
        boolean hall = false;
        carCalls.clear(floor);
        if (direction == TravelDirection.NONE) {
            // 方向が決まっていなければ、この階の乗場呼びの方向に進む
            if (upCalls.get(floor) || downCalls.get(floor)) {
                direction = upCalls.get(floor) ? TravelDirection.UP : TravelDirection.DOWN;
            }
        }
        if (direction != TravelDirection.NONE) {
            if (!hallCall(floor, direction) && !hasCallsBeyond(floor, direction)) {
                // この先に呼びがなければ、ここで反転する
                direction = hallCall(floor, opposite(direction)) || hasCallsBeyond(floor, opposite(direction))
                        ? opposite(direction) : TravelDirection.NONE;
            }
            if (direction != TravelDirection.NONE && hallCall(floor, direction)) {
                hallCalls(direction).clear(floor);
                hall = true;
            }
        }
        // 乗場呼びに応えたときは、乗る人の行く方向を保つ。戸が閉じて呼びがなければ nextStop で NONE に戻る
        if (!hall && !hasAnyCall()) {
            direction = TravelDirection.NONE;
        }
        return hall ? CallType.HALL : CallType.CAR;
    }

    /**
     * 今の進行方向で走行中に、floor を通りかかったときに止まるべきかどうか。かご呼びか、進行方向と同じ向きの乗場呼びがあれば止まる。
     * 止まれるか（減速が間に合うか）は呼び出し側が判断する。
     */
    public boolean isStopOnTheWay(int floor) {
        return direction != TravelDirection.NONE && floor >= 0 && floor < floorCount
                && (carCalls.get(floor) || hallCall(floor, direction));
    }

    public boolean hasCarCall(int floor) {
        return carCalls.get(floor);
    }

    public boolean hasHallCall(int floor, TravelDirection callDirection) {
        return callDirection != TravelDirection.NONE && hallCall(floor, callDirection);
    }

    public TravelDirection direction() {
        return direction;
    }

    public int floorCount() {
        return floorCount;
    }

    /** 保存・同期用。かご呼び、上り、下りの順。 */
    public long[][] snapshot() {
        return new long[][] {carCalls.toLongArray(), upCalls.toLongArray(), downCalls.toLongArray()};
    }

    public void restore(long[] car, long[] up, long[] down, TravelDirection direction) {
        restore(carCalls, car);
        restore(upCalls, up);
        restore(downCalls, down);
        this.direction = direction;
    }

    private static void restore(BitSet target, long[] bits) {
        target.clear();
        target.or(BitSet.valueOf(bits));
    }

    private PressResult register(BitSet calls, int floor, int carFloor, boolean stopped) {
        if (calls.get(floor)) {
            return PressResult.ALREADY_REGISTERED;
        }
        calls.set(floor);
        // 停止中で方向が決まっていなければ、戸が開いていてもその場で呼びの方へ方向を決める（方向灯をすぐにつける）
        if (stopped && direction == TravelDirection.NONE && floor != carFloor) {
            direction = floor > carFloor ? TravelDirection.UP : TravelDirection.DOWN;
        }
        return PressResult.REGISTERED;
    }

    /** floor より先（direction 側）で最初に止まる階。なければ、最も遠い反対方向の乗場呼びの階。どちらもなければ −1。 */
    private int nextStopBeyond(int floor, TravelDirection dir) {
        int step = dir == TravelDirection.UP ? 1 : -1;
        BitSet sameDirection = hallCalls(dir);
        for (int f = floor + step; f >= 0 && f < floorCount; f += step) {
            if (carCalls.get(f) || sameDirection.get(f)) {
                return f;
            }
        }
        BitSet otherDirection = hallCalls(opposite(dir));
        for (int f = dir == TravelDirection.UP ? floorCount - 1 : 0; f != floor; f -= step) {
            if (otherDirection.get(f)) {
                return f;
            }
        }
        return -1;
    }

    private boolean hasCallsBeyond(int floor, TravelDirection dir) {
        if (dir == TravelDirection.NONE) {
            return false;
        }
        int step = dir == TravelDirection.UP ? 1 : -1;
        for (int f = floor + step; f >= 0 && f < floorCount; f += step) {
            if (carCalls.get(f) || upCalls.get(f) || downCalls.get(f)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyCall() {
        return !carCalls.isEmpty() || !upCalls.isEmpty() || !downCalls.isEmpty();
    }

    private boolean hallCall(int floor, TravelDirection dir) {
        return hallCalls(dir).get(floor);
    }

    private BitSet hallCalls(TravelDirection dir) {
        return dir == TravelDirection.UP ? upCalls : downCalls;
    }

    private static TravelDirection opposite(TravelDirection dir) {
        return switch (dir) {
            case UP -> TravelDirection.DOWN;
            case DOWN -> TravelDirection.UP;
            case NONE -> TravelDirection.NONE;
        };
    }
}
