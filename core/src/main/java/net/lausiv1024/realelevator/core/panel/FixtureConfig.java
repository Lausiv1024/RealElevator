package net.lausiv1024.realelevator.core.panel;

/**
 * 操作盤・階床表示器・ホールランタンの配置の設定。
 *
 * @param panelSides         かごの操作盤を置く袖壁
 * @param carIndicator       かごの階床表示器の位置
 * @param leftPanelIndicator 操作盤が両側のとき、左側（かごの中から扉の方を向いて）の操作盤の上にも表示器を付けるか。
 *                           かごの表示器が操作盤の上のときだけ効く
 * @param hallIndicator      乗場の階床表示器の位置
 * @param hallLantern        ホールランタンの位置
 */
public record FixtureConfig(PanelSides panelSides, CarIndicator carIndicator, boolean leftPanelIndicator, HallIndicator hallIndicator,
                            HallLanternPlacement hallLantern) {
    /** 右側の操作盤の上に表示器、乗場は扉の上に表示器とホールランタン。 */
    public static final FixtureConfig DEFAULT = new FixtureConfig(PanelSides.RIGHT, CarIndicator.OVER_PANEL, false,
            HallIndicator.OVER_DOOR, HallLanternPlacement.OVER_DOOR);

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

    public enum HallLanternPlacement {
        /** 出入口の上に横長の棒。 */
        OVER_DOOR,
        /** 出入口の横（乗場から見てボタンと反対側）に縦長の棒。 */
        BESIDE_DOOR
    }

    public FixtureConfig withPanelSides(PanelSides value) {
        return new FixtureConfig(value, carIndicator, leftPanelIndicator, hallIndicator, hallLantern);
    }

    public FixtureConfig withCarIndicator(CarIndicator value) {
        return new FixtureConfig(panelSides, value, leftPanelIndicator, hallIndicator, hallLantern);
    }

    public FixtureConfig withLeftPanelIndicator(boolean value) {
        return new FixtureConfig(panelSides, carIndicator, value, hallIndicator, hallLantern);
    }

    public FixtureConfig withHallIndicator(HallIndicator value) {
        return new FixtureConfig(panelSides, carIndicator, leftPanelIndicator, value, hallLantern);
    }

    public FixtureConfig withHallLantern(HallLanternPlacement value) {
        return new FixtureConfig(panelSides, carIndicator, leftPanelIndicator, hallIndicator, value);
    }
}
