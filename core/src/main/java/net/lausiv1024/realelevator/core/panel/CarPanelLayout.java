package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;

/**
 * かごの主操作盤（袖壁の縦長の板）の配置。
 *
 * <p>袖壁の幅いっぱいの板を、床の近くから天井の近くまで、袖壁から1px浮かせて貼る。上から、黒い表示窓（矢印と階）、
 * 黒い帯、非常呼びボタン、行先ボタン（2列）、開・閉ボタン、開延長ボタン（設定で付ける。開ボタンの下）の順に並べる。
 *
 * <p>ボタンは3px角を基本とする。袖壁が狭く、3px角を2列に並べると両脇に1pxずつの余白が残らない（{@value #MIN_JAMB_FOR_LARGE_PX}px 未満の）
 * 機種では、はじめから2px角にする。行先ボタンが多くて収まらないときは、まず操作盤の下端（開・閉ボタンの下端、
 * 実機 約900mm）を床から{@value #MIN_BOTTOM_PX}pxまで下げ、それでも収まらなければ、そのかごのボタンを2px角にする。
 * さらに収まらなければ非常呼びボタンを省く。
 *
 * <p>座標の Z は、袖壁の内面（{@link CarGeometry#interior} の maxZ）から、かごの中（−Z）へ向かって並べる。
 */
public final class CarPanelLayout {
    /** 板の下端 [px]。 */
    public static final int PLATE_BOTTOM_PX = 2;
    /** 板の上端 [px]（天井の2px下）。 */
    public static final int PLATE_TOP_PX = CarModel.CEILING_HEIGHT_PX - 2;
    /** 板の厚み（袖壁から浮かせる量）[px]。 */
    public static final int PLATE_THICKNESS_PX = 1;
    /** 表示窓の高さ [px]。0.5px のドットで、矢印（5ドット）と 5×7 ドットの階名を縦に並べる。 */
    public static final int WINDOW_HEIGHT_PX = 8;
    /** 開・閉ボタンの下端の、ふつうの高さ（実機 約900mm）[px]。 */
    public static final int DEFAULT_BOTTOM_PX = 22;
    /** 行先ボタンが多いときに、開・閉ボタンの下端を下げられる限り [px]。 */
    public static final int MIN_BOTTOM_PX = 12;
    /** 3px角のボタンを使える袖壁の幅の下限 [px]（2列と、その間の1px、両脇の1pxずつ）。 */
    public static final int MIN_JAMB_FOR_LARGE_PX = ButtonLayout.BUTTON_PX * 2 + ButtonLayout.GAP_PX + 2;
    /** 表示窓と帯の、板の面からの厚み [ブロック]。 */
    private static final double SURFACE_DEPTH = 0.125 / CarModel.PIXELS_PER_BLOCK;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;

    private final int buttonPx;
    private final int bottomPx;
    private final boolean emergency;

    private CarPanelLayout(int buttonPx, int bottomPx, boolean emergency) {
        this.buttonPx = buttonPx;
        this.bottomPx = bottomPx;
        this.emergency = emergency;
    }

    /** 機種と扉の方式が spec、停止階が floorCount のときの配置。操作盤を置きうる袖壁のうち狭い方の幅で、ボタンの大きさを決める。 */
    public static CarPanelLayout of(CarSpec spec, int floorCount) {
        int jamb = Integer.MAX_VALUE;
        for (boolean right : ButtonLayout.panelJambs(spec, FixtureConfig.PanelSides.BOTH)) {
            jamb = Math.min(jamb, ButtonLayout.jambWidthPx(spec, right));
        }
        return of(floorCount, jamb >= MIN_JAMB_FOR_LARGE_PX ? ButtonLayout.BUTTON_PX : ButtonLayout.SMALL_BUTTON_PX);
    }

    /** 停止階が floorCount、ボタンの大きさの上限が maxButtonPx のときの配置。 */
    static CarPanelLayout of(int floorCount, int maxButtonPx) {
        int rows = (floorCount + 1) / 2 + 1;
        int[][] tiers = {{ButtonLayout.BUTTON_PX, 1}, {ButtonLayout.SMALL_BUTTON_PX, 1}, {ButtonLayout.SMALL_BUTTON_PX, 0}};
        for (int[] tier : tiers) {
            int size = tier[0];
            if (size > maxButtonPx) {
                continue;
            }
            boolean emergency = tier[1] == 1;
            int bottom = Math.min(DEFAULT_BOTTOM_PX, topLimit(size, emergency) - (rows - 1) * (size + ButtonLayout.GAP_PX) - size);
            if (bottom >= MIN_BOTTOM_PX) {
                return new CarPanelLayout(size, bottom, emergency);
            }
        }
        // それでも収まらないほど多い階床（未決事項）。いちばん詰めた形で、上へはみ出す
        return new CarPanelLayout(ButtonLayout.SMALL_BUTTON_PX, MIN_BOTTOM_PX, false);
    }

    /** 行先ボタンの最上段の上端の限り [px]。 */
    private static int topLimit(int size, boolean emergency) {
        return emergency ? emergencyBottom(size) - ButtonLayout.GAP_PX : bandBottom() - ButtonLayout.GAP_PX;
    }

    public static int windowTop() {
        return PLATE_TOP_PX - 1;
    }

    public static int windowBottom() {
        return windowTop() - WINDOW_HEIGHT_PX;
    }

