package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.display.FaceGrid;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;

/**
 * 車椅子用操作盤（かごの左右の側壁の横長の板）の配置。
 *
 * <p>側壁の奥行きの中ほどに、横長の板を側壁から1px浮かせて貼る。板の上部は黒い帯で、中央に表示器、見る人から見て右の端に
 * 青い車椅子マークを置く。行先ボタンは帯の下に段を組み、下の段から若い階を並べる（例：1〜6、その上に7〜12）。
 * いちばん下の段は、左に非常呼び、右に開・閉。
 *
 * <p>ボタンは3px角を基本とし、高くなりすぎるときは2px角にする。並びは、見る人（かごの中から側壁を向いた人）から見て左から右。
 */
public final class WheelchairPanelLayout {
    /** いちばん下の段（非常呼び・開・閉）の下端 [px]。 */
    public static final int BOTTOM_PX = 17;
    /** 板の、側壁の奥行きの両端からのすき間 [px]。 */
    public static final int MARGIN_PX = 2;
    /** 板の上端の限り [px]。 */
    public static final int MAX_TOP_PX = 52;
    /** 黒い帯の高さ [px]。 */
    public static final int BAND_HEIGHT_PX = 5;
    /** 表示窓の大きさ [px]。0.5px のドットで、矢印と 5×7 ドットの階名を横に並べる。 */
    public static final int WINDOW_WIDTH_PX = 9;
    public static final int WINDOW_HEIGHT_PX = 4;
    /** 車椅子マークの大きさ [px]。 */
    public static final int MARK_PX = 4;
    private static final int PLATE_THICKNESS_PX = 1;
    private static final double SURFACE_DEPTH = 0.125 / CarModel.PIXELS_PER_BLOCK;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    /** 車椅子マークの図柄（12×12、細かいドット）。 */
    private static final String[] SYMBOL = {
        "....##......",
        "....##......",
        "............",
        "...####.....",
        "..#.#.......",
        ".#..#####...",
        "#.......#...",
        "#.......#...",
        "#......#.#..",
        ".#....#...#.",
        "..####....##",
        "............"};

    private final int buttonPx;
    private final int columns;
    private final int floorRows;
    private final int widthPx;

    private WheelchairPanelLayout(int buttonPx, int columns, int floorRows, int widthPx) {
        this.buttonPx = buttonPx;
        this.columns = columns;
        this.floorRows = floorRows;
        this.widthPx = widthPx;
    }

    public static WheelchairPanelLayout of(CarSpec spec, int floorCount) {
        int available = px(CarGeometry.interior(spec).maxZ() - CarGeometry.interior(spec).minZ()) - MARGIN_PX * 2;
        WheelchairPanelLayout layout = null;
        for (int size : new int[] {ButtonLayout.BUTTON_PX, ButtonLayout.SMALL_BUTTON_PX}) {
            int pitch = size + ButtonLayout.GAP_PX;
            // 板の両端に1pxずつ残す
            int maxColumns = Math.max(3, (available - 2 + ButtonLayout.GAP_PX) / pitch);
            int rows = floorCount <= 1 ? 1 : Math.max(2, (floorCount + maxColumns - 1) / maxColumns);
            int columns = Math.max(3, (floorCount + rows - 1) / rows);
            int minWidth = WINDOW_WIDTH_PX + MARK_PX + 3;
            int width = Math.min(available, Math.max(columns * pitch - ButtonLayout.GAP_PX + 2, minWidth));
            layout = new WheelchairPanelLayout(size, columns, rows, width);
            if (layout.topPx() <= MAX_TOP_PX) {
                return layout;
            }
        }
        return layout;
    }

    public int buttonPx() {
        return buttonPx;
    }

    public int columns() {
        return columns;
    }

    public int floorRows() {
        return floorRows;
    }

    public int widthPx() {
        return widthPx;
    }

    private int pitch() {
        return buttonPx + ButtonLayout.GAP_PX;
    }

    private int bandBottomPx() {
        return BOTTOM_PX + (floorRows + 1) * pitch();
    }

    /** 板の上端 [px]。 */
    public int topPx() {
        return bandBottomPx() + BAND_HEIGHT_PX;
    }

    /** 側壁 right（かごの中から扉の方を向いて右側、−X 側）の操作盤のボタン。並びは開、閉、1階、2階…、非常呼び。 */
    public List<Button> buttons(CarSpec spec, int floorCount, boolean right) {
        Wall wall = new Wall(spec, right, widthPx);
        int first = (widthPx - (columns * pitch() - ButtonLayout.GAP_PX)) / 2;
        List<Button> buttons = new ArrayList<>(floorCount + 3);
        buttons.add(new Button(Kind.OPEN, -1, wall.button(first + (columns - 2) * pitch(), BOTTOM_PX, buttonPx), wall.normal, true));
        buttons.add(new Button(Kind.CLOSE, -1, wall.button(first + (columns - 1) * pitch(), BOTTOM_PX, buttonPx), wall.normal, true));
        for (int floor = 0; floor < floorCount; floor++) {
            int row = floor / columns;
            int column = floor % columns;
            buttons.add(new Button(Kind.CAR_CALL, floor,
                    wall.button(first + column * pitch(), BOTTOM_PX + (row + 1) * pitch(), buttonPx), wall.normal, true));
        }
        buttons.add(new Button(Kind.EMERGENCY, -1, wall.button(first, BOTTOM_PX, buttonPx), wall.normal, true));
        return buttons;
    }

