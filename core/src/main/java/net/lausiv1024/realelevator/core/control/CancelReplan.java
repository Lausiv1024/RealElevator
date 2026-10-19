package net.lausiv1024.realelevator.core.control;

import net.lausiv1024.realelevator.core.motion.Trip;

/**
 * 走行中に、向かっている行先の呼びが2度押しキャンセルで消えたときに、どこに止まるかを決める。
 *
 * <ul>
 * <li>行先の階にまだほかの呼び（乗場呼び・反対側の操作盤のかご呼び）が残っていれば、そのまま行く。</li>
 * <li>その先に止まる階があり、まだ減速を始めていなければ、行先をその階へ延ばす。</li>
 * <li>延ばせなければ、元の行先に止まるが戸は開けず、次の呼びへ向かう。</li>
 * <li>呼びがその先に1つもなければ、止まれる最寄りの階（元の行先より手前で減速が間に合う最初の階、なければ元の行先）に止まり、戸は開けない。</li>
 * </ul>
 *
 * <p>運転曲線は停止から停止までの形しか作れないので、走っている途中から新しい曲線は引けない。延ばすのも縮めるのも、
 * 出発時点から計画し直した曲線が今の状態と一致するときだけできる（{@link Trip#extend}、{@link Trip#retarget}）。
 */
public final class CancelReplan {
    /**
     * @param targetFloor 止まる階
     * @param trip        その階に止まる発車情報（変えなければ元のもの）
     * @param openDoors   着いたら戸を開くか
     */
    public record Outcome(int targetFloor, Trip trip, boolean openDoors) {
    }

    private CancelReplan() {
    }

    /**
     * @param now          今のゲーム時刻
     * @param floors       停止階の床面のY座標（下から順）
     * @param currentFloor 最後に止まった階
     * @param targetFloor  今向かっている階
     */
    public static Outcome replan(CollectiveControl control, Trip trip, long now, int[] floors, int currentFloor, int targetFloor) {
        if (control.hasAnyCallAt(targetFloor)) {
            return new Outcome(targetFloor, trip, true);
        }
        TravelDirection direction = trip.to() > trip.from() ? TravelDirection.UP : TravelDirection.DOWN;
        int next = control.nextStopAfter(targetFloor, direction);
        if (next >= 0) {
            Trip longer = trip.extend(now, floors[next]);
            return longer != null ? new Outcome(next, longer, true) : new Outcome(targetFloor, trip, false);
        }
        return nearestStop(trip, now, floors, currentFloor, targetFloor);
    }

    /**
     * 走行中に、止まれる最寄りの階（元の行先より手前で減速が間に合う最初の階、なければ元の行先）に止まる。
     * 2度押しキャンセルで呼びがなくなったときと、停電のとき（停電時自動着床）に使う。戸を開けるかは呼び出し側が決める。
     */
    public static Outcome nearestStop(Trip trip, long now, int[] floors, int currentFloor, int targetFloor) {
        int step = trip.to() > trip.from() ? 1 : -1;
        for (int floor = currentFloor + step; floor != targetFloor; floor += step) {
            Trip shorter = trip.retarget(now, floors[floor]);
            if (shorter != null) {
                return new Outcome(floor, shorter, false);
            }
        }
        return new Outcome(targetFloor, trip, false);
    }
}
