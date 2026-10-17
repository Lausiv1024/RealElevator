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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 昇降路の頂部の制御盤。縦2ブロックで、下のブロックがブロックエンティティ（{@link ControllerBlockEntity}）を持つ。
 * 制御盤は背面（FACING と反対の面）の壁に付く。後ろ落としでは奥の壁（FACING は乗場の向き）、横落としでは左の壁
 * （FACING は乗場から見て右の向き）で、奥寄りに付ける（CORNER）。
 *
 * <p>サバイバルでは壊せない（撤去は保守用レンチの「解体」で行う）。クリエイティブやコマンドで消されたときは、
 * エレベーターごと解体する。
 */
public class ControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ControllerBlock> CODEC = simpleCodec(ControllerBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** 背面の壁の端に寄せて付けるか（横落としで左の壁の奥寄りに付けるとき）。false なら中央。 */
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");

    /** 背面の壁（FACING と反対の面）に付いた、幅12px・厚み4pxの箱（{@code ShaftLayout#controllerBoxes} と同じ形）。 */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.SOUTH, Block.box(2, 0, 0, 14, 16, 4),
            Direction.NORTH, Block.box(2, 0, 12, 14, 16, 16),
            Direction.EAST, Block.box(0, 0, 2, 4, 16, 14),
            Direction.WEST, Block.box(12, 0, 2, 16, 16, 14));
    /** 端に寄せたもの。FACING から反時計回りに90°の向き（北向きなら西）へ2pxずらす。 */
    private static final Map<Direction, VoxelShape> CORNER_SHAPES = Map.of(
            Direction.SOUTH, Block.box(4, 0, 0, 16, 16, 4),
            Direction.NORTH, Block.box(0, 0, 12, 12, 16, 16),
            Direction.EAST, Block.box(0, 0, 0, 4, 16, 12),
            Direction.WEST, Block.box(12, 0, 4, 16, 16, 16));

    public ControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(CORNER, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, CORNER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(CORNER) ? CORNER_SHAPES : SHAPES).get(state.getValue(FACING));
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
