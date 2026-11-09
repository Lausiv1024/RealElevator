package net.lausiv1024.realelevator.core.panel;

/**
 * 操作盤・階床表示器・ホールランタンの配置の設定。
 *
 * @param panelSides         かごの操作盤を置く袖壁
 * @param leftPanelIndicator 操作盤が両側のとき、左側（かごの中から扉の方を向いて）の操作盤の上にも表示器を付けるか
 * @param hallIndicator      乗場の階床表示器の位置
 * @param hallLantern        ホールランタンの位置
 * @param wheelchairPanel    車椅子用操作盤（かごの両側の側壁）と乗場の車椅子用ボタンを付けるか
 * @param indicatorColor     階床表示器・ホールランタン・ボタンの応答灯の色
 * @param groupControl       群管理向け。乗場の階床表示器を付けず、ホールランタンを扉の右にも置けるようにする
 * @param openExtension      かごの主操作盤に開延長ボタンを付けるか
 * @param indicatorStyle     階床表示器と行先ボタンの階名の描き方
 */
public record FixtureConfig(PanelSides panelSides, boolean leftPanelIndicator, HallIndicator hallIndicator,
                            HallLanternPlacement hallLantern, boolean wheelchairPanel, IndicatorColor indicatorColor,
                            boolean groupControl, boolean openExtension,
                            IndicatorStyle indicatorStyle) {
    /**
     * 日本の標準仕様に合わせた初期値。かごは右側の操作盤の上に表示器、乗場はボタンの上（三方枠の横）に表示器。
     * ホールランタンは有償の付加仕様なので付けない。車椅子用操作盤と開延長ボタンも付けない。
     */
    public static final FixtureConfig DEFAULT = new FixtureConfig(PanelSides.RIGHT, false,
            HallIndicator.OVER_BUTTONS, HallLanternPlacement.NONE, false, IndicatorColor.AMBER, false, false, IndicatorStyle.DOT);

    public FixtureConfig(PanelSides panelSides, boolean leftPanelIndicator, HallIndicator hallIndicator,
                         HallLanternPlacement hallLantern) {
        this(panelSides, leftPanelIndicator, hallIndicator, hallLantern, false, IndicatorColor.AMBER, false, false, IndicatorStyle.DOT);
    }

    /** 階床表示器と行先ボタンの階名の描き方。 */
    public enum IndicatorStyle {
        /** ドットの文字（英数字の一部だけ、3文字まで）。 */
        DOT,
        /** ゲームのフォント（漢字なども出せ、表示器に収まるよう縮める）。 */
        FONT
    }

    public FixtureConfig withIndicatorStyle(IndicatorStyle value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, hallLantern, wheelchairPanel, indicatorColor, groupControl,
                openExtension, value);
    }

    /** 表示器などの光る色。 */
    public enum IndicatorColor {
        AMBER(0xFFAA00),
        WHITE(0xF0F4F8),
        BLUE(0x70C8FF);

        private final int rgb;

        IndicatorColor(int rgb) {
            this.rgb = rgb;
        }

        /** 0xRRGGBB。 */
        public int rgb() {
            return rgb;
        }
    }

    public enum PanelSides {
        /** かごの中から扉の方を向いて右側の袖壁だけ。 */
        RIGHT,
        /** 左右の袖壁の両方。 */
        BOTH
    }

    public enum HallIndicator {
        /** 乗場の操作盤の上部（ボタンの上）。 */
        OVER_BUTTONS,
        /** 出入口の上の中央。 */
        OVER_DOOR
    }

    public enum HallLanternPlacement {
        /** 付けない。 */
        NONE,
        /** 出入口の横（乗場から見てボタンと反対側）に縦長の棒。 */
        BESIDE_DOOR,
        /** 出入口の上に横長の棒。 */
        OVER_DOOR,
        /** 出入口の右（操作盤の上）に縦長の棒。群管理向けのときだけ選べる。 */
        RIGHT_OF_DOOR
    }

    /** 乗場の階床表示器を付けるか（群管理向けでは付けない）。 */
    public boolean hasHallIndicator() {
        return !groupControl;
    }

    /** 乗場の器具のどれかを出入口の上に付けるか。 */
    public boolean usesOverDoorHallFixtures() {
        return hasHallIndicator() && hallIndicator == HallIndicator.OVER_DOOR || hallLantern == HallLanternPlacement.OVER_DOOR;
    }

    /** 出入口の上に付ける乗場の器具を、出入口の横へ移したもの。表示器はボタンの上、ランタンは扉の横にする。 */
    public FixtureConfig withoutOverDoorHallFixtures() {
        return new FixtureConfig(panelSides, leftPanelIndicator,
                hallIndicator == HallIndicator.OVER_DOOR ? HallIndicator.OVER_BUTTONS : hallIndicator,
                hallLantern == HallLanternPlacement.OVER_DOOR ? HallLanternPlacement.BESIDE_DOOR : hallLantern, wheelchairPanel, indicatorColor,
                groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withPanelSides(PanelSides value) {
        return new FixtureConfig(value, leftPanelIndicator, hallIndicator, hallLantern, wheelchairPanel, indicatorColor, groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withLeftPanelIndicator(boolean value) {
        return new FixtureConfig(panelSides, value, hallIndicator, hallLantern, wheelchairPanel, indicatorColor, groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withHallIndicator(HallIndicator value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, value, hallLantern, wheelchairPanel, indicatorColor, groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withHallLantern(HallLanternPlacement value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, value, wheelchairPanel, indicatorColor, groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withWheelchairPanel(boolean value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, hallLantern, value, indicatorColor, groupControl, openExtension, indicatorStyle);
    }

    public FixtureConfig withGroupControl(boolean value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, hallLantern, wheelchairPanel, indicatorColor, value, openExtension, indicatorStyle);
    }

    public FixtureConfig withOpenExtension(boolean value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, hallLantern, wheelchairPanel, indicatorColor, groupControl, value, indicatorStyle);
    }

    public FixtureConfig withIndicatorColor(IndicatorColor value) {
        return new FixtureConfig(panelSides, leftPanelIndicator, hallIndicator, hallLantern, wheelchairPanel, value, groupControl, openExtension, indicatorStyle);
    }
}