    /** 側壁 right の操作盤の板・帯・車椅子マーク。表示窓は表示器として描く（{@link #window}）。 */
    public List<PanelPart> parts(CarSpec spec, boolean right) {
        Wall wall = new Wall(spec, right, widthPx);
        List<PanelPart> parts = new ArrayList<>();
        parts.add(new PanelPart(PanelPart.Type.PLATE, wall.box(0, BOTTOM_PX - 1, widthPx, topPx(), 0, PLATE_THICKNESS_PX * PX)));
        double front = PLATE_THICKNESS_PX * PX;
        parts.add(new PanelPart(PanelPart.Type.BAND, wall.box(0, bandBottomPx(), widthPx, topPx(), front, front + SURFACE_DEPTH)));
        Box mark = wall.box(markLeft(), bandBottomPx() + 0.5, markLeft() + MARK_PX, bandBottomPx() + 0.5 + MARK_PX,
                front + SURFACE_DEPTH, front + 2 * SURFACE_DEPTH);
        parts.add(new PanelPart(PanelPart.Type.MARK, mark));
        // 図柄は、マークの中に 1/4px のドットで、まわりに2ドット空けて描く
        double markFront = wall.normalCoordinate(front + 2 * SURFACE_DEPTH);
        FaceGrid grid = new FaceGrid(wall.normal, markFront, wall.horizontal(markLeft() + 0.5),
                (bandBottomPx() + 0.5 + MARK_PX - 0.5) * PX, PX / 4);
        for (int row = 0; row < SYMBOL.length; row++) {
            for (int column = 0; column < SYMBOL[row].length(); column++) {
                if (SYMBOL[row].charAt(column) == '#') {
                    parts.add(new PanelPart(PanelPart.Type.MARK_SYMBOL, grid.dot(column, row)));
                }
            }
        }
        return parts;
    }

    /** 側壁 right の操作盤の表示窓。帯の中で、マークを除いた部分の中央。 */
    public Box window(CarSpec spec, boolean right) {
        Wall wall = new Wall(spec, right, widthPx);
        double front = PLATE_THICKNESS_PX * PX;
        double left = (markLeft() - WINDOW_WIDTH_PX) / 2.0;
        return wall.box(left, bandBottomPx() + 0.5, left + WINDOW_WIDTH_PX, bandBottomPx() + 0.5 + WINDOW_HEIGHT_PX,
                front + SURFACE_DEPTH, front + 2 * SURFACE_DEPTH);
    }

    /** 表示窓の取り付け面の法線（見る人の向きの反対）。 */
    public static Normal normal(boolean right) {
        return right ? Normal.POS_X : Normal.NEG_X;
    }

    private double markLeft() {
        return widthPx - 0.5 - MARK_PX;
    }

    /** 側壁の内面に沿った、見る人から見た座標（u：左から右 [px]、y [px]、d：壁からの距離 [ブロック]）からの変換。 */
    private static final class Wall {
        final Normal normal;
        /** 板の、見る人から見て左端の Z [px]。 */
        final double leftZ;
        /** 見る人の右が +Z なら 1。 */
        final int sign;
        /** 側壁の内面の X [ブロック]。 */
        final double face;

        Wall(CarSpec spec, boolean right, int widthPx) {
            Box interior = CarGeometry.interior(spec);
            normal = normal(right);
            sign = normal.rightZ();
            face = right ? interior.minX() : interior.maxX();
            int minZ = px(interior.minZ());
            int maxZ = px(interior.maxZ());
            int low = Math.floorDiv(minZ + maxZ - widthPx, 2);
            leftZ = sign > 0 ? low : low + widthPx;
        }

        double horizontal(double u) {
            return (leftZ + sign * u) * PX;
        }

        double normalCoordinate(double distance) {
            return face + normal.x() * distance;
        }

        Box box(double u0, double y0, double u1, double y1, double d0, double d1) {
            double z0 = horizontal(u0);
            double z1 = horizontal(u1);
            double x0 = normalCoordinate(d0);
            double x1 = normalCoordinate(d1);
            return new Box(Math.min(x0, x1), y0 * PX, Math.min(z0, z1), Math.max(x0, x1), y1 * PX, Math.max(z0, z1));
        }

        Box button(int u, int y, int size) {
            double front = PLATE_THICKNESS_PX * PX;
            return box(u, y, u + size, y + size, front, front + ButtonLayout.PROTRUSION);
        }
    }

    private static int px(double blocks) {
        return (int) Math.round(blocks * CarModel.PIXELS_PER_BLOCK);
    }
}
