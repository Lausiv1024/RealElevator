package net.lausiv1024.realelevator.core.display;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

/**
 * 階床表示器。表示する階の決め方と、ドットの配置。
 *
 * <p>表示は階名（2文字まで）と進行方向の矢印で、どちらも {@link DotFont} と同じ 1ドット＝1px のドットで描く。
 * 座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場の表示器はかごがその階に着床したときの位置で表す。
 *
 * <p>並べ方は2通り。
 * <ul>
 * <li>横長：矢印、階名の順に横に並べる。14×5px にふちを付ける。</li>
 * <li>縦長：上に矢印、下に階名。7×11px。袖壁やボタンの上など、幅の狭いところに使う。</li>
 * </ul>
 *
 * <p>位置は {@link FixtureConfig} で選ぶ。
 * <ul>
 * <li>乗場・扉の上：出入口の上の中央に、横長（ふち1px、16×7px）。</li>
 * <li>乗場・ボタンの上：乗場のボタンの上に、縦長。</li>
 * <li>かご・扉の上：出入口の上のトランサム（高さ5px）に、横長（ふちなし）で埋め込む。</li>
 * <li>かご・操作盤の上：操作盤の袖壁の、行先ボタンより上に、縦長。両側の操作盤の場合は、設定により左側にも付ける。</li>
 * </ul>
 */
public final class FloorIndicator {
    /** 階名として表示する文字数。 */
    public static final int MAX_LABEL_LENGTH = 2;
    /** ドットが表示面から浮く量。表示面と同じ面に描くとちらつくので、わずかに手前に出す。 */
    private static final double DOT_LIFT = 0.002;
    /** トランサムに埋め込む表示面の厚み。トランサムの面とちらつかないよう、わずかに手前に出す。 */
    private static final double EMBEDDED_DEPTH = 0.004;
    private static final int PX = CarModel.PIXELS_PER_BLOCK;

    /** 階名の欄の幅（2文字と、その間の1px）。 */
    private static final int LABEL_WIDTH = DotFont.WIDTH * 2 + 1;
    private static final int ARROW_SIZE = 5;
    /** かごの操作盤の上の表示器の下端の最低の高さ（実機 約1800 mm）。 */
    private static final int CAR_PANEL_BOTTOM_PX = 40;
    /** 乗場の扉の上の表示器の下端の、出入口の上端からの高さ。 */
    private static final int HALL_GAP_PX = 2;

    private static final String[] UP_ARROW = {"..#..", ".###.", "#####", "..#..", "..#.."};

    /** 表示器の背面と、点灯しているドット。 */
    public record Frame(Box face, List<Box> dots) {
    }

    private enum Arrangement {
        /** 矢印、階名の順に横に並べる。 */
        HORIZONTAL(ARROW_SIZE + 2 + LABEL_WIDTH, DotFont.HEIGHT),
        /** 上に矢印、下に階名。 */
        VERTICAL(LABEL_WIDTH, ARROW_SIZE + 1 + DotFont.HEIGHT);

        final int width;
        final int height;

        Arrangement(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    /** 表示器を見る側。かごの中からは +Z 向きに見るので左が +X、乗場からは −Z 向きに見るので左が −X。 */
    private enum Viewer {
        INSIDE_CAR,
        LANDING
    }

    private FloorIndicator() {
    }

    /** かごの位置 y に最も近い停止階。階と階の中間を過ぎると切り替わる。 */
    public static int displayedFloor(double y, int[] floors) {
        int nearest = 0;
        for (int i = 1; i < floors.length; i++) {
            if (Math.abs(floors[i] - y) < Math.abs(floors[nearest] - y)) {
                nearest = i;
            }
        }
        return nearest;
    }

    /** 階名の初期値。階名を設定する仕組みができるまでは、下から 1、2、3…。 */
    public static String defaultLabel(int floor) {
        return String.valueOf(floor + 1);
    }

    /** かごの表示器。設定によって1つまたは2つ。 */
    public static List<Frame> car(CarModel model, int floorCount, FixtureConfig config, String label, TravelDirection direction) {
        int face = px(CarGeometry.interior(model).maxZ());
        if (config.carIndicator() == FixtureConfig.CarIndicator.OVER_DOOR) {
            Box entrance = CarGeometry.entrance(model);
            int width = Arrangement.HORIZONTAL.width;
            // かごの中から見て左（+X）の端
            int left = (px(entrance.minX()) + px(entrance.maxX()) + width) / 2;
            return List.of(build(Arrangement.HORIZONTAL, Viewer.INSIDE_CAR, left, px(entrance.maxY()), 0, face, EMBEDDED_DEPTH,
                    label, direction));
        }
        int bottom = Math.max(CAR_PANEL_BOTTOM_PX, ButtonLayout.carPanelTopPx(floorCount) + 2);
        List<Frame> frames = new ArrayList<>(2);
        frames.add(overPanel(model, true, bottom, face, label, direction));
        if (config.panelSides() == FixtureConfig.PanelSides.BOTH && config.leftPanelIndicator()) {
            frames.add(overPanel(model, false, bottom, face, label, direction));
        }
        return frames;
    }

    private static Frame overPanel(CarModel model, boolean right, int bottom, int face, String label, TravelDirection direction) {
        int minX = ButtonLayout.jambMinPx(model, right) + (ButtonLayout.jambWidthPx(model) - Arrangement.VERTICAL.width) / 2;
        return build(Arrangement.VERTICAL, Viewer.INSIDE_CAR, minX + Arrangement.VERTICAL.width, bottom, 0, face, 1.0 / PX,
                label, direction);
    }

    /** 乗場の表示器。 */
    public static Frame hall(CarModel model, FixtureConfig config, String label, TravelDirection direction) {
        int face = px(DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING).maxZ());
        if (config.hallIndicator() == FixtureConfig.HallIndicator.OVER_BUTTONS) {
            // ボタンの列の中央に合わせる
            int buttonCenter2 = ButtonLayout.hallButtonMinXPx(model) * 2 + ButtonLayout.BUTTON_PX;
            int left = (buttonCenter2 - Arrangement.VERTICAL.width) / 2;
            return build(Arrangement.VERTICAL, Viewer.LANDING, left, ButtonLayout.hallButtonsTopPx() + 2, 0, face, 1.0 / PX,
                    label, direction);
        }
        int width = Arrangement.HORIZONTAL.width + 2;
        return build(Arrangement.HORIZONTAL, Viewer.LANDING, -width / 2, px(CarGeometry.entrance(model).maxY()) + HALL_GAP_PX, 1,
                face, 1.0 / PX, label, direction);
    }

