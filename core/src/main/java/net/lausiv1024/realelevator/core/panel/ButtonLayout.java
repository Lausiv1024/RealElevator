package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.door.DoorGeometry;

/**
 * ボタンの配置。座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場のボタンは
 * かごがその階に着床したときの位置で表す。ボタンは四角で、取り付け面から{@value #PROTRUSION_QUARTERS}/4px出っ張る。
 * 面の図柄は {@link ButtonGlyphs}。
 *
 * <p>かごの主操作盤は {@link CarPanelLayout}、車椅子用操作盤は {@link WheelchairPanelLayout} が配置を決める。
 * かごの操作盤は、かごの中から扉の方を向いて右側（−X 側）の袖壁に置く。設定によっては左側（+X 側）にも同じものを置く。
 * 片開き（2S）では、反対側の袖壁が狭いので、戸袋の側の袖壁に1つだけ置く（左右の設定は使わない）。
 *
 * <p>乗場のボタンは、三方枠の乗場側の面の、乗場から扉の方を向いて右側（+X 側）の脇の、幅の中央に置く。
 * 乗場の車椅子用ボタンは、▲▼の下に同じ並びで置く。
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
    /** 乗場の下りボタンの下端の高さ（実機 約1000 mm）。 */
    public static final int HALL_BOTTOM_PX = 25;
    /** 乗場の▲と▼のすき間 [px]。 */
    public static final int HALL_GAP_PX = 2;
    /** 乗場の車椅子用の下りボタンの下端の高さ。 */
    public static final int WHEELCHAIR_HALL_BOTTOM_PX = 15;

    public enum Kind {
        CAR_CALL,
        OPEN,
        CLOSE,
        HALL_UP,
        HALL_DOWN,
        /** 非常呼び（飾り。押すと短い音が鳴る）。 */
        EMERGENCY
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
        CarPanelLayout layout = CarPanelLayout.of(spec, floorCount);
        List<Button> buttons = new ArrayList<>((floorCount + 3) * 2);
        for (boolean right : panelJambs(spec, sides)) {
            buttons.addAll(layout.buttons(spec, floorCount, right));
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

    /** 乗場のボタンの左端（−X 側）[px]。 */
    public static int hallButtonMinXPx(CarModel model) {
        return hallButtonMinXPx(CarSpec.of(model));
    }

    public static int hallButtonMinXPx(CarSpec spec) {
        return (DoorGeometry.frameJambCenter2Px(spec, true) - BUTTON_PX) / 2;
    }

    /** 乗場のボタン（▲▼の両方があるとき）の上端 [px]。 */
    public static int hallButtonsTopPx() {
        return HALL_BOTTOM_PX + BUTTON_PX * 2 + HALL_GAP_PX;
    }

    /**
     * 乗場のボタン。最上階には上り、最下階には下りがない。並びは上り、下りの順で、ない方は含めない。
     */
    public static List<Button> hallButtons(CarModel model, int floor, int floorCount) {
        return hallButtons(CarSpec.of(model), floor, floorCount);
    }

    public static List<Button> hallButtons(CarSpec spec, int floor, int floorCount) {
        return hallButtons(spec, floor, floorCount, 0);
    }

    /** 乗場の面を faceOffset だけずらした乗場のボタン（二方向出入口の奥の乗場）。 */
    public static List<Button> hallButtons(CarSpec spec, int floor, int floorCount, double faceOffset) {
        return hallButtons(spec, floor, floorCount, faceOffset, false);
    }

    /**
     * 乗場のボタン。wheelchair なら、▲▼の下に車椅子用の▲▼も置く。並びは上り、下り、車椅子用の上り、下りの順で、ない方は含めない。
     */
    public static List<Button> hallButtons(CarSpec spec, int floor, int floorCount, double faceOffset, boolean wheelchair) {
        List<Button> buttons = new ArrayList<>(4);
        addHallButtons(buttons, spec, floor, floorCount, faceOffset, HALL_BOTTOM_PX, false);
        if (wheelchair) {
            addHallButtons(buttons, spec, floor, floorCount, faceOffset, WHEELCHAIR_HALL_BOTTOM_PX, true);
        }
        return buttons;
    }

    private static void addHallButtons(List<Button> buttons, CarSpec spec, int floor, int floorCount, double faceOffset,
                                       int bottom, boolean wheelchair) {
        double p = CarModel.PIXELS_PER_BLOCK;
        double x = hallButtonMinXPx(spec) / p;
        double face = DoorGeometry.landingFace(spec, faceOffset);
        double size = BUTTON_PX / p;
        if (floor < floorCount - 1) {
            double upBottom = (bottom + BUTTON_PX + HALL_GAP_PX) / p;
            buttons.add(new Button(Kind.HALL_UP, floor, new Box(x, upBottom, face, x + size, upBottom + size, face + PROTRUSION),
                    Normal.POS_Z, wheelchair));
        }
        if (floor > 0) {
            buttons.add(new Button(Kind.HALL_DOWN, floor, new Box(x, bottom / p, face, x + size, bottom / p + size, face + PROTRUSION),
                    Normal.POS_Z, wheelchair));
        }
    }

    private static int px(double blocks) {
        return (int) Math.round(blocks * CarModel.PIXELS_PER_BLOCK);
    }
}
