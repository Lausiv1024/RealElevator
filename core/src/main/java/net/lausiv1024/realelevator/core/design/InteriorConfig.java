package net.lausiv1024.realelevator.core.design;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * かご内装と乗場の意匠の設定。素材はブロックのID（"minecraft:dark_oak_planks" など）で持ち、そのブロックの側面の見た目を使う。
 * 選べる素材はブロックタグ（{@link MaterialTag}）で決まる。
 *
 * @param wall            側壁と奥壁
 * @param front           前壁（正面の袖壁）
 * @param transom         トランサム（出入口の上）。{@link #SAME_AS_FRONT} なら前壁と、{@link #SAME_AS_DOOR} ならドアと同じ
 * @param door            かごの戸（乗場の戸も、{@link Landing#doorMaterial} が {@link #SAME_AS_DOOR} ならこれ）
 * @param floor           床
 * @param baseboard       幅木（側壁と奥壁の下端の帯）
 * @param carpet          床に重ねるカーペット
 * @param ceiling         天井の種類
 * @param handrail        手すりの位置
 * @param securityWindow  かごの戸の防犯窓（乗場の戸の窓は {@link Landing#securityWindow}）
 * @param landing         乗場の意匠（共通）
 * @param landingVariants 一部の階だけ変える乗場の意匠。どの意匠にも入っていない階は共通の意匠
 */
public record InteriorConfig(String wall, String front, String transom, String door, String floor, String baseboard, Carpet carpet,
                             CeilingType ceiling, HandrailPlacement handrail, SecurityWindow securityWindow, Landing landing,
                             List<LandingVariant> landingVariants) {
    /** 乗場の壁の素材を、まわりの壁に合わせる（カメレオン）ときの値。 */
    public static final String AUTO = "";
    /** トランサムを前壁と同じ素材にするときの値。 */
    public static final String SAME_AS_FRONT = "#front";
    /** トランサムをドアと同じ素材にするとき、乗場の戸をかごの戸と同じ素材にするときの値。 */
    public static final String SAME_AS_DOOR = "#door";
    /** 階ごとの乗場の意匠の数の上限（停止階の数の上限と同じ）。 */
    public static final int MAX_LANDING_VARIANTS = 18;

    /** 標準のプリセットと同じ内装。 */
    public static final InteriorConfig DEFAULT = new InteriorConfig("realelevator:woodgrain_medium", "realelevator:stainless_hairline",
            SAME_AS_FRONT, "realelevator:bronze_hairline", "realelevator:vinyl_gray", "realelevator:stainless_hairline", Carpet.NONE,
            CeilingType.DOWNLIGHT, HandrailPlacement.SIDES, SecurityWindow.NONE, Landing.DEFAULT);

    public InteriorConfig {
        landingVariants = List.copyOf(landingVariants);
    }

    /** 階ごとの乗場の意匠のない設定。 */
    public InteriorConfig(String wall, String front, String transom, String door, String floor, String baseboard, Carpet carpet,
                          CeilingType ceiling, HandrailPlacement handrail, SecurityWindow securityWindow, Landing landing) {
        this(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, securityWindow, landing, List.of());
    }

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
     * @param frame          枠の形
     * @param panel          操作盤の付け方
     * @param frameMaterial  枠（三方枠）の素材
     * @param wallMaterial   枠のまわりの壁の素材。{@link #AUTO} なら、まわりの壁のブロックに合わせる
     * @param lanternPlate   ホールランタンにステンレスの背面プレートを付けるか
     * @param doorMaterial   乗場の戸の素材。{@link #SAME_AS_DOOR} なら、かごの戸と同じ
     * @param securityWindow 乗場の戸の防犯窓。既定はかごに合わせる
     */
    public record Landing(FrameStyle frame, PanelStyle panel, String frameMaterial, String wallMaterial, boolean lanternPlate,
                          String doorMaterial, LandingWindow securityWindow) {
        public static final Landing DEFAULT = new Landing(FrameStyle.THIN, PanelStyle.WALL_MOUNTED, "realelevator:bronze_hairline", AUTO, true,
                SAME_AS_DOOR, LandingWindow.SAME_AS_CAR);

        public Landing withFrame(FrameStyle value) {
            return new Landing(value, panel, frameMaterial, wallMaterial, lanternPlate, doorMaterial, securityWindow);
        }

        public Landing withPanel(PanelStyle value) {
            return new Landing(frame, value, frameMaterial, wallMaterial, lanternPlate, doorMaterial, securityWindow);
        }

        public Landing withLanternPlate(boolean value) {
            return new Landing(frame, panel, frameMaterial, wallMaterial, value, doorMaterial, securityWindow);
        }

        public Landing withSecurityWindow(LandingWindow value) {
            return new Landing(frame, panel, frameMaterial, wallMaterial, lanternPlate, doorMaterial, value);
        }

        /** 乗場の部位（{@link Part#isLanding}）の素材。 */
        public String material(Part part) {
            return switch (part) {
                case LANDING_FRAME -> frameMaterial;
                case LANDING_WALL -> wallMaterial;
                case LANDING_DOOR -> doorMaterial;
                default -> throw new IllegalArgumentException("not a landing part: " + part);
            };
        }

        /** 乗場の部位 part の素材を変える。 */
        public Landing withMaterial(Part part, String value) {
            if (!part.isLanding()) {
                throw new IllegalArgumentException("not a landing part: " + part);
            }
            return new Landing(frame, panel, part == Part.LANDING_FRAME ? value : frameMaterial, part == Part.LANDING_WALL ? value : wallMaterial,
                    lanternPlate, part == Part.LANDING_DOOR ? value : doorMaterial, securityWindow);
        }
    }

    /**
     * 一部の階だけ変える乗場の意匠。
     *
     * @param floors  この意匠にする階（0始まりの停止階の番号。小さい順に並べ、重なりを除く）
     * @param landing 乗場の意匠
     */
    public record LandingVariant(List<Integer> floors, Landing landing) {
        public LandingVariant {
            floors = List.copyOf(new TreeSet<>(floors));
        }

        public LandingVariant withFloors(List<Integer> value) {
            return new LandingVariant(value, landing);
        }

        public LandingVariant withLanding(Landing value) {
            return new LandingVariant(floors, value);
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
        /** 乗場の枠のまわりの壁。{@link #AUTO} も選べる。ガラスなどの透明なブロックも選べる。 */
        LANDING_WALL(MaterialTag.LANDING_WALL),
        /** 乗場の戸。{@link #SAME_AS_DOOR}（かごの戸と同じ）も選べる。 */
        LANDING_DOOR(MaterialTag.WALL);

        private final MaterialTag tag;

        Part(MaterialTag tag) {
            this.tag = tag;
        }

        /** 選べる素材のタグ。 */
        public MaterialTag tag() {
            return tag;
        }

        /** 乗場の意匠（{@link Landing}）の部位か。階ごとの意匠で変えられる。 */
        public boolean isLanding() {
            return this == LANDING_FRAME || this == LANDING_WALL || this == LANDING_DOOR;
        }
    }

    /** 素材の候補のブロックタグ。 */
    public enum MaterialTag {
        WALL("realelevator:materials/wall"),
        FLOOR("realelevator:materials/floor"),
        /** 乗場の壁。壁の候補に、ガラスなどの透明なブロックを足したもの。 */
        LANDING_WALL("realelevator:materials/landing_wall");

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

    /** 乗場の戸の防犯窓。かごに合わせる（既定）か、かごとは別に選ぶ。 */
    public enum LandingWindow {
        SAME_AS_CAR(null),
        NONE(SecurityWindow.NONE),
        STANDARD(SecurityWindow.STANDARD),
        LARGE(SecurityWindow.LARGE),
        EXTRA_LARGE(SecurityWindow.EXTRA_LARGE);

        private final SecurityWindow window;

        LandingWindow(SecurityWindow window) {
            this.window = window;
        }

        /** かごの防犯窓が car のときの、乗場の戸の防犯窓。 */
        public SecurityWindow resolve(SecurityWindow car) {
            return window != null ? window : car;
        }
    }

    public enum HandrailPlacement {
        NONE,
        /** 両側の側壁。 */
        SIDES,
        /** 側壁と奥壁（二方向出入口では奥には付けない）。 */
        SIDES_AND_BACK
    }

    /** 部位 part の素材。乗場の部位は共通の乗場の意匠のもの。 */
    public String material(Part part) {
        return switch (part) {
            case WALL -> wall;
            case FRONT -> front;
            case TRANSOM -> transom;
            case DOOR -> door;
            case FLOOR -> floor;
            case BASEBOARD -> baseboard;
            case LANDING_FRAME, LANDING_WALL, LANDING_DOOR -> landing.material(part);
        };
    }

    /** 部位 part の素材を変える。乗場の部位は共通の乗場の意匠を変える。 */
    public InteriorConfig withMaterial(Part part, String value) {
        Landing newLanding = part.isLanding() ? landing.withMaterial(part, value) : landing;
        return new InteriorConfig(part == Part.WALL ? value : wall, part == Part.FRONT ? value : front, part == Part.TRANSOM ? value : transom,
                part == Part.DOOR ? value : door, part == Part.FLOOR ? value : floor, part == Part.BASEBOARD ? value : baseboard, carpet,
                ceiling, handrail, securityWindow, newLanding, landingVariants);
    }

    public InteriorConfig withCeiling(CeilingType value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, value, handrail, securityWindow, landing, landingVariants);
    }

    public InteriorConfig withHandrail(HandrailPlacement value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, value, securityWindow, landing, landingVariants);
    }

    /** 共通の乗場の意匠を変える。 */
    public InteriorConfig withLanding(Landing value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, securityWindow, value, landingVariants);
    }

    public InteriorConfig withLandingVariants(List<LandingVariant> value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, securityWindow, landing, value);
    }

    public InteriorConfig withCarpet(Carpet value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, value, ceiling, handrail, securityWindow, landing, landingVariants);
    }

    /** かごの戸の防犯窓を変える。 */
    public InteriorConfig withSecurityWindow(SecurityWindow value) {
        return new InteriorConfig(wall, front, transom, door, floor, baseboard, carpet, ceiling, handrail, value, landing, landingVariants);
    }

    /** トランサムの実際の素材（前壁と同じ・ドアと同じを解決したもの）。 */
    public String transomMaterial() {
        return switch (transom) {
            case SAME_AS_FRONT -> front;
            case SAME_AS_DOOR -> door;
            default -> transom;
        };
    }

    /** 停止階 floor（0始まり）の乗場の意匠。その階を含む階ごとの意匠があればそれ、なければ共通の意匠。 */
    public Landing landingAt(int floor) {
        for (LandingVariant variant : landingVariants) {
            if (variant.floors().contains(floor)) {
                return variant.landing();
            }
        }
        return landing;
    }

    /** 乗場の意匠 landing の戸の実際の防犯窓（かごに合わせるを解決したもの）。 */
    public SecurityWindow landingWindow(Landing landing) {
        return landing.securityWindow().resolve(securityWindow);
    }

    /** 乗場の意匠 landing の戸の実際の素材（かごの戸と同じを解決したもの）。 */
    public String landingDoorMaterial(Landing landing) {
        return SAME_AS_DOOR.equals(landing.doorMaterial()) ? door : landing.doorMaterial();
    }

    /**
     * 停止階 floor を、階ごとの意匠 variant（番号）に入れる。ほかの意匠に入っていれば、そこから外す。
     * variant が −1 なら、どの意匠からも外して共通の意匠に戻す。
     */
    public InteriorConfig assignLandingFloor(int floor, int variant) {
        List<LandingVariant> result = new ArrayList<>();
        for (int i = 0; i < landingVariants.size(); i++) {
            LandingVariant v = landingVariants.get(i);
            List<Integer> floors = new ArrayList<>(v.floors());
            floors.remove(Integer.valueOf(floor));
            if (i == variant) {
                floors.add(floor);
            }
            result.add(v.withFloors(floors));
        }
        return withLandingVariants(result);
    }

    /**
     * 階ごとの意匠の階を、停止階を oldFloors から newFloors（どちらも高さ、低い順）に変えたあとの番号に直す。
     * なくなった階は外す（階が1つもなくなった意匠も、あとで階を選び直せるように残す）。
     */
    public InteriorConfig remapLandingFloors(int[] oldFloors, int[] newFloors) {
        if (landingVariants.isEmpty()) {
            return this;
        }
        List<LandingVariant> result = new ArrayList<>();
        for (LandingVariant variant : landingVariants) {
            List<Integer> floors = new ArrayList<>();
            for (int index : variant.floors()) {
                if (index < 0 || index >= oldFloors.length) {
                    continue;
                }
                for (int i = 0; i < newFloors.length; i++) {
                    if (newFloors[i] == oldFloors[index]) {
                        floors.add(i);
                    }
                }
            }
            result.add(variant.withFloors(floors));
        }
        return withLandingVariants(result);
    }
}
