package net.lausiv1024.realelevator.design;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.lausiv1024.realelevator.RealElevatorConfig;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 内装の素材（ブロック）の候補。候補はブロックタグで決まり、データパックから足せる。
 * サーバーのコンフィグで自由設定をオンにすると、任意の不透明な立方体のブロックも使える（{@link #isFreeMaterial}）。
 * 乗場の壁だけは、ガラスなどの透明な立方体のブロックも使える。
 */
public final class Materials {
    private Materials() {
    }

    public static TagKey<Block> tag(InteriorConfig.MaterialTag tag) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag.id()));
    }

    /** 部位 part に選べる素材のID（タグの並び順）。 */
    public static List<String> candidates(InteriorConfig.Part part) {
        List<String> ids = new ArrayList<>();
        BuiltInRegistries.BLOCK.getTag(tag(part.tag())).ifPresent(set -> {
            for (Holder<Block> holder : set) {
                holder.unwrapKey().ifPresent(key -> ids.add(key.location().toString()));
            }
        });
        return ids;
    }

    /** id の素材ブロック。見つからなければ空気。 */
    public static Block block(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location != null ? BuiltInRegistries.BLOCK.get(location) : Blocks.AIR;
    }

    /** block のID。 */
    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** 自由設定で素材にできるブロックか（不透明な立方体で、ブロックエンティティを持たず、ふつうのモデルで描くもの）。 */
    public static boolean isFreeMaterial(Block block) {
        return isCube(block) && block.defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** 自由設定で部位 part の素材にできるブロックか。乗場の壁は、ガラスなどの透明な立方体も使える。 */
    public static boolean isFreeMaterial(InteriorConfig.Part part, Block block) {
        return part == InteriorConfig.Part.LANDING_WALL ? isCube(block) : isFreeMaterial(block);
    }

    /**
     * 立方体のブロックか（当たり判定が1ブロックいっぱいで、ブロックエンティティを持たず、ふつうのモデルで描くもの）。
     * ガラスのような透明なものも含む。
     */
    public static boolean isCube(Block block) {
        BlockState state = block.defaultBlockState();
        return !state.isAir() && !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL
                && Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /** id のブロックが、向こうが透けて見えるもの（ガラスなど）か。 */
    public static boolean isSeeThrough(String id) {
        Block block = block(id);
        return block != Blocks.AIR && !block.defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** 部位 part の素材にできるか。タグの候補か、自由設定がオンなら不透明な立方体のブロック。 */
    public static boolean isAllowed(InteriorConfig.Part part, String id, boolean freeMaterials) {
        if (part == InteriorConfig.Part.LANDING_WALL && InteriorConfig.AUTO.equals(id)) {
            return true;
        }
        if (part == InteriorConfig.Part.TRANSOM && (InteriorConfig.SAME_AS_FRONT.equals(id) || InteriorConfig.SAME_AS_DOOR.equals(id))) {
            return true;
        }
        if (part == InteriorConfig.Part.LANDING_DOOR && InteriorConfig.SAME_AS_DOOR.equals(id)) {
            return true;
        }
        Block block = block(id);
        if (block == Blocks.AIR) {
            return false;
        }
        return block.defaultBlockState().is(tag(part.tag())) || freeMaterials && isFreeMaterial(part, block);
    }

    /** 候補から外した素材と、その代わりの素材。 */
    private static final Map<String, String> REPLACED = Map.of("minecraft:black_concrete", "realelevator:black_hairline");

    /**
     * 部位ごとの素材のうち、選べないもの（不正な設定や、データパックから外された素材）を初期値に戻す。階ごとの乗場の意匠も同じ。
     * 自由設定はサーバーのコンフィグに従う。階ごとの意匠は {@link InteriorConfig#MAX_LANDING_VARIANTS} 個までにする。
     */
    public static InteriorConfig sanitize(InteriorConfig config) {
        boolean free = RealElevatorConfig.freeMaterials();
        InteriorConfig result = config;
        for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
            String id = config.material(part);
            if (!isAllowed(part, id, free)) {
                result = result.withMaterial(part, replacement(part, id, free));
            }
        }
        List<InteriorConfig.LandingVariant> variants = new ArrayList<>();
        for (InteriorConfig.LandingVariant variant : result.landingVariants()) {
            if (variants.size() >= InteriorConfig.MAX_LANDING_VARIANTS) {
                break;
            }
            InteriorConfig.Landing landing = variant.landing();
            for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
                if (part.isLanding() && !isAllowed(part, landing.material(part), free)) {
                    landing = landing.withMaterial(part, replacement(part, landing.material(part), free));
                }
            }
            variants.add(variant.withLanding(landing));
        }
        return result.withLandingVariants(variants);
    }

    /** 選べない素材 id の代わり。候補から外した黒コンクリートは、入れ替えた黒のヘアラインにする。ほかは初期値。 */
    private static String replacement(InteriorConfig.Part part, String id, boolean free) {
        String replacement = REPLACED.getOrDefault(id, InteriorConfig.DEFAULT.material(part));
        return isAllowed(part, replacement, free) ? replacement : InteriorConfig.DEFAULT.material(part);
    }
}
