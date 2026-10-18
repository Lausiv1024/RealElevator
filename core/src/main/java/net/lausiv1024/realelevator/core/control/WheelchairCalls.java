package net.lausiv1024.realelevator.core.control;

import java.util.BitSet;

/**
 * 車椅子用操作盤・乗場の車椅子用ボタンで登録された呼びを覚える。
 *
 * <p>運行（止まる階と方向）は {@link CollectiveControl} が普通の呼びと同じに扱い、ここでは着いたときに
 * 開放待機を延ばしてゆっくり閉じるかどうかを決める。
 */
public final class WheelchairCalls {
    private final BitSet car = new BitSet();
    private final BitSet up = new BitSet();
    private final BitSet down = new BitSet();

    private BitSet hall(TravelDirection direction) {
        return direction == TravelDirection.UP ? up : down;
    }

    /** 車椅子用操作盤で、floor へのかご呼びが押された。 */
    public void pressCar(int floor) {
        car.set(floor);
    }

    /** 乗場の車椅子用ボタンで、floor の direction 向きの呼びが押された。 */
    public void press(int floor, TravelDirection direction) {
        if (direction != TravelDirection.NONE) {
            hall(direction).set(floor);
        }
    }

    /** 乗場の車椅子用ボタンの応答灯。 */
    public boolean isRegistered(int floor, TravelDirection direction) {
        return direction != TravelDirection.NONE && hall(direction).get(floor);
    }

    public boolean isCarRegistered(int floor) {
        return car.get(floor);
    }

    /**
     * かごが floor に着いて呼びに応えた。応えた呼びの記録を消す。
     *
     * @param carCall       かご呼びに応えたか
     * @param hallDirection 応えた乗場呼びの方向（応えていなければ NONE）
     * @return 応えた呼びのどれかが車椅子用のボタンで登録されていたか
     */
    public boolean arrive(int floor, boolean carCall, TravelDirection hallDirection) {
        boolean wheelchair = false;
        if (carCall) {
            wheelchair |= car.get(floor);
            car.clear(floor);
        }
        if (hallDirection != TravelDirection.NONE) {
            wheelchair |= hall(hallDirection).get(floor);
            hall(hallDirection).clear(floor);
        }
        return wheelchair;
    }

    /** 保存・同期用。かご呼び、上り、下りの順。 */
    public long[][] snapshot() {
        return new long[][] {car.toLongArray(), up.toLongArray(), down.toLongArray()};
    }

    public void restore(long[] car, long[] up, long[] down) {
        restore(this.car, car);
        restore(this.up, up);
        restore(this.down, down);
    }

    private static void restore(BitSet target, long[] bits) {
        target.clear();
        target.or(BitSet.valueOf(bits));
    }
}
