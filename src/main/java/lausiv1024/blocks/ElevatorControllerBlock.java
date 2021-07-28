package lausiv1024.blocks;

import lausiv1024.util.ModelRotationHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
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

public class ElevatorControllerBlock extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape NORTH = ModelRotationHelper.rotate(4, 0, 0, 12, 27, 3, Direction.NORTH);
    private static final VoxelShape SOUTH = ModelRotationHelper.rotate(4, 0, 0, 12, 27, 3, Direction.SOUTH);
    private static final VoxelShape EAST = ModelRotationHelper.rotate(4, 0, 0, 12, 27, 3, Direction.EAST);
    private static final VoxelShape WEST = ModelRotationHelper.rotate(4, 0, 0, 12, 27, 3, Direction.WEST);

    public ElevatorControllerBlock(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
        }
        return NORTH;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP){
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
