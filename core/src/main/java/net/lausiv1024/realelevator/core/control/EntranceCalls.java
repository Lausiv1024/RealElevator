package net.lausiv1024.realelevator.core.control;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.lausiv1024.realelevator.core.car.Entrance;

/**
 * 二方向出入口で、呼びがどちらの側（正面／奥）で登録されたかを覚える。
 *
 * <p>運行（止まる階と方向）は {@link CollectiveControl} が階と方向だけで決め、ここでは着いたときにどの扉を開くかを決める。
 * かご呼びは押した操作盤の側、乗場呼びは押した乗場の側の扉を開く。両側の扉が同時に開くことはなく、
 * 同じ階で両側に呼びがあるときは、先に登録された側から片側ずつ開く。
 */
public final class EntranceCalls {
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int CAR = 2;

    /** [側][上り・下り・かご呼び] ごとの、呼びのある階。 */
    private final BitSet[][] calls = new BitSet[Entrance.values().length][3];
    /** [側] ごとの、階 → その側に最初に呼びが登録された順番。 */
    private final List<Map<Integer, Long>> order = new ArrayList<>();
    private long counter;

    public EntranceCalls() {
        for (BitSet[] side : calls) {
            for (int i = 0; i < side.length; i++) {
                side[i] = new BitSet();
            }
        }
        for (int i = 0; i < Entrance.values().length; i++) {
            order.add(new HashMap<>());
        }
    }

    private BitSet bits(Entrance side, int kind) {
        return calls[side.ordinal()][kind];
    }

    private static int kind(TravelDirection direction) {
        return direction == TravelDirection.UP ? UP : DOWN;
    }

    private void remember(int floor, Entrance side) {
        order.get(side.ordinal()).putIfAbsent(floor, ++counter);
    }

    /** 側 side の乗場で、floor の direction 向きの呼びが押された。 */
    public void press(int floor, TravelDirection direction, Entrance side) {
        if (direction != TravelDirection.NONE) {
            bits(side, kind(direction)).set(floor);
            remember(floor, side);
        }
    }

    /** 側 side の操作盤で、floor へのかご呼びが押された。 */
    public void pressCar(int floor, Entrance side) {
        bits(side, CAR).set(floor);
        remember(floor, side);
    }

    /** 側 side の乗場の、floor の direction 向きの呼びが登録されているか（応答灯）。 */
    public boolean isRegistered(int floor, TravelDirection direction, Entrance side) {
        return direction != TravelDirection.NONE && bits(side, kind(direction)).get(floor);
    }

    /** 側 side の操作盤の、floor へのかご呼びが登録されているか（応答灯）。 */
    public boolean isCarRegistered(int floor, Entrance side) {
        return bits(side, CAR).get(floor);
    }

    /**
     * かごが floor に着いて、呼びに応えたときに開く扉を、開く順に返す。最初に開く側の呼びの記録だけ消し、
     * あとから開く側の記録（応答灯）は、その扉を開くときに {@link #served} で消す。
     *
     * @param carCall       かご呼びに応えたか
     * @param hallDirection 応えた乗場呼びの方向（応えていなければ NONE）
     * @param available     その階で使う出入口
     */
    public List<Entrance> arrive(int floor, boolean carCall, TravelDirection hallDirection, Set<Entrance> available) {
        List<Entrance> open = wouldOpen(floor, carCall, hallDirection, available);
        served(floor, open.get(0), carCall, hallDirection, available);
        return open;
    }

    /**
     * floor で扉 opened を開いたときに、それで応えた呼び（その扉を開くことになった側のかご呼びと、hallDirection 向きの乗場呼び）の記録を消す。
     *
     * @param available その階で使う出入口（その階にない側の操作盤で押されたかご呼びは、その階にある扉で応える）
     */
    public void served(int floor, Entrance opened, boolean carCall, TravelDirection hallDirection, Set<Entrance> available) {
        for (Entrance side : Entrance.values()) {
            Entrance target = available.contains(side) ? side : available.iterator().next();
            if (target != opened) {
                continue;
            }
            if (carCall) {
                bits(side, CAR).clear(floor);
            }
            if (hallDirection != TravelDirection.NONE) {
                bits(side, kind(hallDirection)).clear(floor);
            }
            // その側にもう呼びがなければ、順番も忘れる
            if (!bits(side, UP).get(floor) && !bits(side, DOWN).get(floor) && !bits(side, CAR).get(floor)) {
                order.get(side.ordinal()).remove(floor);
            }
        }
    }

    /** {@link #arrive} で開く扉（開く順）。呼びの記録は消さない（到着する前に、ランタンをどちらの側で点けるか決めるのに使う）。 */
    public List<Entrance> wouldOpen(int floor, boolean carCall, TravelDirection hallDirection, Set<Entrance> available) {
        List<Entrance> open = new ArrayList<>(2);
        for (Entrance side : Entrance.values()) {
            boolean called = (carCall && bits(side, CAR).get(floor))
                    || (hallDirection != TravelDirection.NONE && bits(side, kind(hallDirection)).get(floor));
            if (called) {
                // その階にない側の操作盤から呼ばれたら、その階にある扉を開く
                Entrance target = available.contains(side) ? side : available.iterator().next();
                if (!open.contains(target)) {
                    open.add(target);
                }
            }
        }
        if (open.isEmpty()) {
            // 呼びの記録がない（一方向出入口や古い保存データ）ときは、その階で使う扉を開く
            open.addAll(available);
        }
        open.sort(Comparator.comparingLong(side -> order.get(side.ordinal()).getOrDefault(floor, Long.MAX_VALUE)));
        return open;
    }

    /** 保存・同期用。側ごとに上り、下り、かご呼びの順。最後に、側ごとの [階, 順番, 階, 順番, …]。 */
    public long[][] snapshot() {
        int sides = Entrance.values().length;
        long[][] out = new long[sides * 4][];
        for (Entrance side : Entrance.values()) {
            for (int kind = 0; kind < 3; kind++) {
                out[side.ordinal() * 3 + kind] = bits(side, kind).toLongArray();
            }
            Map<Integer, Long> sideOrder = order.get(side.ordinal());
            long[] pairs = new long[sideOrder.size() * 2];
            int i = 0;
            for (Map.Entry<Integer, Long> entry : sideOrder.entrySet()) {
                pairs[i++] = entry.getKey();
                pairs[i++] = entry.getValue();
            }
            out[sides * 3 + side.ordinal()] = pairs;
        }
        return out;
    }

    public void restore(long[][] snapshot) {
        int sides = Entrance.values().length;
        counter = 0;
        for (Entrance side : Entrance.values()) {
            for (int kind = 0; kind < 3; kind++) {
                int index = side.ordinal() * 3 + kind;
                BitSet target = bits(side, kind);
                target.clear();
                if (index < snapshot.length && snapshot[index] != null) {
                    target.or(BitSet.valueOf(snapshot[index]));
                }
            }
            Map<Integer, Long> sideOrder = order.get(side.ordinal());
            sideOrder.clear();
            int index = sides * 3 + side.ordinal();
            long[] pairs = index < snapshot.length && snapshot[index] != null ? snapshot[index] : new long[0];
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                sideOrder.put((int) pairs[i], pairs[i + 1]);
                counter = Math.max(counter, pairs[i + 1]);
            }
        }
    }
}