    /**
     * 表示器を組み立てる。
     *
     * @param left   見る人から見た左端の X [px]
     * @param bottom 下端の Y [px]
     * @param border ふちの幅 [px]
     * @param face   取り付け面の Z [px]。表示面はそこから見る人の側へ depth だけ出る
     */
    private static Frame build(Arrangement arrangement, Viewer viewer, int left, int bottom, int border, int face, double depth,
                               String label, TravelDirection direction) {
        int width = arrangement.width + border * 2;
        int height = arrangement.height + border * 2;
        boolean fromInside = viewer == Viewer.INSIDE_CAR;
        double faceZ = face / (double) PX;
        double minX = (fromInside ? left - width : left) / (double) PX;
        Box background = fromInside
                ? new Box(minX, bottom / (double) PX, faceZ - depth, minX + width / (double) PX, (bottom + height) / (double) PX, faceZ)
                : new Box(minX, bottom / (double) PX, faceZ, minX + width / (double) PX, (bottom + height) / (double) PX, faceZ + depth);
        double dotNear = fromInside ? background.minZ() - DOT_LIFT : background.maxZ() + DOT_LIFT;
        double dotFar = fromInside ? background.minZ() : background.maxZ();
        int contentLeft = fromInside ? left - border : left + border;
        int contentTop = bottom + height - border;

        // 見る人から見た (列, 行) → ボックス。列は左から、行は上から
        DotGrid grid = (column, row) -> {
            int x0 = fromInside ? contentLeft - column - 1 : contentLeft + column;
            int y0 = contentTop - row - 1;
            return new Box(x0 / (double) PX, y0 / (double) PX, Math.min(dotNear, dotFar),
                    (x0 + 1) / (double) PX, (y0 + 1) / (double) PX, Math.max(dotNear, dotFar));
        };

        List<Box> dots = new ArrayList<>();
        if (arrangement == Arrangement.HORIZONTAL) {
            addArrow(dots, grid, direction, 0, 0);
            addLabel(dots, grid, label, ARROW_SIZE + 2, 0);
        } else {
            addArrow(dots, grid, direction, (LABEL_WIDTH - ARROW_SIZE) / 2, 0);
            addLabel(dots, grid, label, 0, ARROW_SIZE + 1);
        }
        return new Frame(background, dots);
    }

    private interface DotGrid {
        Box dot(int column, int row);
    }

    private static void addArrow(List<Box> dots, DotGrid grid, TravelDirection direction, int column, int row) {
        if (direction == TravelDirection.NONE) {
            return;
        }
        for (int y = 0; y < ARROW_SIZE; y++) {
            String line = UP_ARROW[direction == TravelDirection.UP ? y : ARROW_SIZE - 1 - y];
            for (int x = 0; x < ARROW_SIZE; x++) {
                if (line.charAt(x) == '#') {
                    dots.add(grid.dot(column + x, row + y));
                }
            }
        }
    }

    /** 階名を欄に描く。1文字なら欄の中央に置く。 */
    private static void addLabel(List<Box> dots, DotGrid grid, String label, int column, int row) {
        String text = label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label;
        int start = column + (LABEL_WIDTH - (text.length() * (DotFont.WIDTH + 1) - 1)) / 2;
        for (int i = 0; i < text.length(); i++) {
            int charColumn = start + i * (DotFont.WIDTH + 1);
            for (int y = 0; y < DotFont.HEIGHT; y++) {
                for (int x = 0; x < DotFont.WIDTH; x++) {
                    if (DotFont.isLit(text.charAt(i), x, y)) {
                        dots.add(grid.dot(charColumn + x, row + y));
                    }
                }
            }
        }
    }

    private static int px(double blocks) {
        return (int) Math.round(blocks * PX);
    }
}
