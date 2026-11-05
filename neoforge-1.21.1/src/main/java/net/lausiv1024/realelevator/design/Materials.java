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
        BlockState state = block.defaultBlockState();
        return !state.isAir() && !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL
                && state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** 部位 part の素材にできるか。タグの候補か、自由設定がオンなら不透明な立方体のブロック。 */
    public static boolean isAllowed(InteriorConfig.Part part, String id, boolean freeMaterials) {
        if (part == InteriorConfig.Part.LANDING_WALL && InteriorConfig.AUTO.equals(id)) {
            return true;
        }
        if (part == InteriorConfig.Part.TRANSOM && (InteriorConfig.SAME_AS_FRONT.equals(id) || InteriorConfig.SAME_AS_DOOR.equals(id))) {
            return true;
        }
        Block block = block(id);
        if (block == Blocks.AIR) {
            return false;
        }
        return block.defaultBlockState().is(tag(part.tag())) || freeMaterials && isFreeMaterial(block);
    }

    /**
     * 部位ごとの素材のうち、選べないもの（不正な設定や、データパックから外された素材）を初期値に戻す。
     * 自由設定はサーバーのコンフィグに従う。
     */
    /** 候補から外した素材と、その代わりの素材。 */
    private static final Map<String, String> REPLACED = Map.of("minecraft:black_concrete", "realelevator:black_hairline");

    public static InteriorConfig sanitize(InteriorConfig config) {
        boolean free = RealElevatorConfig.freeMaterials();
        InteriorConfig result = config;
        for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
            String id = config.material(part);
            if (!isAllowed(part, id, free)) {
                // 候補から外した黒コンクリートは、入れ替えた黒のヘアラインにする
                String replacement = REPLACED.getOrDefault(id, InteriorConfig.DEFAULT.material(part));
                result = result.withMaterial(part, isAllowed(part, replacement, free) ? replacement : InteriorConfig.DEFAULT.material(part));
            }
        }
        return result;
    }
}
