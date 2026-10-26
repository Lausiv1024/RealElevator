package net.lausiv1024.realelevator.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ガイドレール。かごの左右に、昇降路の全高にわたって置く。FACING はかごの方向。
 *
 * <p>断面は T 形で、ブロックのかごと反対側の端に、壁側のフランジ（奥行き4px×厚み1px）と、かごに向かう刃（厚み1px×長さ3px）。
 * 刃の厚みの中心がブロックの中央に来るよう、刃は 7.5〜8.5px に置く。BRACKET のブロックには、壁への取り付け金具も付く。
 */
public class GuideRailBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GuideRailBlock> CODEC = simpleCodec(GuideRailBlock::new);
    /** 壁への取り付け金具を付けるか。設置のとき、何ブロックかおきに付ける。 */
    public static final BooleanProperty BRACKET = BooleanProperty.create("bracket");
    /** 取り付け金具を付ける間隔 [ブロック]。 */
    public static final int BRACKET_SPACING = 3;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // 北向き（かごが北、レールはブロックの南の端）の形を回して作る
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape flange = box(facing, 6, 0, 15, 10, 16, 16);
            VoxelShape blade = box(facing, 7.5, 0, 12, 8.5, 16, 15);
            SHAPES.put(facing, Shapes.or(flange, blade));
        }
    }

    public GuideRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BRACKET, false));
    }

    /** 北向きの座標 [px] の箱を、facing の向きに回したもの。 */
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BRACKET);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
