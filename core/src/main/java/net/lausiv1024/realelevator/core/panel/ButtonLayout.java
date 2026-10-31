package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;

/**
 * ボタンの配置。座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場のボタンは
 * かごがその階に着床したときの位置で表す。ボタンは四角で、取り付け面から{@value #PROTRUSION_QUARTERS}/4px出っ張る。
 * 面の図柄は {@link ButtonGlyphs}。
 *
 * <p>かごの主操作盤は {@link CarPanelLayout}、車椅子用操作盤は {@link WheelchairPanelLayout} が配置を決める。
 * かごの操作盤は、かごの中から扉の方を向いて右側（−X 側）の袖壁に置く。設定によっては左側（+X 側）にも同じものを置く。
 * 片開き（2S）では、反対側の袖壁が狭いので、戸袋の側の袖壁に1つだけ置く（左右の設定は使わない）。
 *
 * <p>乗場のボタンは {@link net.lausiv1024.realelevator.core.design.LandingLayout} が配置を決める。
 */
public final class ButtonLayout {
    /** ボタンの大きさ [px]。 */
    public static final int BUTTON_PX = 3;
    /** 行先ボタンが多くて収まらないときのボタンの大きさ [px]。 */
    public static final int SMALL_BUTTON_PX = 2;
    public static final int GAP_PX = 1;
    private static final int PROTRUSION_QUARTERS = 2;
    /** ボタンの出っ張り [ブロック]（0.5px）。 */
    public static final double PROTRUSION = PROTRUSION_QUARTERS / 4.0 / CarModel.PIXELS_PER_BLOCK;

    public enum Kind {
        CAR_CALL,
        OPEN,
        CLOSE,
        HALL_UP,
        HALL_DOWN,
        /** 非常呼び（飾り。押すと短い音が鳴る）。 */
        EMERGENCY,
        /** 開延長（設定で付ける。押すと戸を60秒開けておく。閉ボタンで解除）。 */
        OPEN_EXTEND
    }

    /**
     * @param floor      行先ボタン・乗場のボタンの階（0始まり）。それ以外のボタンでは −1
     * @param normal     ボタンの面の向き（押す人のいる側）
     * @param wheelchair 車椅子用操作盤・乗場の車椅子用ボタンか
     */
    public record Button(Kind kind, int floor, Box box, Normal normal, boolean wheelchair) {
        public Button(Kind kind, int floor, Box box, Normal normal) {
            this(kind, floor, box, normal, false);
        }

        /** Y軸まわりに90°ずつ回したもの（{@link Box#rotateQuarterTurns}）。 */
        public Button rotateQuarterTurns(int quarterTurns) {
            return new Button(kind, floor, box.rotateQuarterTurns(quarterTurns), normal.rotateQuarterTurns(quarterTurns), wheelchair);
        }
    }

    private ButtonLayout() {
    }

    /**
     * 右側だけのかごの操作盤のボタン。
     */
    public static List<Button> carPanel(CarModel model, int floorCount) {
        return carPanel(model, floorCount, FixtureConfig.PanelSides.RIGHT);
    }

    /**
     * かごの操作盤のボタン。並びは、右側の操作盤の開、閉、1階、2階…、非常呼び、続いて両側なら左側の操作盤の同じ並びで、
     * この並びの番号でボタンを指す。
     */
    public static List<Button> carPanel(CarModel model, int floorCount, FixtureConfig.PanelSides sides) {
        return carPanel(CarSpec.of(model), floorCount, sides);
    }

    public static List<Button> carPanel(CarSpec spec, int floorCount, FixtureConfig.PanelSides sides) {
        return carPanel(spec, floorCount, sides, false);
    }

    /**
     * かごの操作盤のボタン。
     *
     * @param openExtension 開延長ボタンを付けるか（各操作盤の並びの最後）
     */
    public static List<Button> carPanel(CarSpec spec, int floorCount, FixtureConfig.PanelSides sides, boolean openExtension) {
        CarPanelLayout layout = CarPanelLayout.of(spec, floorCount);
        List<Button> buttons = new ArrayList<>((floorCount + 3) * 2);
        for (boolean right : panelJambs(spec, sides)) {
            buttons.addAll(layout.buttons(spec, floorCount, right, openExtension));
        }
        return buttons;
    }

    /**
     * 操作盤を置く袖壁（かごの中から見て右側なら true）。並びの順。片開き（2S）では戸袋の側の1つだけ。
     */
    public static List<Boolean> panelJambs(CarSpec spec, FixtureConfig.PanelSides sides) {
        return switch (spec.door()) {
            // 左開き（乗場から見て −X 側の戸袋）は、かごの中から見て右側
            case SIDE_LEFT -> List.of(true);
            case SIDE_RIGHT -> List.of(false);
            default -> sides == FixtureConfig.PanelSides.BOTH ? List.of(true, false) : List.of(true);
        };
    }

    /**
     * 袖壁の内面の左端（−X 側）[px]。
     *
     * @param right かごの中から扉の方を向いて右側（−X 側）の袖壁か
     */
    public static int jambMinPx(CarModel model, boolean right) {
        return jambMinPx(CarSpec.of(model), right);
    }

    public static int jambMinPx(CarSpec spec, boolean right) {
        return right ? px(CarGeometry.interior(spec).minX()) : px(CarGeometry.entrance(spec).maxX());
    }

    /** 袖壁の内面の幅 [px]。right はかごの中から見て右側（−X 側）の袖壁か。 */
    public static int jambWidthPx(CarSpec spec, boolean right) {
        return right
                ? px(CarGeometry.entrance(spec).minX() - CarGeometry.interior(spec).minX())
                : px(CarGeometry.interior(spec).maxX() - CarGeometry.entrance(spec).maxX());
    }

    /** 中央開き（CO）の袖壁の内面の幅 [px]。左右同じ。 */
    public static int jambWidthPx(CarModel model) {
        return jambWidthPx(CarSpec.of(model), true);
    }

    /**
     * 車椅子用操作盤のボタン。かごの中から扉の方を向いて右側（−X 側）の側壁、左側（+X 側）の側壁の順で、
     * それぞれ開、閉、1階、2階…、非常呼びの並び。
     */
    public static List<Button> wheelchairPanel(CarSpec spec, int floorCount) {
        WheelchairPanelLayout layout = WheelchairPanelLayout.of(spec, floorCount);
        List<Button> buttons = new ArrayList<>((floorCount + 3) * 2);
        buttons.addAll(layout.buttons(spec, floorCount, true));
        buttons.addAll(layout.buttons(spec, floorCount, false));
        return buttons;
    }

    private static int px(double blocks) {
        return (int) Math.round(blocks * CarModel.PIXELS_PER_BLOCK);
    }
}
