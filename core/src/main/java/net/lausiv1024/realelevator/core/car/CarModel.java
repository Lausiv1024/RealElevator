package net.lausiv1024.realelevator.core.car;

/**
 * かごの機種。寸法はJIS A 4301の値を1.5倍し、1px（1/16ブロック）単位に丸めたもの。
 */
public enum CarModel {
    /** P-9：かご内 1400×850 mm、出入口幅 800 mm。 */
    P9(34, 20, 19),
    /** P-11：かご内 1400×1100 mm、出入口幅 800 mm。 */
    P11(34, 26, 19),
    /** P-13：かご内 1600×1350 mm、出入口幅 900 mm。 */
    P13(38, 32, 22);

    public static final int PIXELS_PER_BLOCK = 16;
    /** 出入口高さ（実機 2100 mm）。 */
    public static final int ENTRANCE_HEIGHT_PX = 50;
    /** 天井高（実機 2300 mm）。 */
    public static final int CEILING_HEIGHT_PX = 55;

    private final int interiorWidthPx;
    private final int interiorDepthPx;
    private final int entranceWidthPx;

    CarModel(int interiorWidthPx, int interiorDepthPx, int entranceWidthPx) {
        this.interiorWidthPx = interiorWidthPx;
        this.interiorDepthPx = interiorDepthPx;
        this.entranceWidthPx = entranceWidthPx;
    }

    /** かご内の幅（出入口のある面に平行な方向）[px]。 */
    public int interiorWidthPx() {
        return interiorWidthPx;
    }

    /** かご内の奥行 [px]。 */
    public int interiorDepthPx() {
        return interiorDepthPx;
    }

    /** 出入口の幅 [px]。 */
    public int entranceWidthPx() {
        return entranceWidthPx;
    }
}
