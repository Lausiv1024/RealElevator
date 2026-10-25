package net.lausiv1024.realelevator.core.door;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.design.InteriorConfig.SecurityWindow;

/**
 * 戸板の防犯窓。戸板を、窓の穴のまわりの枠（左右・上下の4つの箱）と、穴にはめる網入りガラスに分ける。
 *
 * <p>戸板はワールドの向きに回したあとの箱でよい（幅の向きは、X と Z のうち長い方）。窓は戸板の幅の中央に置き、
 * 戸の上から{@value #TOP_PX}px（戸の高さの約1/6）の位置から下へ伸ばす。幅は戸板の幅に割合を掛け、
 * 戸板の幅と偶奇をそろえた整数 [px] に丸める（左右の枠を同じ幅にするため）。
 */
public final class DoorWindows {
    /** 窓の上端の、戸の上端からの距離 [px]。 */
    public static final int TOP_PX = 8;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;

    /** 戸板を、窓の穴と、そのまわりの枠に分けたもの。glass は窓がなければ null。 */
    public record Pieces(List<Box> frame, Box glass) {
    }

    private DoorWindows() {
    }

    /** 窓の幅 [px]。戸板の幅 panelPx と偶奇をそろえる。 */
    public static int widthPx(int panelPx, SecurityWindow window) {
        double wanted = panelPx * window.widthRatio();
        int width = (int) Math.round(wanted);
        if ((panelPx - width) % 2 != 0) {
            // 偶奇がずれたら、割合に近い方へ1px動かす
            width += wanted >= width ? 1 : -1;
        }
        return Math.clamp(width, 1, panelPx - 2);
    }

    /** 窓の高さ [px]。 */
    public static int heightPx(SecurityWindow window) {
        return (int) Math.round(CarModel.ENTRANCE_HEIGHT_PX * window.heightRatio());
    }

    /** 戸板 panel（下端が床、高さは戸の高さ）を、防犯窓 window の穴と枠に分ける。窓なしなら戸板そのまま。 */
    public static Pieces split(Box panel, SecurityWindow window) {
        if (window == SecurityWindow.NONE) {
            return new Pieces(List.of(panel), null);
        }
        boolean alongX = panel.sizeX() >= panel.sizeZ();
        double min = alongX ? panel.minX() : panel.minZ();
        double max = alongX ? panel.maxX() : panel.maxZ();
        int panelPx = (int) Math.round((max - min) / PX);
        double margin = (panelPx - widthPx(panelPx, window)) / 2.0 * PX;
        double holeMin = min + margin;
        double holeMax = max - margin;
        double top = panel.maxY() - TOP_PX * PX;
        double bottom = top - heightPx(window) * PX;
        List<Box> frame = new ArrayList<>(4);
        frame.add(along(panel, alongX, min, holeMin, panel.minY(), panel.maxY()));
        frame.add(along(panel, alongX, holeMax, max, panel.minY(), panel.maxY()));
        frame.add(along(panel, alongX, holeMin, holeMax, top, panel.maxY()));
        frame.add(along(panel, alongX, holeMin, holeMax, panel.minY(), bottom));
        return new Pieces(frame, along(panel, alongX, holeMin, holeMax, bottom, top));
    }

    /** 戸板 panel の、幅の向きに a〜b、高さ y0〜y1 の部分（厚みはそのまま）。 */
    private static Box along(Box panel, boolean alongX, double a, double b, double y0, double y1) {
        return alongX ? new Box(a, y0, panel.minZ(), b, y1, panel.maxZ()) : new Box(panel.minX(), y0, a, panel.maxX(), y1, b);
    }
}
