package net.lausiv1024.realelevator.design;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 内装の素材（ブロック）の候補。候補はブロックタグで決まり、データパックから足せる。
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

    /**
     * 部位ごとの素材のうち、その部位のタグに入っていないものを初期値に戻す（不正な設定や、データパックから外された素材）。
     * 乗場の壁の「まわりに合わせる」（{@link InteriorConfig#AUTO}）はそのまま。
     */
    public static InteriorConfig sanitize(InteriorConfig config) {
        InteriorConfig result = config;
        for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
            if (part == InteriorConfig.Part.LANDING_WALL && InteriorConfig.AUTO.equals(config.material(part))) {
                continue;
            }
            Block block = block(config.material(part));
            if (block == Blocks.AIR || !block.defaultBlockState().is(tag(part.tag()))) {
                result = result.withMaterial(part, InteriorConfig.DEFAULT.material(part));
            }
        }
        return result;
    }
}
