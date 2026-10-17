package net.lausiv1024.realelevator.core.car;

/**
 * かごの機種。寸法はJIS A 4301の値を1.5倍し、1px（1/16ブロック）単位に丸めたもの。
 *
 * <p>P-6・P-9・P-11の出入口幅は、丸めると19pxになるが、中央開きの戸を同じ幅の2枚に分けられるよう20px（実機換算 約833 mm）にする。
 * P-17・P-20の幅（43.2px）とP-20の奥行き（40.8px）は、かごの中心を1pxの格子にそろえるため偶数（44px、40px）に丸める。
 *
 * <p>昇降路の幅は、P-6〜P-15が3ブロック、P-17以上が4ブロック（かごの外形の幅が45px以上になり、3ブロックではロープとレールが収まらない）。
 */
public enum CarModel {
    /** P-6：かご内 1400×850 mm、出入口幅 800 mm。 */
    P6(34, 20, 20, 3),
    /** P-9：かご内 1400×1100 mm、出入口幅 800 mm。 */
    P9(34, 26, 20, 3),
    /** P-11：かご内 1400×1350 mm、出入口幅 800 mm。 */
    P11(34, 32, 20, 3),
    /** P-13：かご内 1600×1350 mm、出入口幅 900 mm。 */
    P13(38, 32, 22, 3),
    /** P-15：かご内 1600×1500 mm、出入口幅 900 mm。 */
    P15(38, 36, 22, 3),
    /** P-17：かご内 1800×1500 mm、出入口幅 1000 mm。 */
    P17(44, 36, 24, 4),
    /** P-20：かご内 1800×1700 mm、出入口幅 1000 mm。 */
    P20(44, 40, 24, 4),
    /** P-24：かご内 2000×1750 mm、出入口幅 1100 mm。 */
    P24(48, 42, 26, 4);

    public static final int PIXELS_PER_BLOCK = 16;
    /** 出入口高さ（実機 2100 mm）。 */
    public static final int ENTRANCE_HEIGHT_PX = 50;
    /** 天井高（実機 2300 mm）。 */
    public static final int CEILING_HEIGHT_PX = 55;

    private final int interiorWidthPx;
    private final int interiorDepthPx;
    private final int entranceWidthPx;
    private final int shaftWidth;

    CarModel(int interiorWidthPx, int interiorDepthPx, int entranceWidthPx, int shaftWidth) {
        this.interiorWidthPx = interiorWidthPx;
        this.interiorDepthPx = interiorDepthPx;
        this.entranceWidthPx = entranceWidthPx;
        this.shaftWidth = shaftWidth;
    }

    /** 昇降路と乗場の三方枠の幅 [ブロック]。 */
    public int shaftWidth() {
        return shaftWidth;
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
