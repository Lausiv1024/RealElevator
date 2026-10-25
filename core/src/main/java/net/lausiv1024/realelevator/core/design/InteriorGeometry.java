package net.lausiv1024.realelevator.core.design;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;

/**
 * かご内装の細部（壁の継ぎ目、幅木、天井、手すり）の形。座標はかごのローカル座標（{@link CarGeometry} と同じ）。
 *
 * <ul>
 * <li>壁の継ぎ目：側壁と奥壁を、幅に応じて2〜3枚の板に分ける細い縦の線（1/4px）。</li>
 * <li>幅木：側壁と奥壁の下端の、高さ{@value #BASEBOARD_PX}pxの帯。壁から出っ張らせない（壁の面にわずかに浮かせて重ねる）。</li>
 * <li>天井：ダウンライト（四角い照明4つ）、間接照明風（まわりを一段下げた縁取りと中央の光る板）、全面照明（全体が光る板）。
 * 全面照明のほかは、板の継ぎ目（奥行きを3つに分ける細い線）を描く。</li>
 * <li>手すり：車椅子用操作盤のすぐ下の高さに、暗い色の四角い棒を金具3つで支える。</li>
 * </ul>
 */
public final class InteriorGeometry {
    public static final int BASEBOARD_PX = 2;
    /** 手すりの棒の下端の高さ [px]（車椅子用操作盤の板の下端の少し下）。 */
    public static final int HANDRAIL_BOTTOM_PX = 13;
    /** 手すりの、壁の両端から空ける長さ [px]。 */
    public static final int HANDRAIL_MARGIN_PX = 3;
    /** 間接照明風の天井の縁取りの幅 [px]。 */
    public static final int CEILING_FRAME_PX = 3;
    /** ダウンライトの大きさ [px]。 */
    public static final int DOWNLIGHT_PX = 2;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    /** 線の太さ（1/4px）。 */
    private static final double LINE = PX / 4;
    /** 面に重ねるものを浮かせる量。 */
    private static final double LIFT = 0.002;

    public enum Type {
        /** 壁や天井の板の継ぎ目（暗い細い線）。 */
        SEAM,
        /** 幅木（幅木の素材で描く）。 */
        BASEBOARD,
        /** 天井の光らない板（明るい灰色）。 */
        CEILING_PANEL,
        /** 天井の光る部分。 */
        CEILING_GLOW,
        /** 手すりの棒（暗い色）。 */
        HANDRAIL,
        /** 手すりの金具（ステンレス）。 */
        HANDRAIL_BRACKET
    }

    public record Part(Type type, Box box) {
    }

    private InteriorGeometry() {
    }

    /** 内装の細部。through なら二方向出入口（前後）で、奥壁はない。 */
    public static List<Part> parts(CarSpec spec, boolean through, InteriorConfig config) {
        Box in = CarGeometry.interior(spec);
        List<Part> parts = new ArrayList<>();
        // 側壁（−X と +X）
        for (int side = -1; side <= 1; side += 2) {
            double face = side < 0 ? in.minX() : in.maxX();
            double lift = -side * LIFT;
            for (double z : divisions(in.minZ(), in.maxZ())) {
                parts.add(new Part(Type.SEAM, box(face, face + lift, BASEBOARD_PX * PX, in.maxY(), z - LINE / 2, z + LINE / 2, true)));
            }
            parts.add(new Part(Type.BASEBOARD, box(face, face + lift * 2, 0, BASEBOARD_PX * PX, in.minZ(), in.maxZ(), true)));
        }
        if (!through) {
            double face = in.minZ();
            for (double x : divisions(in.minX(), in.maxX())) {
                parts.add(new Part(Type.SEAM, box(face, face + LIFT, BASEBOARD_PX * PX, in.maxY(), x - LINE / 2, x + LINE / 2, false)));
            }
            parts.add(new Part(Type.BASEBOARD, box(face, face + LIFT * 2, 0, BASEBOARD_PX * PX, in.minX(), in.maxX(), false)));
        }
        addCeiling(parts, in, config.ceiling());
        if (config.handrail() != InteriorConfig.HandrailPlacement.NONE) {
            addHandrail(parts, in.minX(), 1, in.minZ(), in.maxZ(), true);
            addHandrail(parts, in.maxX(), -1, in.minZ(), in.maxZ(), true);
            if (config.handrail() == InteriorConfig.HandrailPlacement.SIDES_AND_BACK && !through) {
                addHandrail(parts, in.minZ(), 1, in.minX(), in.maxX(), false);
            }
        }
        return parts;
    }

    /** 壁を板に分ける継ぎ目の位置。幅が1.5ブロック以下なら2枚、それより広ければ3枚。 */
    static List<Double> divisions(double min, double max) {
        int panels = max - min <= 1.5 ? 2 : 3;
        List<Double> positions = new ArrayList<>(panels - 1);
        for (int i = 1; i < panels; i++) {
            positions.add(snap(min + (max - min) * i / panels));
        }
        return positions;
    }

