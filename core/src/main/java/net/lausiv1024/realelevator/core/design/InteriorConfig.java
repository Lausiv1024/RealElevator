package net.lausiv1024.realelevator.core.design;

/**
 * かご内装の設定。素材はブロックのID（"minecraft:dark_oak_planks" など）で持ち、そのブロックの側面の見た目を使う。
 * 選べる素材はブロックタグ（壁・前壁・ドア・幅木は {@code realelevator:materials/wall}、床は {@code realelevator:materials/floor}）で決まる。
 *
 * @param wall      側壁と奥壁
 * @param front     前壁（正面の袖壁とトランサム）
 * @param door      かごと乗場の戸
 * @param floor     床
 * @param baseboard 幅木（側壁と奥壁の下端の帯）
 * @param ceiling   天井の種類
 * @param handrail  手すりの位置
 */
public record InteriorConfig(String wall, String front, String door, String floor, String baseboard, CeilingType ceiling,
                             HandrailPlacement handrail) {
    /** 標準のプリセットと同じ内装。 */
    public static final InteriorConfig DEFAULT = new InteriorConfig("realelevator:woodgrain_medium", "realelevator:stainless_hairline",
            "realelevator:bronze_hairline", "realelevator:vinyl_gray", "realelevator:stainless_hairline", CeilingType.DOWNLIGHT,
            HandrailPlacement.SIDES);

    /** 素材を選ぶ部位。 */
    public enum Part {
        WALL(MaterialTag.WALL),
        FRONT(MaterialTag.WALL),
        DOOR(MaterialTag.WALL),
        FLOOR(MaterialTag.FLOOR),
        BASEBOARD(MaterialTag.WALL);

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
            case DOOR -> door;
            case FLOOR -> floor;
            case BASEBOARD -> baseboard;
        };
    }

    public InteriorConfig withMaterial(Part part, String value) {
        return new InteriorConfig(part == Part.WALL ? value : wall, part == Part.FRONT ? value : front, part == Part.DOOR ? value : door,
                part == Part.FLOOR ? value : floor, part == Part.BASEBOARD ? value : baseboard, ceiling, handrail);
    }

    public InteriorConfig withCeiling(CeilingType value) {
        return new InteriorConfig(wall, front, door, floor, baseboard, value, handrail);
    }

    public InteriorConfig withHandrail(HandrailPlacement value) {
        return new InteriorConfig(wall, front, door, floor, baseboard, ceiling, value);
    }
}
