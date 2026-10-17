package net.lausiv1024.realelevator.block;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 昇降路の頂部の制御盤。縦2ブロックで、下のブロックがブロックエンティティ（{@link ControllerBlockEntity}）を持つ。
 * FACING は乗場の向きで、制御盤は昇降路の奥の壁に付いている。
 *
 * <p>サバイバルでは壊せない（撤去は保守用レンチの「解体」で行う）。クリエイティブやコマンドで消されたときは、
 * エレベーターごと解体する。
 */
public class ControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ControllerBlock> CODEC = simpleCodec(ControllerBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /** 奥の壁（FACING と反対の面）に付いた、幅12px・奥行き6pxの箱。 */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.SOUTH, Block.box(2, 0, 0, 14, 16, 6),
            Direction.NORTH, Block.box(2, 0, 10, 14, 16, 16),
            Direction.EAST, Block.box(0, 0, 2, 6, 16, 14),
            Direction.WEST, Block.box(10, 0, 2, 16, 16, 14));

    public ControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ControllerBlockEntity(pos, state) : null;
    }

    /** 下のブロックの位置。 */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(lowerPos(state, pos)) instanceof ControllerBlockEntity controller) {
            // 解体の途中で消したのでなければ、エレベーターごと解体する
            controller.dismantle(null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
