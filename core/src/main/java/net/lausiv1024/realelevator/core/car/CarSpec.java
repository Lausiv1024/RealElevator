package net.lausiv1024.realelevator.core.car;

/**
 * かごの仕様。機種と扉の方式。出入口の幅と位置は、扉の方式で変わる。
 *
 * <ul>
 * <li>CO：出入口は機種の幅で、正面の中央。</li>
 * <li>2S：出入口は機種の幅で、戸袋の側の袖壁を戸1枚分（（出入口の幅＋2px）÷2）にし、残りを反対側の袖壁にする。</li>
 * <li>4CO：出入口は機種の4CO用の幅で、正面の中央。</li>
 * </ul>
 *
 * @param model 機種
 * @param door  扉の方式。機種で選べない方式なら、CO にする
 */
public record CarSpec(CarModel model, DoorType door) {
    /** 2Sの戸の、閉じたときの2枚の重なり [px]。 */
    public static final int SIDE_OVERLAP_PX = 2;
    /** 4COの戸の、閉じたときの片側2枚の重なり [px]。 */
    public static final int FOUR_CENTER_OVERLAP_PX = 1;

    public CarSpec {
        if (!door.isAvailable(model)) {
            door = DoorType.CENTER;
        }
    }

    /** 中央開き（CO）のかご。 */
    public static CarSpec of(CarModel model) {
        return new CarSpec(model, DoorType.CENTER);
    }

    public int interiorWidthPx() {
        return model.interiorWidthPx();
    }

    public int interiorDepthPx() {
        return model.interiorDepthPx();
    }

    public int shaftWidth() {
        return model.shaftWidth();
    }

    /** 出入口の幅 [px]。 */
    public int entranceWidthPx() {
        return door == DoorType.FOUR_CENTER ? model.fourPanelEntranceWidthPx() : model.entranceWidthPx();
    }

    /** 戸1枚の幅 [px]。 */
    public int panelWidthPx() {
        int entrance = entranceWidthPx();
        return switch (door) {
            case CENTER -> entrance / 2;
            case SIDE_LEFT, SIDE_RIGHT -> (entrance + SIDE_OVERLAP_PX) / 2;
            case FOUR_CENTER -> (entrance / 2 + FOUR_CENTER_OVERLAP_PX) / 2;
        };
    }

    /** 出入口の左端（−X 側）の X [px]。かごの中心が 0。 */
    public int entranceMinXPx() {
        int half = model.interiorWidthPx() / 2;
        return switch (door) {
            case CENTER, FOUR_CENTER -> -entranceWidthPx() / 2;
            case SIDE_LEFT -> -half + panelWidthPx();
            case SIDE_RIGHT -> half - panelWidthPx() - entranceWidthPx();
        };
    }

    /** 出入口の右端（+X 側）の X [px]。 */
    public int entranceMaxXPx() {
        return entranceMinXPx() + entranceWidthPx();
    }
}
