package net.lausiv1024.realelevator.core.control;

import java.util.List;

/**
 * 群管理の乗場呼びの割り当て。乗場呼びを、その階に着くまでの時間の見積もりがいちばん短いかごに割り当てる（割り当てたら変えない）。
 *
 * <p>見積もりは、かごが今の進行方向に、止まる予定の階（かご呼び・乗場呼び）に順に止まりながら進み、その向きの最後の呼びで折り返す
 * として、走る距離を最高速度で割った時間に、止まる回数ごとの時間（{@value #STOP_SECONDS}秒：戸の開閉と開放待機、加減速）と、
 * 走り出しの時間（{@value #START_SECONDS}秒）と、今開いている戸が閉まるまでの時間を足したもの。
 * 呼びと同じ向きに進んでいて、呼びの階がまだ先にあれば、その途中で止まる。
 *
 * <p>見積もりが同じかごが複数あれば（同じ階で待っているかごなど）、乗場のボタンを押した場所にいちばん近いかごに割り当てる。
 */
public final class GroupDispatcher {
    /** 1回止まるのにかかる時間 [秒]（開くのに2.0秒、開放待機4秒、閉じるのに2.5秒、加減速の分）。 */
    public static final double STOP_SECONDS = 10;
    /** 止まっているかごが走り出して最高速度に乗るまでの分 [秒]。 */
    public static final double START_SECONDS = 2;

    /**
     * 割り当てを考えるときのかごの様子。
     *
     * @param y           かごの床面の位置 [ブロック]
     * @param direction   進行方向（呼びがなく止まっていれば NONE）
     * @param speed       最高速度 [ブロック/s]
     * @param doorSeconds 今開いている戸が閉まるまでの時間 [秒]（閉まっていれば0）
     * @param stops       止まる予定の階の位置 [ブロック]（かご呼びと、割り当て済みの乗場呼び）
     * @param available   この呼びに応えられるか（その階に止まり、休止中でない）
     */
    public record CarState(double y, TravelDirection direction, double speed, double doorSeconds, List<Double> stops, boolean available) {
    }

    private GroupDispatcher() {
    }

    /** 見積もりが同じとみなす差 [秒]。 */
    private static final double SAME_TIME = 1e-6;

    /** 位置 floorY の、向き callDirection の乗場呼びを割り当てるかごの番号。応えられるかごがなければ −1。 */
    public static int choose(List<CarState> cars, double floorY, TravelDirection callDirection) {
        return choose(cars, floorY, callDirection, null);
    }

    /**
     * 位置 floorY の、向き callDirection の乗場呼びを割り当てるかごの番号。応えられるかごがなければ −1。
     *
     * @param distances かごごとの、乗場のボタンを押した場所からの距離（cars と同じ並び）。見積もりが同じかごのうち、
     *                  いちばん近いかごを選ぶ。null なら並びの先のかご
     */
    public static int choose(List<CarState> cars, double floorY, TravelDirection callDirection, List<Double> distances) {
        int best = -1;
        double bestTime = Double.POSITIVE_INFINITY;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < cars.size(); i++) {
            CarState car = cars.get(i);
            if (!car.available()) {
                continue;
            }
            double time = estimate(car, floorY, callDirection);
            double distance = distances != null ? distances.get(i) : 0;
            boolean faster = time < bestTime - SAME_TIME;
            boolean sameButCloser = Math.abs(time - bestTime) <= SAME_TIME && distance < bestDistance;
            if (faster || sameButCloser) {
                best = i;
                bestTime = time;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** かご car が、位置 floorY の向き callDirection の乗場呼びに応えるまでの時間の見積もり [秒]。 */
    public static double estimate(CarState car, double floorY, TravelDirection callDirection) {
        double base = car.doorSeconds() + START_SECONDS;
        double v = Math.max(car.speed(), 1e-6);
        if (car.direction() == TravelDirection.NONE) {
            return base + Math.abs(floorY - car.y()) / v;
        }
        // 上りなら座標をそのまま、下りなら上下を入れ替えて、上りとして考える
        int sign = car.direction() == TravelDirection.UP ? 1 : -1;
        double y = car.y() * sign;
        double target = floorY * sign;
        boolean sameDirection = callDirection == car.direction();
        double farthest = y;
        for (double stop : car.stops()) {
            farthest = Math.max(farthest, stop * sign);
        }
        if (target >= y && (sameDirection || target >= farthest)) {
            // 進む途中（またはその向きの最後）で止まる
            return base + (target - y) / v + STOP_SECONDS * count(car.stops(), sign, y, target);
        }
        // その向きの最後の呼びまで行って折り返してから応える
        double turn = Math.max(farthest, target);
        return base + (turn - y + turn - target) / v + STOP_SECONDS * car.stops().size();
    }

    /** 上り（sign で向きをそろえた座標）で、from より先で to より手前の止まる予定の数。 */
    private static int count(List<Double> stops, int sign, double from, double to) {
        int n = 0;
        for (double stop : stops) {
            double s = stop * sign;
            if (s > from + 1e-9 && s < to - 1e-9) {
                n++;
            }
        }
        return n;
    }
}
