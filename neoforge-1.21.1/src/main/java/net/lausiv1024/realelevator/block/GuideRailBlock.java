package net.lausiv1024.realelevator.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ガイドレール（仮）。かごの左右に、昇降路の全高にわたって置く細い柱。FACING はかごの方向で、
 * レールはブロックのかごと反対側の端（幅4px）にある。形は後で作り込む。
 */
public class GuideRailBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GuideRailBlock> CODEC = simpleCodec(GuideRailBlock::new);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(6, 0, 12, 10, 16, 16),
            Direction.SOUTH, Block.box(6, 0, 0, 10, 16, 4),
            Direction.EAST, Block.box(0, 0, 6, 4, 16, 10),
            Direction.WEST, Block.box(12, 0, 6, 16, 16, 10));

    public GuideRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