    private static int bandTop() {
        return windowBottom() - 1;
    }

    private static int bandBottom() {
        return bandTop() - 1;
    }

    private static int emergencyTop() {
        return bandBottom() - 1;
    }

    private static int emergencyBottom(int size) {
        return emergencyTop() - size;
    }

    public int buttonPx() {
        return buttonPx;
    }

    public int bottomPx() {
        return bottomPx;
    }

    public boolean hasEmergency() {
        return emergency;
    }

    /** 行先ボタンの最上段の上端 [px]。 */
    public int buttonsTopPx(int floorCount) {
        int rows = (floorCount + 1) / 2 + 1;
        return bottomPx + (rows - 1) * (buttonPx + ButtonLayout.GAP_PX) + buttonPx;
    }

    /**
     * 袖壁 right（かごの中から扉の方を向いて右側か）の操作盤のボタン。並びは開、閉、1階、2階…、非常呼び（あれば）。
     */
    public List<Button> buttons(CarSpec spec, int floorCount, boolean right) {
        return buttons(spec, floorCount, right, false);
    }

    /**
     * {@link #buttons(CarSpec, int, boolean)} に、openExtension なら開延長ボタンを足したもの（開ボタンの下で、並びの最後）。
     * 開・閉ボタンの下端は {@value #MIN_BOTTOM_PX}px より下がらないので、板の下端（{@value #PLATE_BOTTOM_PX}px）までに必ず収まる。
     */
    public List<Button> buttons(CarSpec spec, int floorCount, boolean right, boolean openExtension) {
        int pitch = buttonPx + ButtonLayout.GAP_PX;
        int jambMin = ButtonLayout.jambMinPx(spec, right);
        int groupWidth = buttonPx * 2 + ButtonLayout.GAP_PX;
        // かごの中から見て、左の列が +X 側
        int rightColumn = jambMin + (ButtonLayout.jambWidthPx(spec, right) - groupWidth) / 2;
        int leftColumn = rightColumn + pitch;
        double face = buttonFace(spec);

        List<Button> buttons = new ArrayList<>(floorCount + 3);
        buttons.add(new Button(Kind.OPEN, -1, button(leftColumn, bottomPx, face), Normal.NEG_Z));
        buttons.add(new Button(Kind.CLOSE, -1, button(rightColumn, bottomPx, face), Normal.NEG_Z));
        for (int floor = 0; floor < floorCount; floor++) {
            int row = floor / 2 + 1;
            int column = floor % 2 == 0 ? leftColumn : rightColumn;
            buttons.add(new Button(Kind.CAR_CALL, floor, button(column, bottomPx + row * pitch, face), Normal.NEG_Z));
        }
        if (emergency) {
            buttons.add(new Button(Kind.EMERGENCY, -1, button(leftColumn, emergencyBottom(buttonPx), face), Normal.NEG_Z));
        }
        if (openExtension) {
            buttons.add(new Button(Kind.OPEN_EXTEND, -1, button(leftColumn, bottomPx - pitch, face), Normal.NEG_Z));
        }
        return buttons;
    }

    /** 袖壁 right の操作盤の板・帯・表示窓。withWindow が false なら表示窓を付けない（表示器を扉の上に付けるときなど）。 */
    public List<PanelPart> parts(CarSpec spec, boolean right, boolean withWindow) {
        int jambMin = ButtonLayout.jambMinPx(spec, right);
        int jambMax = jambMin + ButtonLayout.jambWidthPx(spec, right);
        double wall = CarGeometry.interior(spec).maxZ();
        double plate = wall - PLATE_THICKNESS_PX * PX;
        List<PanelPart> parts = new ArrayList<>(3);
        parts.add(new PanelPart(PanelPart.Type.PLATE,
                new Box(jambMin * PX, PLATE_BOTTOM_PX * PX, plate, jambMax * PX, PLATE_TOP_PX * PX, wall)));
        parts.add(new PanelPart(PanelPart.Type.BAND,
                new Box(jambMin * PX, bandBottom() * PX, plate - SURFACE_DEPTH, jambMax * PX, bandTop() * PX, plate)));
        if (withWindow) {
            parts.add(new PanelPart(PanelPart.Type.WINDOW, window(spec, right)));
        }
        return parts;
    }

    /** 袖壁 right の操作盤の表示窓。両端に0.5pxずつ板を残す。 */
    public static Box window(CarSpec spec, boolean right) {
        int jambMin = ButtonLayout.jambMinPx(spec, right);
        int jambMax = jambMin + ButtonLayout.jambWidthPx(spec, right);
        double plate = CarGeometry.interior(spec).maxZ() - PLATE_THICKNESS_PX * PX;
        return new Box((jambMin + 0.5) * PX, windowBottom() * PX, plate - SURFACE_DEPTH,
                (jambMax - 0.5) * PX, windowTop() * PX, plate);
    }

    /** ボタンを付ける面（板の面）の Z [ブロック]。 */
    private static double buttonFace(CarSpec spec) {
        return CarGeometry.interior(spec).maxZ() - PLATE_THICKNESS_PX * PX;
    }

    private Box button(int minX, int minY, double face) {
        return new Box(minX * PX, minY * PX, face - ButtonLayout.PROTRUSION, (minX + buttonPx) * PX, (minY + buttonPx) * PX, face);
    }
}
