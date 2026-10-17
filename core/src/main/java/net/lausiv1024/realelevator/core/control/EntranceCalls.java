package net.lausiv1024.realelevator.core.control;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import net.lausiv1024.realelevator.core.car.Entrance;

/**
 * 二方向出入口で、乗場呼びがどちらの側（正面／奥）の乗場で押されたかを覚える。
 *
 * <p>運行（止まる階と方向）は {@link CollectiveControl} が階と方向だけで決め、ここでは着いたときにどの扉を開くかを決める。
 * かご呼びで着いた階ではその階で使うすべての扉、乗場呼びで着いた階では呼ばれた側の扉を開く。
 */
public final class EntranceCalls {
    /** [側][方向（0=上り、1=下り）] ごとの、呼びのある階。 */
    private final BitSet[][] calls = new BitSet[Entrance.values().length][2];

    public EntranceCalls() {
        for (BitSet[] side : calls) {
            side[0] = new BitSet();
            side[1] = new BitSet();
        }
    }

    private BitSet bits(Entrance side, TravelDirection direction) {
        return calls[side.ordinal()][direction == TravelDirection.UP ? 0 : 1];
    }

    /** 側 side の乗場で、floor の direction 向きの呼びが押された。 */
    public void press(int floor, TravelDirection direction, Entrance side) {
        if (direction != TravelDirection.NONE) {
            bits(side, direction).set(floor);
        }
    }

    /** 側 side の乗場の、floor の direction 向きの呼びが登録されているか（応答灯）。 */
    public boolean isRegistered(int floor, TravelDirection direction, Entrance side) {
        return direction != TravelDirection.NONE && bits(side, direction).get(floor);
    }

    /**
     * かごが floor に着いて、呼びに応えたときに開く扉。応えた乗場呼びの記録は消す。
     *
     * @param carCall       かご呼びに応えたか
     * @param hallDirection 応えた乗場呼びの方向（応えていなければ NONE）
     * @param available     その階で使う出入口
     */
    public Set<Entrance> arrive(int floor, boolean carCall, TravelDirection hallDirection, Set<Entrance> available) {
        Set<Entrance> open = wouldOpen(floor, carCall, hallDirection, available);
        if (hallDirection != TravelDirection.NONE) {
            clear(floor, hallDirection);
        }
        return open;
    }

    /** {@link #arrive} で開く扉。呼びの記録は消さない（到着する前に、ランタンをどちらの側で点けるか決めるのに使う）。 */
    public Set<Entrance> wouldOpen(int floor, boolean carCall, TravelDirection hallDirection, Set<Entrance> available) {
        Set<Entrance> open = EnumSet.noneOf(Entrance.class);
        if (carCall) {
            open.addAll(available);
        }
        if (hallDirection != TravelDirection.NONE) {
            for (Entrance side : Entrance.values()) {
                if (bits(side, hallDirection).get(floor)) {
                    open.add(side);
                }
            }
        }
        open.retainAll(available);
        if (open.isEmpty()) {
            // 呼びの記録がない（古い保存データなど）ときは、その階で使う扉をすべて開く
            open.addAll(available);
        }
        return open;
    }

    /** floor の direction 向きの呼びを、どちらの側の記録も消す。 */
    public void clear(int floor, TravelDirection direction) {
        for (Entrance side : Entrance.values()) {
            bits(side, direction).clear(floor);
        }
    }

    /** 保存・同期用。側ごとに上り、下りの順。 */
    public long[][] snapshot() {
        long[][] out = new long[Entrance.values().length * 2][];
        for (Entrance side : Entrance.values()) {
            out[side.ordinal() * 2] = bits(side, TravelDirection.UP).toLongArray();
            out[side.ordinal() * 2 + 1] = bits(side, TravelDirection.DOWN).toLongArray();
        }
        return out;
    }

    public void restore(long[][] snapshot) {
        for (Entrance side : Entrance.values()) {
            for (int d = 0; d < 2; d++) {
                int index = side.ordinal() * 2 + d;
                BitSet target = calls[side.ordinal()][d];
                target.clear();
                if (index < snapshot.length) {
                    target.or(BitSet.valueOf(snapshot[index]));
                }
            }
        }
    }
}
