package net.lausiv1024.realelevator.core.design;

import java.util.Locale;

/**
 * かご内装と乗場の意匠の設定。素材はブロックのID（"minecraft:dark_oak_planks" など）で持ち、そのブロックの側面の見た目を使う。
 * 選べる素材はブロックタグ（床のほかは {@code realelevator:materials/wall}、床は {@code realelevator:materials/floor}）で決まる。
 *
 * @param wall           側壁と奥壁
 * @param front          前壁（正面の袖壁）
 * @param transom        トランサム（出入口の上）。{@link #SAME_AS_FRONT} なら前壁と、{@link #SAME_AS_DOOR} ならドアと同じ
 * @param door           かごと乗場の戸
 * @param floor          床
 * @param baseboard      幅木（側壁と奥壁の下端の帯）
 * @param carpet         床に重ねるカーペット
 * @param ceiling        天井の種類
 * @param handrail       手すりの位置
 * @param securityWindow かごと乗場の戸の防犯窓
 * @param landing        乗場の意匠
 */
public record InteriorConfig(String wall, String front, String transom, String door, String floor, String baseboard, Carpet carpet,
                             CeilingType ceiling, HandrailPlacement handrail, SecurityWindow securityWindow, Landing landing) {
    /** 乗場の壁の素材を、まわりの壁に合わせる（カメレオン）ときの値。 */
    public static final String AUTO = "";
    /** トランサムを前壁と同じ素材にするときの値。 */
    public static final String SAME_AS_FRONT = "#front";
    /** トランサムをドアと同じ素材にするときの値。 */
    public static final String SAME_AS_DOOR = "#door";

    /** 標準のプリセットと同じ内装。 */
    public static final InteriorConfig DEFAULT = new InteriorConfig("realelevator:woodgrain_medium", "realelevator:stainless_hairline",
            SAME_AS_FRONT, "realelevator:bronze_hairline", "realelevator:vinyl_gray", "realelevator:stainless_hairline", Carpet.NONE,
            CeilingType.DOWNLIGHT, HandrailPlacement.SIDES, SecurityWindow.NONE, Landing.DEFAULT);

    /** 床に重ねるカーペット。バニラのカーペットの16色（{@link #blockId}）。 */
    public enum Carpet {
        NONE, WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK;

        /** そのカーペットのブロックのID。なしなら null。 */
        public String blockId() {
            return this == NONE ? null : "minecraft:" + name().toLowerCase(Locale.ROOT) + "_carpet";
        }
    }

    /**
     * 乗場の意匠。
     *
     * @param frame         枠の形
     * @param panel         操作盤の付け方
     * @param frameMaterial 枠（三方枠）の素材
     * @param wallMaterial  枠のまわりの壁の素材。{@link #AUTO} なら、まわりの壁のブロックに合わせる
     * @param lanternPlate  ホールランタンにステンレスの背面プレートを付けるか
     */
    public record Landing(FrameStyle frame, PanelStyle panel, String frameMaterial, String wallMaterial, boolean lanternPlate) {
        public static final Landing DEFAULT = new Landing(FrameStyle.THIN, PanelStyle.WALL_MOUNTED, "realelevator:bronze_hairline", AUTO, true);

        public Landing withFrame(FrameStyle value) {
            return new Landing(value, panel, frameMaterial, wallMaterial, lanternPlate);
        }

        public Landing withPanel(PanelStyle value) {
            return new Landing(frame, value, frameMaterial, wallMaterial, lanternPlate);
        }

        public Landing withLanternPlate(boolean value) {
            return new Landing(frame, panel, frameMaterial, wallMaterial, value);
        }
    }

    public enum FrameStyle {
        /** 出入口のまわりの細い枠。 */
        THIN,
        /** 細枠の上の部分を、三方枠の範囲の上端まで伸ばした幕板。 */
        FASCIA
    }

    public enum PanelStyle {
        /** 枠のすぐ右の壁に、縦長のステンレス板を貼る。 */
        WALL_MOUNTED,
        /** 右の枠を幅広にして埋め込む。 */
        EMBEDDED
    }

    /** 素材を選ぶ部位。 */
    public enum Part {
        WALL(MaterialTag.WALL),
        FRONT(MaterialTag.WALL),
        /** トランサム。{@link #SAME_AS_FRONT}・{@link #SAME_AS_DOOR} も選べる。 */
        TRANSOM(MaterialTag.WALL),
        DOOR(MaterialTag.WALL),
        FLOOR(MaterialTag.FLOOR),
        BASEBOARD(MaterialTag.WALL),
        /** 乗場の枠（三方枠）。 */
        LANDING_FRAME(MaterialTag.WALL),
        /** 乗場の枠のまわりの壁。{@link #AUTO} も選べる。 */
        LANDING_WALL(MaterialTag.WALL);

        private final MaterialTag tag;

        Part(MaterialTag tag) {
            this.tag = tag;
        }

        /** 選べる素材のタグ。 */
        public MaterialTag tag() {
            return tag;
        }
    }

    /** 素材の候補のブロックタグ。 */
    public enum MaterialTag {
        WALL("realelevator:materials/wall"),
        FLOOR("realelevator:materials/floor");

        private final String id;

        MaterialTag(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum CeilingType {
        /** 明るい灰色の平らな天井に、四角い照明を4つ。 */
        DOWNLIGHT,
        /** まわりを一段下げた縁取りと、中央の光る板。 */
        INDIRECT,
        /** 天井全体が光る白い板。 */
        FULL
    }

    /**
     * 戸板の防犯窓（縦長、角は四角、網入りガラス）。大きさは戸板の幅と戸の高さに対する割合で、戸の上から約1/6の位置から下へ伸ばす。
     */
    public enum SecurityWindow {
        NONE(0, 0),
        /** 細め（戸板の幅の約1/3）で、戸の高さの約1/3の長さ。 */
        STANDARD(1.0 / 3, 1.0 / 3),
        /** 広め（戸板の幅の約2/3）で、戸の高さの約3/5の長さ。 */
        LARGE(2.0 / 3, 3.0 / 5),
        /** 大型と同じ幅で、戸の高さの約4/5の長さ。 */
        EXTRA_LARGE(2.0 / 3, 4.0 / 5);

        private final double widthRatio;
        private final double heightRatio;

        SecurityWindow(double widthRatio, double heightRatio) {
            this.widthRatio = widthRatio;
            this.heightRatio = heightRatio;
        }

        /** 戸板の幅に対する窓の幅の割合。 */
        public double widthRatio() {
            return widthRatio;
        }

        /** 戸の高さに対する窓の高さの割合。 */
        public double heightRatio() {
            return heightRatio;
        }
    }

    public enum HandrailPlacement {
        NONE,
        /** 両側の側壁。 */
        SIDES,
        /** 側壁と奥壁（二方向出入口では奥には付けない）。 */
        SIDES_AND_BACK
    }

    public String material(Part part) {
        return switch (part) {
            case WALL -> wall;
            case FRONT -> front;
            case TRANSOM -> transom;
            case DOOR -> door;
            case FLOOR -> floor;
            case BASEBOARD -> baseboard;
            case LANDING_FRAME -> landing.frameMaterial();
            case LANDING_WALL -> landing.wallMaterial();
        };
    }

    public InteriorConfig withMaterial(Part part, String value) {
        Landing newLanding = new Landing(landing.frame(), landing.panel(), part == Part.LANDING_FRAME ? value : landing.frameMaterial(),
                part == Part.LANDING_WALL ? value : landing.wallMaterial(), landing.lanternPlate());
        return new InteriorConfig(part == Part.WALL ? value : wall, part == Part.FRONT ? value : front, part == Part.TRANSOM ? value : transom,
                part == Part.DOOR ? value : door, part == Part.FLOOR ? value : floor, part == Part.BASEBOARD ? value : baseboard, carpet,
                ceiling, handrail, securityWindow, newLanding);
    }

    public InteriorConfig withCeiling(CeilingType value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, value, handrail, securityWindow, landing);
    }

    public InteriorConfig withHandrail(HandrailPlacement value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, value, securityWindow, landing);
    }

    public InteriorConfig withLanding(Landing value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, securityWindow, value);
    }

    public InteriorConfig withCarpet(Carpet value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, value, ceiling, handrail, securityWindow, landing);
    }

    /** トランサムの実際の素材（前壁と同じ・ドアと同じを解決したもの）。 */
    public String transomMaterial() {
        return switch (transom) {
            case SAME_AS_FRONT -> front;
            case SAME_AS_DOOR -> door;
            default -> transom;
        };
    }

    public InteriorConfig withSecurityWindow(SecurityWindow value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, value, landing);
    }
}
