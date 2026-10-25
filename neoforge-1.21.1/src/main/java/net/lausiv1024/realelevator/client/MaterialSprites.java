package net.lausiv1024.realelevator.client;

import java.util.List;

import javax.annotation.Nullable;

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
 * 内装の素材（ブロック）の見た目。ブロックのモデルの面のテクスチャを使うので、リソースパックにも追従する。
 */
final class MaterialSprites {
    private static final RandomSource RANDOM = RandomSource.create(42);

    private MaterialSprites() {
    }

    /** 素材 id のブロックの側面（北向きの面）のテクスチャ。見つからなければ鉄ブロック。 */
    static TextureAtlasSprite side(String id) {
        BlockState state = Materials.block(id).defaultBlockState();
        if (state.isAir()) {
            state = Blocks.IRON_BLOCK.defaultBlockState();
        }
        BakedQuad quad = quad(state, Direction.NORTH);
        return quad != null ? quad.getSprite() : model(state).getParticleIcon();
    }

    /** ブロックの状態 state の、direction 向きの面の最初の四角形。なければ null。 */
    @Nullable
    static BakedQuad quad(BlockState state, Direction direction) {
        List<BakedQuad> quads = model(state).getQuads(state, direction, RANDOM);
        return quads.isEmpty() ? null : quads.get(0);
    }

    private static BakedModel model(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }
}
