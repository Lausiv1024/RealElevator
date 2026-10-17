package net.lausiv1024.realelevator.core.car;

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

    public enum PartType {
        FLOOR,
        WALL,
        CEILING
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
                new Part(PartType.WALL, Box.ofPixels(-outerX, 0, halfDepth, entranceMinX, height, outerZ)),
                new Part(PartType.WALL, Box.ofPixels(entranceMaxX, 0, halfDepth, outerX, height, outerZ)),
                new Part(PartType.WALL, Box.ofPixels(entranceMinX, CarModel.ENTRANCE_HEIGHT_PX, halfDepth,
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
