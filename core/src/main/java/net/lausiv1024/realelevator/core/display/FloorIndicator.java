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
 * <p>表示は階名（{@value #MAX_LABEL_LENGTH}文字まで）と進行方向の矢印で、どちらも {@link DotFont} と同じ 1ドット＝1px のドットで描く。
 * 3文字の階名（「MB1」など）があるエレベーターでは、1px のドットの表示器と乗場の操作盤の表示窓を3文字分に広げる（{@code longLabels}）。
 * 0.5px のドットで 5×7 の文字を使う表示器は、3文字の階名だけ 3×5 の文字にして収める。
 * 表示窓の中の矢印と階名は、ドットの並びの端数を含めてちょうど中央に置く。
 *
 * <p>ドットで出すのは {@link DotFont} にある文字だけ（先頭から{@value #MAX_LABEL_LENGTH}文字まで）。ゲームのフォントで描くとき
 * （{@link FixtureConfig.IndicatorStyle#FONT}）は、階名を空にして矢印だけドットで描き、{@link Frame#label} の場所にフォントで描く。
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
    public static final int MAX_LABEL_LENGTH = 3;
    /** ふつうの表示器の幅に収まる階名の文字数。 */
    public static final int SHORT_LABEL_LENGTH = 2;
    /** ドットが表示面から浮く量。表示面と同じ面に描くとちらつくので、わずかに手前に出す。 */
    private static final double DOT_LIFT = 0.002;
    /** トランサムに埋め込む表示面の厚み。トランサムの面とちらつかないよう、わずかに手前に出す。 */
    private static final double EMBEDDED_DEPTH = 0.004;
    private static final int PX = CarModel.PIXELS_PER_BLOCK;

    /** 階名の欄の幅（2文字と、その間の1px）。 */
    private static final int LABEL_WIDTH = DotFont.WIDTH * 2 + 1;
    /** 3文字の階名の欄の幅。 */
    private static final int LONG_LABEL_WIDTH = DotFont.WIDTH * 3 + 2;
    private static final int ARROW_SIZE = 5;
    /** 主操作盤・車椅子用操作盤の表示窓のドットの大きさ（0.5px）。 */
    private static final double LARGE_DOT = 0.5 / PX;
    /** 0.5px のドットの表示での、階名の欄の幅（2文字と、その間の1ドット）。 */
    private static final int LARGE_LABEL_WIDTH = LargeDotFont.WIDTH * 2 + 1;
    /** 乗場の扉の上の表示器の幅と高さ（ふちを含む）[px]。3文字の階名があるときは幅を {@link #LONG_LABEL_EXTRA_PX} 広げる。 */
    public static final int OVER_DOOR_WIDTH_PX = 16;
    /** 3文字の階名のために、1px のドットの表示器を広げる幅 [px]。 */
    public static final int LONG_LABEL_EXTRA_PX = LONG_LABEL_WIDTH - LABEL_WIDTH;
    public static final int OVER_DOOR_HEIGHT_PX = 7;

    private static final String[] UP_ARROW = {"..#..", ".###.", "#####", "..#..", "..#.."};

    /**
     * 表示器の背面と、点灯しているドット。
     *
     * @param label  階名を描く場所（表示面から浮かせた薄い箱）。フォントで描くときに使う
     * @param normal 表示面の向き（見る人のいる側）
     */
    public record Frame(Box face, List<Box> dots, Box label, Normal normal) {
    }

    /** 1px のドットの表示器の、ふちを除いた幅と高さ（矢印、階名の順に横に並べる）。 */
    private static final int WIDTH = ARROW_SIZE + 2 + LABEL_WIDTH;
    /** 3文字の階名があるときの、1px のドットの表示器の、ふちを除いた幅。 */
    private static final int LONG_WIDTH = ARROW_SIZE + 2 + LONG_LABEL_WIDTH;
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

    /** ドットの表示器に出す階名。ドットにある文字だけを、先頭から{@value #MAX_LABEL_LENGTH}文字まで。 */
    public static String shown(String label) {
        StringBuilder shown = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (DotFont.supports(c) && shown.length() < MAX_LABEL_LENGTH) {
                shown.append(c);
            }
        }
        return shown.toString();
    }

    /** 階名 label を、ドットの表示器にそのまま出せるか（ドットにない文字がなく、{@value #MAX_LABEL_LENGTH}文字以内）。 */
    public static boolean fitsDots(String label) {
        return shown(label).equals(label);
    }

    /** 階名 labels のどれかが、ふつうの表示器の幅（{@value #SHORT_LABEL_LENGTH}文字）に収まらないか。 */
    public static boolean hasLongLabel(String... labels) {
        for (String label : labels) {
            if (shown(label).length() > SHORT_LABEL_LENGTH) {
                return true;
            }
        }
        return false;
    }

    /** 階名の初期値。階名を設定する仕組みができるまでは、下から 1、2、3…。 */
    public static String defaultLabel(int floor) {
        return String.valueOf(floor + 1);
    }

    /** かごの表示器。設定によって1つまたは2つ。 */
    public static List<Frame> car(CarModel model, int floorCount, FixtureConfig config, String label, TravelDirection direction) {
        return car(CarSpec.of(model), floorCount, config, label, direction);
    }

    public static List<Frame> car(CarSpec model, int floorCount, FixtureConfig config, String label, TravelDirection direction) {
        return car(model, floorCount, config, label, direction, false);
    }

    /**
     * かごの表示器。操作盤の上なら、操作盤ごとに（2つ目は設定による）。片開き（2S）では操作盤が1つなので1つ。
     *
     * @param longLabels 3文字の階名があるか（扉の上の表示器を広げる）
     */
    public static List<Frame> car(CarSpec model, int floorCount, FixtureConfig config, String label, TravelDirection direction,
                                  boolean longLabels) {
        int face = px(CarGeometry.interior(model).maxZ());
        if (config.carIndicator() == FixtureConfig.CarIndicator.OVER_DOOR) {
            Box entrance = CarGeometry.entrance(model);
            int width = longLabels ? LONG_WIDTH : WIDTH;
            // かごの中から見て左（+X）の端
            int left = (px(entrance.minX()) + px(entrance.maxX()) + width) / 2;
            return List.of(build(Viewer.INSIDE_CAR, left, px(entrance.maxY()), 0, face, EMBEDDED_DEPTH, label, direction, longLabels));
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
        String text = shown(label);
        // 3文字は 5×7 では収まらないので 3×5 の文字にする
        Font font = text.length() > SHORT_LABEL_LENGTH ? Font.SMALL : Font.LARGE;
        Box labelArea;
        if (vertical) {
            addArrow(dots, grid, direction, (columns - ARROW_SIZE) / 2.0, 1);
            addText(dots, grid, font, text, (columns - font.width(text)) / 2.0, rows - 1 - font.height);
            labelArea = verticalLabelArea(face, columns, rows);
        } else {
            double start = (columns - (ARROW_SIZE + 1 + LARGE_LABEL_WIDTH)) / 2.0;
            addArrow(dots, grid, direction, start, (rows - ARROW_SIZE) / 2.0);
            addText(dots, grid, font, text, start + ARROW_SIZE + 1 + (LARGE_LABEL_WIDTH - font.width(text)) / 2.0, (rows - font.height) / 2.0);
            labelArea = face.cell(start + ARROW_SIZE + 1, 1, LARGE_LABEL_WIDTH, rows - 2);
        }
        return new Frame(window, dots, labelArea, normal);
    }

    /** 上に矢印、下に階名を並べる表示窓の、階名の場所（矢印の下から、まわりに1ドットずつ残す）。 */
    private static Box verticalLabelArea(FaceGrid face, int columns, int rows) {
        return face.cell(1, ARROW_SIZE + 2, columns - 2, rows - ARROW_SIZE - 3);
    }

    /** 階名の文字の大きさ。 */
    private enum Font {
        /** 3×5（{@link DotFont}）。 */
        SMALL(DotFont.WIDTH, DotFont.HEIGHT),
        /** 5×7（{@link LargeDotFont}）。 */
        LARGE(LargeDotFont.WIDTH, LargeDotFont.HEIGHT);

        final int width;
        final int height;

        Font(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /** text を1ドットずつ空けて並べた幅。 */
        int width(String text) {
            return Math.max(0, text.length() * (width + 1) - 1);
        }

        boolean isLit(char c, int x, int y) {
            return this == SMALL ? DotFont.isLit(c, x, y) : LargeDotFont.isLit(c, x, y);
        }
    }

    /** text を、左端の列 column・上端の行 row から1ドットずつ空けて描く。 */
    private static void addText(List<Box> dots, DotGrid grid, Font font, String text, double column, double row) {
        for (int i = 0; i < text.length(); i++) {
            double charColumn = column + i * (font.width + 1);
            for (int y = 0; y < font.height; y++) {
                for (int x = 0; x < font.width; x++) {
                    if (font.isLit(text.charAt(i), x, y)) {
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
        return hallOverDoor(model, bottom, face, label, direction, false);
    }

    /** {@link #hallOverDoor(CarSpec, int, int, String, TravelDirection)} の、longLabels なら3文字の階名の分だけ広げたもの。 */
    public static Frame hallOverDoor(CarSpec model, int bottom, int face, String label, TravelDirection direction, boolean longLabels) {
        Box entrance = CarGeometry.entrance(model);
        int left = (px(entrance.minX()) + px(entrance.maxX()) - overDoorWidthPx(longLabels)) / 2;
        return build(Viewer.LANDING, left, bottom, 1, face, 1.0 / PX, label, direction, longLabels);
    }

    /** 乗場の扉の上の表示器の幅（ふちを含む）[px]。 */
    public static int overDoorWidthPx(boolean longLabels) {
        return OVER_DOOR_WIDTH_PX + (longLabels ? LONG_LABEL_EXTRA_PX : 0);
    }

    /** 乗場の操作盤の表示窓 window（乗場の側の面）に、0.5px のドットで、上に矢印、下に 3×5 ドットの階名を描く。 */
    public static Frame hallWindow(Box window, String label, TravelDirection direction) {
        int columns = (int) Math.round(window.sizeX() / LARGE_DOT);
        int rows = (int) Math.round(window.sizeY() / LARGE_DOT);
        FaceGrid face = new FaceGrid(Normal.POS_Z, window.maxZ(), window.minX(), window.maxY(), LARGE_DOT);
        List<Box> dots = new ArrayList<>();
        String text = shown(label);
        addArrow(dots, face::dot, direction, (columns - ARROW_SIZE) / 2.0, 1);
        addText(dots, face::dot, Font.SMALL, text, (columns - Font.SMALL.width(text)) / 2.0, rows - 1 - DotFont.HEIGHT);
        return new Frame(window, dots, verticalLabelArea(face, columns, rows), Normal.POS_Z);
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
                               String label, TravelDirection direction, boolean longLabels) {
        int width = (longLabels ? LONG_WIDTH : WIDTH) + border * 2;
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
        // 1px のドットは px の格子からずらさない
        DotGrid grid = (column, row) -> {
            int x0 = fromInside ? contentLeft - (int) column - 1 : contentLeft + (int) column;
            int y0 = contentTop - (int) row - 1;
            return new Box(x0 / (double) PX, y0 / (double) PX, Math.min(dotNear, dotFar),
                    (x0 + 1) / (double) PX, (y0 + 1) / (double) PX, Math.max(dotNear, dotFar));
        };

        List<Box> dots = new ArrayList<>();
        addArrow(dots, grid, direction, 0, 0);
        String text = shown(label);
        int labelWidth = longLabels ? LONG_LABEL_WIDTH : LABEL_WIDTH;
        // 欄に収まらない文字は描かない（3文字の階名がないエレベーターに、3文字目が出ないように）
        String fitted = Font.SMALL.width(text) > labelWidth ? text.substring(0, SHORT_LABEL_LENGTH) : text;
        addText(dots, grid, Font.SMALL, fitted, ARROW_SIZE + 2 + (labelWidth - Font.SMALL.width(fitted)) / 2, 0);
        // 階名の欄の左上と右下のドットを囲む箱
        Box first = grid.dot(ARROW_SIZE + 2, 0);
        Box last = grid.dot(ARROW_SIZE + 2 + labelWidth - 1, HEIGHT - 1);
        Box labelArea = new Box(Math.min(first.minX(), last.minX()), Math.min(first.minY(), last.minY()), Math.min(first.minZ(), last.minZ()),
                Math.max(first.maxX(), last.maxX()), Math.max(first.maxY(), last.maxY()), Math.max(first.maxZ(), last.maxZ()));
        return new Frame(background, dots, labelArea, fromInside ? Normal.NEG_Z : Normal.POS_Z);
    }

    private interface DotGrid {
        Box dot(double column, double row);
    }

    private static void addArrow(List<Box> dots, DotGrid grid, TravelDirection direction, double column, double row) {
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

    private static int px(double blocks) {
        return (int) Math.round(blocks * PX);
    }
}
