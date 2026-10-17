package net.lausiv1024.realelevator.core.display;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
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
 * <p>位置は {@link FixtureConfig.HallLanternPlacement} で選ぶ。
 * <ul>
 * <li>扉の上：出入口と同じ幅の横長の棒。各段の高さ2px。乗場の階床表示器が扉の上にあるときは、その上に置く。</li>
 * <li>扉の横：乗場から見て出入口の左（ボタンと反対側）の三方枠の脇の、幅の中央に縦長の棒。幅2px、各段7px、段の間1px。
 * 最上階と最下階では上下の段をつなげて1本にする。</li>
 * </ul>
 */
public final class HallLantern {
    /** 点滅の点灯時間 [tick]（0.75秒）。 */
    public static final int BLINK_ON_TICKS = 15;
    /** 点滅の消灯時間 [tick]（0.5秒）。 */
    public static final int BLINK_OFF_TICKS = 10;
    /** 到着チャイムを鳴らすのは、到着のこの時間前から [tick]（6秒）。 */
    public static final int CHIME_LEAD_TICKS = 120;

    /** 扉の上：1段の高さ [px]。 */
    private static final int OVER_SECTION_HEIGHT_PX = 2;
    /** 扉の上：出入口（または階床表示器）の上端からのすき間 [px]。三方枠の高さに収まるよう、1px。 */
    private static final int OVER_GAP_PX = 1;
    /** 扉の横：棒の幅 [px]。 */
    private static final int BESIDE_WIDTH_PX = 2;
    /** 扉の横：1段の高さ [px]。 */
    private static final int BESIDE_SECTION_HEIGHT_PX = 7;
    /** 扉の横：下の段の下端の高さ [px]（中心が実機の約1800 mm）。 */
    private static final int BESIDE_BOTTOM_PX = 36;

    private static final Set<TravelDirection> UP_ONLY = Set.of(TravelDirection.UP);
    private static final Set<TravelDirection> DOWN_ONLY = Set.of(TravelDirection.DOWN);
    private static final Set<TravelDirection> BOTH = Set.of(TravelDirection.UP, TravelDirection.DOWN);

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

    /**
     * ランプの段。上の段（上り）、下の段（下り）の順。扉の横で最上階・最下階のときは、上下をつなげた1段だけ。付けないときは空。
     *
     * @param terminal 最上階または最下階か
     */
    public static List<Section> sections(CarModel model, FixtureConfig config, boolean terminal) {
        if (config.hallLantern() == FixtureConfig.HallLanternPlacement.NONE) {
            return List.of();
        }
        double px = 1.0 / CarModel.PIXELS_PER_BLOCK;
        double face = DoorGeometry.landingFace(model);
        if (config.hallLantern() == FixtureConfig.HallLanternPlacement.BESIDE_DOOR) {
            // 左側の脇の、幅の中央
            double minX = (DoorGeometry.frameJambCenter2Px(model, false) - BESIDE_WIDTH_PX) / 2 * px;
            double maxX = minX + BESIDE_WIDTH_PX * px;
            double bottom = BESIDE_BOTTOM_PX * px;
            double downTop = bottom + BESIDE_SECTION_HEIGHT_PX * px;
            double upBottom = downTop + px;
            double top = upBottom + BESIDE_SECTION_HEIGHT_PX * px;
            if (terminal) {
                return List.of(new Section(BOTH, new Box(minX, bottom, face, maxX, top, face + px)));
            }
            return List.of(
                    new Section(UP_ONLY, new Box(minX, upBottom, face, maxX, top, face + px)),
                    new Section(DOWN_ONLY, new Box(minX, bottom, face, maxX, downTop, face + px)));
        }
        Box entrance = CarGeometry.entrance(model);
        double base = config.hallIndicator() == FixtureConfig.HallIndicator.OVER_DOOR
                ? FloorIndicator.hall(model, config, "", TravelDirection.NONE).face().maxY()
                : entrance.maxY();
        double bottom = base + OVER_GAP_PX * px;
        double middle = bottom + OVER_SECTION_HEIGHT_PX * px;
        return List.of(
                new Section(UP_ONLY, new Box(entrance.minX(), middle, face, entrance.maxX(), middle + OVER_SECTION_HEIGHT_PX * px, face + px)),
                new Section(DOWN_ONLY, new Box(entrance.minX(), bottom, face, entrance.maxX(), middle, face + px)));
    }
}
