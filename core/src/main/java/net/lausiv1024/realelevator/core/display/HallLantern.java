package net.lausiv1024.realelevator.core.display;

import java.util.EnumSet;
import java.util.Set;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

/**
 * ホールランタン（乗場の到着灯）。
 *
 * <p>棒状のランプを上下2段に分け、上の段が上り、下の段が下りを表す。かごが到着して扉が開いている間だけ点滅し、
 * 中間階では進行方向の段を、最上階と最下階では両方の段を点ける。座標はかごのローカル座標で、
 * かごがその階に着床したときの位置で表す。
 *
 * <p>到着チャイムは、到着が確定していて、なおかつ到着の {@link #CHIME_LEAD_TICKS} 前を過ぎたときに鳴らす。
 *
 * <p>位置は {@link FixtureConfig.HallLanternPlacement} で選び、形と位置は
 * {@link net.lausiv1024.realelevator.core.design.LandingLayout#lantern} が決める。
 */
public final class HallLantern {
    /** 点滅の点灯時間 [tick]（0.75秒）。 */
    public static final int BLINK_ON_TICKS = 15;
    /** 点滅の消灯時間 [tick]（0.5秒）。 */
    public static final int BLINK_OFF_TICKS = 10;
    /** 到着チャイムを鳴らすのは、到着のこの時間前から [tick]（6秒）。 */
    public static final int CHIME_LEAD_TICKS = 120;

    public static final Set<TravelDirection> UP_ONLY = Set.of(TravelDirection.UP);
    public static final Set<TravelDirection> DOWN_ONLY = Set.of(TravelDirection.DOWN);
    public static final Set<TravelDirection> BOTH = Set.of(TravelDirection.UP, TravelDirection.DOWN);

    /** ランプの1段。directions はその段が表す進行方向（上下をつなげた段は両方）。 */
    public record Section(Set<TravelDirection> directions, Box box) {
        /** 点灯する方向 lit のどれかを表していれば点灯する。 */
        public boolean isLit(Set<TravelDirection> lit) {
            return directions.stream().anyMatch(lit::contains);
        }
    }

    private HallLantern() {
    }

    /**
     * 点灯する段（点滅の消灯中かどうかは含めない）。
     *
     * @param doorOpen  かごがこの階に着床していて、扉が全閉でないか
     * @param direction かごの進行方向
     */
    public static Set<TravelDirection> litSections(int floor, int floorCount, TravelDirection direction, boolean doorOpen) {
        if (!doorOpen) {
            return EnumSet.noneOf(TravelDirection.class);
        }
        if (floor == 0 || floor == floorCount - 1) {
            return EnumSet.of(TravelDirection.UP, TravelDirection.DOWN);
        }
        return direction == TravelDirection.NONE ? EnumSet.noneOf(TravelDirection.class) : EnumSet.of(direction);
    }

    /** 点滅の点灯中かどうか。ticks は点灯を始めてからの経過時間（partialTickを含めてよい）。点灯から始まる。 */
    public static boolean isBlinkOn(double ticks) {
        double cycle = BLINK_ON_TICKS + BLINK_OFF_TICKS;
        return ticks - Math.floor(ticks / cycle) * cycle < BLINK_ON_TICKS;
    }

    /**
     * 到着チャイムを鳴らすときかどうか。
     *
     * @param arrivalCertain 到着が確定しているか（途中階で呼ばれても止まれないか）
     * @param ticksToArrival 到着までの時間 [tick]
     */
    public static boolean shouldChime(boolean arrivalCertain, double ticksToArrival) {
        return arrivalCertain && ticksToArrival <= CHIME_LEAD_TICKS;
    }
}
