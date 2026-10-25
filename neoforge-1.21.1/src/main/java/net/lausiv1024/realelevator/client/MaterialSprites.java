package net.lausiv1024.realelevator.client;

import java.util.List;

import net.lausiv1024.realelevator.design.Materials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 内装の素材（ブロック）の見た目。ブロックのモデルの側面（北向きの面）のテクスチャを使うので、リソースパックにも追従する。
 */
final class MaterialSprites {
    private static final RandomSource RANDOM = RandomSource.create(42);

    private MaterialSprites() {
    }

    /** 素材 id のブロックの側面のテクスチャ。見つからなければ鉄ブロック。 */
    static TextureAtlasSprite side(String id) {
        BlockState state = Materials.block(id).defaultBlockState();
        if (state.isAir()) {
            state = Blocks.IRON_BLOCK.defaultBlockState();
        }
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = model.getQuads(state, Direction.NORTH, RANDOM);
        return quads.isEmpty() ? model.getParticleIcon() : quads.get(0).getSprite();
    }
}
