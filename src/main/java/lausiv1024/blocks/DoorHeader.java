package lausiv1024.blocks;

import lausiv1024.util.RotatableBoxShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;

public class DoorHeader extends ElevatorPartBlock{
    private static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty MIRROR = BooleanProperty.create("mirror");

    private static final RotatableBoxShape SHA = new RotatableBoxShape(-12, 0, 0, 16, 10, 2);
    private static final RotatableBoxShape MIR = new RotatableBoxShape(0, 0, 0, 28, 10, 2);

    public DoorHeader(){
        super();
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(MIRROR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, MIRROR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP ?
                defaultBlockState().setValue(FACING, context.getHorizontalDirection()) :
                defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState blockState, IBlockReader iBlockReader, BlockPos pos, ISelectionContext iSelectionContext) {
        RotatableBoxShape shape = blockState.getValue(MIRROR) ? MIR : SHA;
        return shape.rotateAndConvert(blockState.getValue(FACING));
    }
}
