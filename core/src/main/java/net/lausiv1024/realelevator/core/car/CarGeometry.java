package net.lausiv1024.realelevator.core.car;

import java.util.ArrayList;
import java.util.List;

/**
 * かごの形状（床・壁・天井）。描画と当たり判定の両方に使う。
 *
 * <p>座標はかごに固定したローカル座標で、単位はブロック。原点はかご内の床面の中心、+Y が上、
 * +Z が出入口のある正面（袖壁）の向き、X がかご内の幅の方向。
 */
public final class CarGeometry {
    /** 壁の厚み（実機 約50 mm）。 */
    public static final int WALL_PX = 1;
    /** 床の厚み（実機 約100〜150 mm）。 */
    public static final int FLOOR_PX = 2;
    /** 天井の厚み（実機 約100〜150 mm）。 */
    public static final int CEILING_PX = 2;
    /** 展望仕様の奥壁の腰壁の高さ（ガラスの下端）[px]（実機 約900 mm）。 */
    public static final int WAIST_PX = 15;

    public enum PartType {
        FLOOR,
        /** 側壁と奥壁。 */
        WALL,
        /** 正面の袖壁とトランサム（二方向出入口では奥のものも）。 */
        FRONT_WALL,
        CEILING,
        /** 展望仕様の奥壁のガラス（腰壁より上）。 */
        GLASS
    }

    public record Part(PartType type, Box box) {
    }

    private CarGeometry() {
    }

    /** 中央開き（CO）のかごの部位。 */
    public static List<Part> parts(CarModel model) {
        return parts(CarSpec.of(model));
    }

    /**
     * かごを構成する部位。正面の袖壁は出入口の分を空け、左右の袖壁とトランサムに分ける。
     * 出入口の位置は扉の方式で決まる（{@link CarSpec}）。
     */
    public static List<Part> parts(CarSpec model) {
        return parts(model, false);
    }

    /**
     * かごを構成する部位。through なら二方向出入口（前後）で、奥壁にも正面と同じ出入口を開ける
     * （正面の袖壁とトランサムを、かごの中心のまわりに180°回したもの）。
     */
    public static List<Part> parts(CarSpec model, boolean through) {
        return parts(model, through, false);
    }

    /**
     * かごを構成する部位。observation なら展望仕様で、奥壁を腰壁（高さ{@value #WAIST_PX}px）とその上のガラスに分ける。
     * 展望仕様は二方向出入口（前後）とは組み合わせない（through が優先）。
     */
    public static List<Part> parts(CarSpec model, boolean through, boolean observation) {
        List<Part> single = single(model);
        if (!through) {
            if (!observation) {
                return single;
            }
            Box back = single.get(BACK_WALL).box();
            double waist = WAIST_PX / (double) CarModel.PIXELS_PER_BLOCK;
            List<Part> parts = new ArrayList<>(single);
            parts.set(BACK_WALL, new Part(PartType.WALL, new Box(back.minX(), back.minY(), back.minZ(), back.maxX(), waist, back.maxZ())));
            parts.add(new Part(PartType.GLASS, new Box(back.minX(), waist, back.minZ(), back.maxX(), back.maxY(), back.maxZ())));
            return List.copyOf(parts);
        }
        // 奥壁を除き、正面の袖壁とトランサムを180°回したものを奥に足す
        List<Part> parts = new ArrayList<>(single.subList(0, BACK_WALL));
        parts.addAll(single.subList(FRONT_WALLS, single.size()));
        for (Part front : single.subList(FRONT_WALLS, single.size())) {
            parts.add(new Part(front.type(), front.box().rotateQuarterTurns(Entrance.REAR.quarterTurns())));
        }
        return List.copyOf(parts);
    }

    /** {@link #single} の並びでの、奥壁の番号と、正面の袖壁（左・右・トランサム）の始まりの番号。 */
    private static final int BACK_WALL = 4;
    private static final int FRONT_WALLS = 5;

    private static List<Part> single(CarSpec model) {
        int halfWidth = model.interiorWidthPx() / 2;
        int halfDepth = model.interiorDepthPx() / 2;
        int height = CarModel.CEILING_HEIGHT_PX;
        int outerX = halfWidth + WALL_PX;
        int outerZ = halfDepth + WALL_PX;

        int entranceMinX = model.entranceMinXPx();
        int entranceMaxX = model.entranceMaxXPx();

        return List.of(
                new Part(PartType.FLOOR, Box.ofPixels(-outerX, -FLOOR_PX, -outerZ, outerX, 0, outerZ)),
                new Part(PartType.CEILING, Box.ofPixels(-outerX, height, -outerZ, outerX, height + CEILING_PX, outerZ)),
                // 側壁
                new Part(PartType.WALL, Box.ofPixels(-outerX, 0, -halfDepth, -halfWidth, height, halfDepth)),
                new Part(PartType.WALL, Box.ofPixels(halfWidth, 0, -halfDepth, outerX, height, halfDepth)),
                // 奥壁
                new Part(PartType.WALL, Box.ofPixels(-outerX, 0, -outerZ, outerX, height, -halfDepth)),
                // 袖壁（正面）とトランサム
                new Part(PartType.FRONT_WALL, Box.ofPixels(-outerX, 0, halfDepth, entranceMinX, height, outerZ)),
                new Part(PartType.FRONT_WALL, Box.ofPixels(entranceMaxX, 0, halfDepth, outerX, height, outerZ)),
                new Part(PartType.FRONT_WALL, Box.ofPixels(entranceMinX, CarModel.ENTRANCE_HEIGHT_PX, halfDepth,
                        entranceMaxX, height, outerZ)));
    }

    /** 中央開き（CO）のかごの出入口。 */
    public static Box entrance(CarModel model) {
        return entrance(CarSpec.of(model));
    }

    /** 出入口（正面の袖壁の開口部）。奥行は袖壁の厚みの分。 */
    public static Box entrance(CarSpec spec) {
        int halfDepth = spec.interiorDepthPx() / 2;
        return Box.ofPixels(spec.entranceMinXPx(), 0, halfDepth, spec.entranceMaxXPx(), CarModel.ENTRANCE_HEIGHT_PX, halfDepth + WALL_PX);
    }

    public static Box interior(CarSpec spec) {
        return interior(spec.model());
    }

    public static Box exterior(CarSpec spec) {
        return exterior(spec.model());
    }

    /** かご内の空間。 */
    public static Box interior(CarModel model) {
        int halfWidth = model.interiorWidthPx() / 2;
        int halfDepth = model.interiorDepthPx() / 2;
        return Box.ofPixels(-halfWidth, 0, -halfDepth, halfWidth, CarModel.CEILING_HEIGHT_PX, halfDepth);
    }

    /** かご全体の外形。 */
    public static Box exterior(CarModel model) {
        int outerX = model.interiorWidthPx() / 2 + WALL_PX;
        int outerZ = model.interiorDepthPx() / 2 + WALL_PX;
        return Box.ofPixels(-outerX, -FLOOR_PX, -outerZ, outerX, CarModel.CEILING_HEIGHT_PX + CEILING_PX, outerZ);
    }
}
