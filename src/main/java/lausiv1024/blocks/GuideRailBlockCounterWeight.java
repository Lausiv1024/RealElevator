package lausiv1024.blocks;

import lausiv1024.util.RotatableBoxShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;

public class GuideRailBlockCounterWeight extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
    private static final RotatableBoxShape BASE1 = new RotatableBoxShape(3.4, 0, 8.5, 4, 16, 10);
    private static final RotatableBoxShape BASE2 = new RotatableBoxShape(10, 0, 8.5, 10.6, 16, 10);

    private static final RotatableBoxShape RAIL1 = new RotatableBoxShape(3.4, 0, 7.9, 6.4, 16, 8.5);
    private static final RotatableBoxShape RAIL2 = new RotatableBoxShape(7.6, 0, 7.9, 10.6, 16, 8.5);

    public GuideRailBlockCounterWeight(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP ?
                this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()) :
                this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING, PART);
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        Direction facing = p_220053_1_.getValue(FACING);
        VoxelShape v;
        if (p_220053_1_.getValue(PART) == 0) v = VoxelShapes.or(BASE1.rotateAndConvert(facing), RAIL1.rotateAndConvert(facing));
        else v = VoxelShapes.or(BASE2.rotateAndConvert(facing), RAIL2.rotateAndConvert(facing));
        return v;
    }
}
