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
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;

public class DoorRailDouble extends ElevatorPartBlock {
    private static final DirectionProperty FACING = HorizontalBlock.FACING;

    private static final VoxelShape MAIN = ModelRotationHelper.rotate(0, 13.8, 0, 16, 15.8, 2, Direction.NORTH);
    private static final VoxelShape MAIN2 = ModelRotationHelper.rotate(0, 13.8, 1.95, 16, 15.8, 3.95, Direction.NORTH);
    private static final VoxelShape MIZO0 = ModelRotationHelper.rotate(0, 15.8, 0, 16, 16, 0.4, Direction.NORTH);
    private static final VoxelShape MIZO1 = ModelRotationHelper.rotate(0, 15.8, 0.8, 16, 16, 1.2, Direction.NORTH);
    private static final VoxelShape MIZO2 = ModelRotationHelper.rotate(0, 15.8, 1.6, 16, 16, 2, Direction.NORTH);
    private static final VoxelShape MIZO3 = ModelRotationHelper.rotate(0, 15.8, 1.95, 16, 16, 2.35, Direction.NORTH);
    private static final VoxelShape MIZO4 = ModelRotationHelper.rotate(0, 15.8, 2.75, 16, 16, 3.15, Direction.NORTH);
    private static final VoxelShape MIZO5 = ModelRotationHelper.rotate(0, 15.8, 3.55, 16, 16, 3.95, Direction.NORTH);

    private static final VoxelShape SMAIN = ModelRotationHelper.rotate(0, 13.8, 0, 16, 15.8, 2, Direction.SOUTH);
    private static final VoxelShape SMAIN2 = ModelRotationHelper.rotate(0, 13.8, 1.95, 16, 15.8, 3.95, Direction.SOUTH);
    private static final VoxelShape SMIZO0 = ModelRotationHelper.rotate(0, 15.8, 0, 16, 16, 0.4, Direction.SOUTH);
    private static final VoxelShape SMIZO1 = ModelRotationHelper.rotate(0, 15.8, 0.8, 16, 16, 1.2, Direction.SOUTH);
    private static final VoxelShape SMIZO2 = ModelRotationHelper.rotate(0, 15.8, 1.6, 16, 16, 2, Direction.SOUTH);
    private static final VoxelShape SMIZO3 = ModelRotationHelper.rotate(0, 15.8, 1.95, 16, 16, 2.35, Direction.SOUTH);
    private static final VoxelShape SMIZO4 = ModelRotationHelper.rotate(0, 15.8, 2.75, 16, 16, 3.15, Direction.SOUTH);
    private static final VoxelShape SMIZO5 = ModelRotationHelper.rotate(0, 15.8, 3.55, 16, 16, 3.95, Direction.SOUTH);

    private static final VoxelShape EMAIN = ModelRotationHelper.rotate(0, 13.8, 0, 16, 15.8, 2, Direction.EAST);
    private static final VoxelShape EMAIN2 = ModelRotationHelper.rotate(0, 13.8, 1.95, 16, 15.8, 3.95, Direction.EAST);
    private static final VoxelShape EMIZO0 = ModelRotationHelper.rotate(0, 15.8, 0, 16, 16, 0.4, Direction.EAST);
    private static final VoxelShape EMIZO1 = ModelRotationHelper.rotate(0, 15.8, 0.8, 16, 16, 1.2, Direction.EAST);
    private static final VoxelShape EMIZO2 = ModelRotationHelper.rotate(0, 15.8, 1.6, 16, 16, 2, Direction.EAST);
    private static final VoxelShape EMIZO3 = ModelRotationHelper.rotate(0, 15.8, 1.95, 16, 16, 2.35, Direction.EAST);
    private static final VoxelShape EMIZO4 = ModelRotationHelper.rotate(0, 15.8, 2.75, 16, 16, 3.15, Direction.EAST);
    private static final VoxelShape EMIZO5 = ModelRotationHelper.rotate(0, 15.8, 3.55, 16, 16, 3.95, Direction.EAST);

    private static final VoxelShape WMAIN = ModelRotationHelper.rotate(0, 13.8, 0, 16, 15.8, 2, Direction.WEST);
    private static final VoxelShape WMAIN2 = ModelRotationHelper.rotate(0, 13.8, 1.95, 16, 15.8, 3.95, Direction.WEST);
    private static final VoxelShape WMIZO0 = ModelRotationHelper.rotate(0, 15.8, 0, 16, 16, 0.4, Direction.WEST);
    private static final VoxelShape WMIZO1 = ModelRotationHelper.rotate(0, 15.8, 0.8, 16, 16, 1.2, Direction.WEST);
    private static final VoxelShape WMIZO2 = ModelRotationHelper.rotate(0, 15.8, 1.6, 16, 16, 2, Direction.WEST);
    private static final VoxelShape WMIZO3 = ModelRotationHelper.rotate(0, 15.8, 1.95, 16, 16, 2.35, Direction.WEST);
    private static final VoxelShape WMIZO4 = ModelRotationHelper.rotate(0, 15.8, 2.75, 16, 16, 3.15, Direction.WEST);
    private static final VoxelShape WMIZO5 = ModelRotationHelper.rotate(0, 15.8, 3.55, 16, 16, 3.95, Direction.WEST);

    private static final VoxelShape NORTH = VoxelShapes.or(MAIN, MAIN2, MIZO0, MIZO1, MIZO2, MIZO3, MIZO4, MIZO5);
    private static final VoxelShape SOUTH = VoxelShapes.or(SMAIN, SMAIN2, SMIZO0, SMIZO1, SMIZO2, SMIZO3, SMIZO4, SMIZO5);
    private static final VoxelShape EAST = VoxelShapes.or(EMAIN, EMAIN2, EMIZO0, EMIZO1, EMIZO2, EMIZO3, EMIZO4, EMIZO5);
    private static final VoxelShape WEST = VoxelShapes.or(WMAIN, WMAIN2, WMIZO0, WMIZO1, WMIZO2, WMIZO3, WMIZO4, WMIZO5);

    public DoorRailDouble(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case EAST: return EAST;
            case SOUTH: return SOUTH;
            case WEST: return WEST;
            case NORTH: return NORTH;
            default: return null;
        }
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
    }
}
