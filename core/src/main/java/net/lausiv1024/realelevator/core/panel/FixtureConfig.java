package net.lausiv1024.realelevator.core.panel;

/**
 * 操作盤と階床表示器の配置の設定。
 *
 * @param panelSides         かごの操作盤を置く袖壁
 * @param carIndicator       かごの階床表示器の位置
 * @param leftPanelIndicator 操作盤が両側のとき、左側（かごの中から扉の方を向いて）の操作盤の上にも表示器を付けるか。
 *                           かごの表示器が操作盤の上のときだけ効く
 * @param hallIndicator      乗場の階床表示器の位置
 */
public record FixtureConfig(PanelSides panelSides, CarIndicator carIndicator, boolean leftPanelIndicator, HallIndicator hallIndicator) {
    /** 右側の操作盤の上に表示器、乗場は扉の上。 */
    public static final FixtureConfig DEFAULT = new FixtureConfig(PanelSides.RIGHT, CarIndicator.OVER_PANEL, false, HallIndicator.OVER_DOOR);

    public enum PanelSides {
        /** かごの中から扉の方を向いて右側の袖壁だけ。 */
        RIGHT,
        /** 左右の袖壁の両方。 */
        BOTH
    }

    public enum CarIndicator {
        /** 出入口の上のトランサムに埋め込む。 */
        OVER_DOOR,
        /** 操作盤の行先ボタンの上。 */
        OVER_PANEL
    }

    public enum HallIndicator {
        /** 出入口の上の中央。 */
        OVER_DOOR,
        /** 乗場のボタンの上。 */
        OVER_BUTTONS
    }

    public FixtureConfig withPanelSides(PanelSides value) {
        return new FixtureConfig(value, carIndicator, leftPanelIndicator, hallIndicator);
    }

    public FixtureConfig withCarIndicator(CarIndicator value) {
        return new FixtureConfig(panelSides, value, leftPanelIndicator, hallIndicator);
    }

    public FixtureConfig withLeftPanelIndicator(boolean value) {
        return new FixtureConfig(panelSides, carIndicator, value, hallIndicator);
    }

    public FixtureConfig withHallIndicator(HallIndicator value) {
        return new FixtureConfig(panelSides, carIndicator, leftPanelIndicator, value);
    }
}
