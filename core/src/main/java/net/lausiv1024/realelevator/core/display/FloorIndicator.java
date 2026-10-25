package net.lausiv1024.realelevator.core.display;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.CarPanelLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.core.panel.WheelchairPanelLayout;

/**
 * 階床表示器。表示する階の決め方と、ドットの配置。
 *
 * <p>表示は階名（2文字まで）と進行方向の矢印で、どちらも {@link DotFont} と同じ 1ドット＝1px のドットで描く。
 * 座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場の表示器はかごがその階に着床したときの位置で表す。
 *
 * <p>1px のドットの表示器は、矢印、階名の順に横に並べる（14×5px）。
 *
 * <p>位置は {@link FixtureConfig} で選ぶ。乗場の位置は {@link net.lausiv1024.realelevator.core.design.LandingLayout} が決める。
 * <ul>
 * <li>乗場・扉の上：出入口の上の中央に、横長（ふち1px、16×7px）。</li>
 * <li>乗場・操作盤の上部：操作盤の表示窓に、0.5px のドットで縦長（上に矢印、下に 3×5 ドットの階名）。</li>
 * <li>かご・扉の上：出入口の上のトランサム（高さ5px）に、横長（ふちなし）で埋め込む。</li>
 * <li>かご・操作盤の上：主操作盤の板の上部の表示窓（{@link CarPanelLayout#window}）に、0.5px のドットで縦長
 * （上に矢印、下に 5×7 ドットの階名）。両側の操作盤の場合は、設定により左側にも付ける。</li>
 * <li>車椅子用操作盤：板の上の黒い帯の表示窓に、0.5px のドットで横長（矢印、5×7 ドットの階名）。両側の側壁に付ける。</li>
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
    /** 主操作盤・車椅子用操作盤の表示窓のドットの大きさ（0.5px）。 */
    private static final double LARGE_DOT = 0.5 / PX;
    /** 0.5px のドットの表示での、階名の欄の幅（2文字と、その間の1ドット）。 */
    private static final int LARGE_LABEL_WIDTH = LargeDotFont.WIDTH * 2 + 1;
    /** 乗場の扉の上の表示器の幅と高さ（ふちを含む）[px]。 */
    public static final int OVER_DOOR_WIDTH_PX = 16;
    public static final int OVER_DOOR_HEIGHT_PX = 7;

    private static final String[] UP_ARROW = {"..#..", ".###.", "#####", "..#..", "..#.."};

    /** 表示器の背面と、点灯しているドット。 */
    public record Frame(Box face, List<Box> dots) {
    }

    /** 1px のドットの表示器の、ふちを除いた幅と高さ（矢印、階名の順に横に並べる）。 */
    private static final int WIDTH = ARROW_SIZE + 2 + LABEL_WIDTH;
    private static final int HEIGHT = DotFont.HEIGHT;

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

    /** この間隔 [ブロック] 以上ある階間（急行区間など）では、途中でハイフンを出す。 */
    public static final int LONG_GAP = 10;
    /** 長い階間でも、停止階からこの距離 [ブロック] 未満のあいだは、その階を出す（約1階分）。 */
    public static final double NEAR_FLOOR = 4.0;
    /** 長い階間の途中で、階名の代わりに出す文字。 */
    public static final String BETWEEN_FLOORS = "--";

    /**
     * かごの位置 y で出す階名。長い階間（{@value #LONG_GAP} ブロック以上）の途中で、どちらの停止階からも
     * {@value #NEAR_FLOOR} ブロック以上離れているあいだは {@value #BETWEEN_FLOORS}、それ以外は {@link #displayedFloor} の階名。
     *
     * @param labels 停止階の階名（下から順）
     */
    public static String displayedLabel(double y, int[] floors, IntFunction<String> labels) {
        for (int i = 1; i < floors.length; i++) {
            int below = floors[i - 1];
            int above = floors[i];
            if (above - below >= LONG_GAP && y - below >= NEAR_FLOOR && above - y >= NEAR_FLOOR) {
                return BETWEEN_FLOORS;
            }
        }
        return labels.apply(displayedFloor(y, floors));
    }

    /** 階名の初期値。階名を設定する仕組みができるまでは、下から 1、2、3…。 */
    public static String defaultLabel(int floor) {
        return String.valueOf(floor + 1);
    }

    /** かごの表示器。設定によって1つまたは2つ。 */
    public static List<Frame> car(CarModel model, int floorCount, FixtureConfig config, String label, TravelDirection direction) {
        return car(CarSpec.of(model), floorCount, config, label, direction);
    }

    /** かごの表示器。操作盤の上なら、操作盤ごとに（2つ目は設定による）。片開き（2S）では操作盤が1つなので1つ。 */
    public static List<Frame> car(CarSpec model, int floorCount, FixtureConfig config, String label, TravelDirection direction) {
        int face = px(CarGeometry.interior(model).maxZ());
        if (config.carIndicator() == FixtureConfig.CarIndicator.OVER_DOOR) {
            Box entrance = CarGeometry.entrance(model);
            // かごの中から見て左（+X）の端
            int left = (px(entrance.minX()) + px(entrance.maxX()) + WIDTH) / 2;
            return List.of(build(Viewer.INSIDE_CAR, left, px(entrance.maxY()), 0, face, EMBEDDED_DEPTH, label, direction));
        }
        List<Boolean> jambs = ButtonLayout.panelJambs(model, config.panelSides());
        List<Frame> frames = new ArrayList<>(2);
        for (int i = 0; i < jambs.size(); i++) {
            if (i == 0 || config.leftPanelIndicator()) {
                frames.add(large(CarPanelLayout.window(model, jambs.get(i)), Normal.NEG_Z, true, label, direction));
            }
        }
        return frames;
    }

    /** 主操作盤の、表示窓を付ける袖壁（かごの中から見て右側なら true）。並びは {@link #car} の表示器と同じ。 */
    public static List<Boolean> carWindowJambs(CarSpec model, FixtureConfig config) {
        if (config.carIndicator() == FixtureConfig.CarIndicator.OVER_DOOR) {
            return List.of();
        }
        List<Boolean> jambs = ButtonLayout.panelJambs(model, config.panelSides());
        return jambs.size() > 1 && !config.leftPanelIndicator() ? jambs.subList(0, 1) : jambs;
    }

    /** 車椅子用操作盤の表示器（右の側壁、左の側壁の順）。 */
    public static List<Frame> wheelchair(CarSpec model, int floorCount, String label, TravelDirection direction) {
        WheelchairPanelLayout layout = WheelchairPanelLayout.of(model, floorCount);
        return List.of(
                large(layout.window(model, true), WheelchairPanelLayout.normal(true), false, label, direction),
                large(layout.window(model, false), WheelchairPanelLayout.normal(false), false, label, direction));
    }

    /**
     * 0.5px のドットで描く表示器。vertical なら上に矢印、下に階名。そうでなければ矢印、階名の順に横に並べる。
     *
     * @param window 表示窓。ドットはその normal の側の面に描く
     */
    private static Frame large(Box window, Normal normal, boolean vertical, String label, TravelDirection direction) {
        boolean alongX = normal.x() == 0;
        double plane = switch (normal) {
            case POS_X -> window.maxX();
            case NEG_X -> window.minX();
            case POS_Z -> window.maxZ();
            case NEG_Z -> window.minZ();
        };
        int rightSign = alongX ? normal.rightX() : normal.rightZ();
        double left = alongX ? (rightSign > 0 ? window.minX() : window.maxX()) : (rightSign > 0 ? window.minZ() : window.maxZ());
        double width = alongX ? window.sizeX() : window.sizeZ();
        int columns = (int) Math.round(width / LARGE_DOT);
        int rows = (int) Math.round(window.sizeY() / LARGE_DOT);
        FaceGrid face = new FaceGrid(normal, plane, left, window.maxY(), LARGE_DOT);
        DotGrid grid = face::dot;
        List<Box> dots = new ArrayList<>();
        if (vertical) {
            addArrow(dots, grid, direction, (columns - ARROW_SIZE) / 2, 1);
            addLargeLabel(dots, grid, label, (columns - LARGE_LABEL_WIDTH) / 2, rows - 1 - LargeDotFont.HEIGHT);
        } else {
            int start = (columns - (ARROW_SIZE + 1 + LARGE_LABEL_WIDTH)) / 2;
            addArrow(dots, grid, direction, start, (rows - ARROW_SIZE) / 2);
            addLargeLabel(dots, grid, label, start + ARROW_SIZE + 1, (rows - LargeDotFont.HEIGHT) / 2);
        }
        return new Frame(window, dots);
    }

    /** 階名を 5×7 ドットの文字で欄に描く。1文字なら欄の中央に置く。 */
    private static void addLargeLabel(List<Box> dots, DotGrid grid, String label, int column, int row) {
        String text = label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label;
        int start = column + (LARGE_LABEL_WIDTH - (text.length() * (LargeDotFont.WIDTH + 1) - 1)) / 2;
        for (int i = 0; i < text.length(); i++) {
            int charColumn = start + i * (LargeDotFont.WIDTH + 1);
            for (int y = 0; y < LargeDotFont.HEIGHT; y++) {
                for (int x = 0; x < LargeDotFont.WIDTH; x++) {
                    if (LargeDotFont.isLit(text.charAt(i), x, y)) {
                        dots.add(grid.dot(charColumn + x, row + y));
                    }
                }
            }
        }
    }

    /**
     * 乗場の扉の上の表示器。出入口の中央に、横長（ふち1px）で付ける。
     *
     * @param bottom 下端の Y [px]
     * @param face   取り付け面の Z [px]。表示面はそこから乗場の側へ1px出る
     */
    public static Frame hallOverDoor(CarSpec model, int bottom, int face, String label, TravelDirection direction) {
        Box entrance = CarGeometry.entrance(model);
        int left = (px(entrance.minX()) + px(entrance.maxX()) - OVER_DOOR_WIDTH_PX) / 2;
        return build(Viewer.LANDING, left, bottom, 1, face, 1.0 / PX, label, direction);
    }

    /** 乗場の操作盤の表示窓 window（乗場の側の面）に、0.5px のドットで、上に矢印、下に 3×5 ドットの階名を描く。 */
    public static Frame hallWindow(Box window, String label, TravelDirection direction) {
        int columns = (int) Math.round(window.sizeX() / LARGE_DOT);
        int rows = (int) Math.round(window.sizeY() / LARGE_DOT);
        FaceGrid face = new FaceGrid(Normal.POS_Z, window.maxZ(), window.minX(), window.maxY(), LARGE_DOT);
        List<Box> dots = new ArrayList<>();
        addArrow(dots, face::dot, direction, (columns - ARROW_SIZE) / 2, 1);
        addLabel(dots, face::dot, label, (columns - LABEL_WIDTH) / 2, rows - 1 - DotFont.HEIGHT);
        return new Frame(window, dots);
    }

    /**
     * 表示器を組み立てる。
     *
     * @param left   見る人から見た左端の X [px]
     * @param bottom 下端の Y [px]
     * @param border ふちの幅 [px]
     * @param face   取り付け面の Z [px]。表示面はそこから見る人の側へ depth だけ出る
     */
    private static Frame build(Viewer viewer, int left, int bottom, int border, int face, double depth,
                               String label, TravelDirection direction) {
        int width = WIDTH + border * 2;
        int height = HEIGHT + border * 2;
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
        addArrow(dots, grid, direction, 0, 0);
        addLabel(dots, grid, label, ARROW_SIZE + 2, 0);
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
