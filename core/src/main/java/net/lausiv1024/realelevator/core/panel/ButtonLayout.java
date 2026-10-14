package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;

/**
 * ボタンの配置。座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場のボタンは
 * かごがその階に着床したときの位置で表す。ボタンは2px角で、取り付け面から1px出っ張る。
 *
 * <p>かごの操作盤は、かごの中から扉の方を向いて右側（−X 側）の袖壁の内面に置く。設定によっては左側（+X 側）にも同じものを置く。
 * 行先ボタンは2列で、下の段から順に、左の列に奇数階、右の列に偶数階を並べる。その下の段に開・閉ボタンを置く。
 *
 * <p>乗場のボタンは、乗場から扉の方を向いて右側（+X 側）、戸が開いたときに滑り込む範囲のさらに外側に置く。
 */
public final class ButtonLayout {
    public static final int BUTTON_PX = 2;
    public static final int GAP_PX = 1;
    public static final int PROTRUSION_PX = 1;
    /** 開・閉ボタンの下端の高さ（実機 約900 mm）。 */
    public static final int CAR_PANEL_BOTTOM_PX = 22;
    /** 乗場の下りボタンの下端の高さ（実機 約1000 mm）。 */
    public static final int HALL_BOTTOM_PX = 25;

    public enum Kind {
        CAR_CALL,
        OPEN,
        CLOSE,
        HALL_UP,
        HALL_DOWN
    }

    /**
     * @param floor 行先ボタンの階（0始まり）。それ以外のボタンでは −1
     */
    public record Button(Kind kind, int floor, Box box) {
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
     * かごの操作盤のボタン。並びは、右側の操作盤の開、閉、1階、2階…、続いて両側なら左側の操作盤の同じ並びで、
     * この並びの番号でボタンを指す。
     */
    public static List<Button> carPanel(CarModel model, int floorCount, FixtureConfig.PanelSides sides) {
        List<Button> buttons = new ArrayList<>((floorCount + 2) * 2);
        addPanel(buttons, model, floorCount, true);
        if (sides == FixtureConfig.PanelSides.BOTH) {
            addPanel(buttons, model, floorCount, false);
        }
        return buttons;
    }

    private static void addPanel(List<Button> buttons, CarModel model, int floorCount, boolean right) {
        int jambMin = jambMinPx(model, right);
        int groupWidth = 2 * BUTTON_PX + GAP_PX;
        // かごの中から見て、左の列が +X 側
        int rightColumn = jambMin + (jambWidthPx(model) - groupWidth) / 2;
        int leftColumn = rightColumn + BUTTON_PX + GAP_PX;
        int face = px(CarGeometry.interior(model).maxZ());

        buttons.add(new Button(Kind.OPEN, -1, carButton(leftColumn, CAR_PANEL_BOTTOM_PX, face)));
        buttons.add(new Button(Kind.CLOSE, -1, carButton(rightColumn, CAR_PANEL_BOTTOM_PX, face)));
        int pitch = BUTTON_PX + GAP_PX;
        for (int floor = 0; floor < floorCount; floor++) {
            int row = floor / 2 + 1;
            int column = floor % 2 == 0 ? leftColumn : rightColumn;
            buttons.add(new Button(Kind.CAR_CALL, floor, carButton(column, CAR_PANEL_BOTTOM_PX + row * pitch, face)));
        }
    }

    /**
     * 袖壁の内面の左端（−X 側）[px]。
     *
     * @param right かごの中から扉の方を向いて右側（−X 側）の袖壁か
     */
    public static int jambMinPx(CarModel model, boolean right) {
        return right ? px(CarGeometry.interior(model).minX()) : px(CarGeometry.entrance(model).maxX());
    }

    /** 袖壁の内面の幅 [px]。 */
    public static int jambWidthPx(CarModel model) {
        return px(CarGeometry.entrance(model).minX() - CarGeometry.interior(model).minX());
    }

    /** かごの操作盤のボタンの上端 [px]。 */
    public static int carPanelTopPx(int floorCount) {
        int rows = (floorCount + 1) / 2;
        return CAR_PANEL_BOTTOM_PX + rows * (BUTTON_PX + GAP_PX) + BUTTON_PX;
    }

    /** 乗場のボタンの左端（−X 側）[px]。 */
    public static int hallButtonMinXPx(CarModel model) {
        return px(DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING).maxX()) + GAP_PX * 2;
    }

    /** 乗場のボタン（▲▼の両方があるとき）の上端 [px]。 */
    public static int hallButtonsTopPx() {
        return HALL_BOTTOM_PX + BUTTON_PX * 2 + GAP_PX * 2;
    }

    /**
     * 乗場のボタン。最上階には上り、最下階には下りがない。並びは上り、下りの順で、ない方は含めない。
     */
    public static List<Button> hallButtons(CarModel model, int floor, int floorCount) {
        int x = hallButtonMinXPx(model);
        int face = px(DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING).maxZ());
        List<Button> buttons = new ArrayList<>(2);
        if (floor < floorCount - 1) {
            buttons.add(new Button(Kind.HALL_UP, floor, Box.ofPixels(x, HALL_BOTTOM_PX + BUTTON_PX + GAP_PX * 2, face,
                    x + BUTTON_PX, hallButtonsTopPx(), face + PROTRUSION_PX)));
        }
        if (floor > 0) {
            buttons.add(new Button(Kind.HALL_DOWN, floor, Box.ofPixels(x, HALL_BOTTOM_PX, face,
                    x + BUTTON_PX, HALL_BOTTOM_PX + BUTTON_PX, face + PROTRUSION_PX)));
        }
        return buttons;
    }

    /** かご内向き（−Z 方向）に出っ張るボタン。 */
    private static Box carButton(int minX, int minY, int face) {
        return Box.ofPixels(minX, minY, face - PROTRUSION_PX, minX + BUTTON_PX, minY + BUTTON_PX, face);
    }

    private static int px(double blocks) {
        return (int) Math.round(blocks * CarModel.PIXELS_PER_BLOCK);
    }
}
