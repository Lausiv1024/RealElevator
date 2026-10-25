package net.lausiv1024.realelevator;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * サーバーのコンフィグ（ワールドごと、{@code serverconfig/realelevator-server.toml}）。
 */
public final class RealElevatorConfig {
    public static final ModConfigSpec SPEC;
    /** 自由設定：タグの候補のほかに、手に持った任意の不透明な立方体のブロックを素材にできる。 */
    public static final ModConfigSpec.BooleanValue FREE_MATERIALS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        FREE_MATERIALS = builder
                .comment("Allow any opaque full block held in hand as an elevator material, not only the blocks in the material tags.")
                .define("freeMaterials", false);
        SPEC = builder.build();
    }

    private RealElevatorConfig() {
    }

    /** 自由設定がオンか。コンフィグがまだ読み込まれていなければオフ。 */
    public static boolean freeMaterials() {
        return SPEC.isLoaded() && FREE_MATERIALS.get();
    }
}