    private static void addCeiling(List<Part> parts, Box in, InteriorConfig.CeilingType type) {
        double top = in.maxY();
        double below = top - LIFT;
        switch (type) {
            case DOWNLIGHT -> {
                parts.add(new Part(Type.CEILING_PANEL, new Box(in.minX(), below, in.minZ(), in.maxX(), top, in.maxZ())));
                double size = DOWNLIGHT_PX * PX;
                for (double x : new double[] {quarter(in.minX(), in.maxX(), 1), quarter(in.minX(), in.maxX(), 3)}) {
                    for (double z : new double[] {quarter(in.minZ(), in.maxZ(), 1), quarter(in.minZ(), in.maxZ(), 3)}) {
                        parts.add(new Part(Type.CEILING_GLOW, new Box(x - size / 2, below - LIFT, z - size / 2, x + size / 2, below, z + size / 2)));
                    }
                }
                addCeilingSeams(parts, in.minX(), in.maxX(), in.minZ(), in.maxZ(), below - LIFT);
            }
            case INDIRECT -> {
                double frame = CEILING_FRAME_PX * PX;
                double drop = top - PX;
                // 縁取り：まわりを1px下げる
                parts.add(new Part(Type.CEILING_PANEL, new Box(in.minX(), drop, in.minZ(), in.maxX(), top, in.minZ() + frame)));
                parts.add(new Part(Type.CEILING_PANEL, new Box(in.minX(), drop, in.maxZ() - frame, in.maxX(), top, in.maxZ())));
                parts.add(new Part(Type.CEILING_PANEL, new Box(in.minX(), drop, in.minZ() + frame, in.minX() + frame, top, in.maxZ() - frame)));
                parts.add(new Part(Type.CEILING_PANEL, new Box(in.maxX() - frame, drop, in.minZ() + frame, in.maxX(), top, in.maxZ() - frame)));
                parts.add(new Part(Type.CEILING_GLOW,
                        new Box(in.minX() + frame, below, in.minZ() + frame, in.maxX() - frame, top, in.maxZ() - frame)));
                addCeilingSeams(parts, in.minX() + frame, in.maxX() - frame, in.minZ() + frame, in.maxZ() - frame, below - LIFT);
            }
            case FULL -> {
                // 全面照明は、光る板の継ぎ目が見えない
                parts.add(new Part(Type.CEILING_GLOW, new Box(in.minX(), below, in.minZ(), in.maxX(), top, in.maxZ())));
            }
        }
    }

    /** 天井の板の継ぎ目：奥行きを3つに分ける、左右に通る細い線。 */
    private static void addCeilingSeams(List<Part> parts, double minX, double maxX, double minZ, double maxZ, double y) {
        for (int i = 1; i < 3; i++) {
            double z = snap(minZ + (maxZ - minZ) * i / 3);
            parts.add(new Part(Type.SEAM, new Box(minX, y - LIFT, z - LINE / 2, maxX, y, z + LINE / 2)));
        }
    }

    /**
     * 壁の面 face に沿った手すり。outward は壁からかごの中への向き（+1 か −1）。
     *
     * @param alongZ 側壁なら true（Z に沿う）、奥壁なら false（X に沿う）
     */
    private static void addHandrail(List<Part> parts, double face, int outward, double min, double max, boolean alongZ) {
        double start = min + HANDRAIL_MARGIN_PX * PX;
        double end = max - HANDRAIL_MARGIN_PX * PX;
        double y0 = HANDRAIL_BOTTOM_PX * PX;
        double y1 = y0 + PX;
        double near = face;
        double mid = face + outward * PX;
        double far = face + outward * 2 * PX;
        parts.add(new Part(Type.HANDRAIL, box(mid, far, y0, y1, start, end, alongZ)));
        for (double at : new double[] {start + PX, snap((start + end) / 2) - PX / 2, end - 2 * PX}) {
            parts.add(new Part(Type.HANDRAIL_BRACKET, box(near, mid, y0, y1, at, at + PX, alongZ)));
        }
    }

    /** 壁に垂直な座標 a〜b、高さ y0〜y1、壁に沿った座標 c0〜c1 の箱。alongZ なら壁に沿う向きが Z（側壁）。 */
    private static Box box(double a, double b, double y0, double y1, double c0, double c1, boolean alongZ) {
        double minA = Math.min(a, b);
        double maxA = Math.max(a, b);
        return alongZ ? new Box(minA, y0, c0, maxA, y1, c1) : new Box(c0, y0, minA, c1, y1, maxA);
    }

    private static double quarter(double min, double max, int quarters) {
        return snap(min + (max - min) * quarters / 4);
    }

    /** 1px の格子にそろえる。 */
    private static double snap(double blocks) {
        return Math.round(blocks / PX) * PX;
    }
}
