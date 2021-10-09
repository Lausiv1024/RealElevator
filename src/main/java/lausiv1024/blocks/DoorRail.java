package lausiv1024.blocks;

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

public class DoorRail extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape NORTH = Block.box(0, 13.8, 0, 16, 15.8, 2);
    private static final VoxelShape NORTH_MIZO0 = Block.box(0, 15.8, 0, 16, 16, 0.4);
    private static final VoxelShape NORTH_MIZO1 = Block.box(0, 15.8, 0.8, 16, 16, 1.2);
    private static final VoxelShape NORTH_MIZO2 = Block.box(0, 15.8, 1.6, 16, 16, 2.0);
    private static final VoxelShape EAST = box(14, 13.8, 0, 16, 14, 16);
    private static final VoxelShape EASt_MIZO0 = box(15.6, 15.8, 0, 16, 16, 16);
    private static final VoxelShape EAST_MIZO1 = box(14.8, 15.8, 0, 15.2, 16, 16);
    private static final VoxelShape EAST_MIZO2 = box(14, 15.8, 0, 14.4, 16, 16);
    private static final VoxelShape SOUTH = box(0, 13.8, 14, 16, 15.8, 16);
    private static final VoxelShape SOUTH_MIZO0 = box(0, 15.8, 15.6, 16, 16, 16);
    private static final VoxelShape SOUth_MIZO1 = box(0, 15.8, 14.8, 16, 16, 15.2);
    private static final VoxelShape SOUTH_MIZO2 = box(0, 15.8, 14, 16, 16, 14.4);
    private static final VoxelShape WEST = box(0, 13.8, 0, 2, 15.8, 16);
    private static final VoxelShape WEST_MIZO0 = box(0, 15.8, 0, 0.4, 16, 16);
    private static final VoxelShape WEST_MIZO1  = box(1.6, 15.8, 0, 2, 16, 16);
    private static final VoxelShape WEST_MIZO2 = box(0.8, 15.8, 0, 1.2, 16, 16);

    private static final VoxelShape NORTH_ALL = VoxelShapes.or(NORTH, NORTH_MIZO0, NORTH_MIZO1, NORTH_MIZO2);
    private static final VoxelShape SOUTH_ALL = VoxelShapes.or(SOUTH, SOUth_MIZO1, SOUTH_MIZO0, SOUTH_MIZO2);
    private static final VoxelShape EAST_ALL = VoxelShapes.or(EAST, EAST_MIZO1, EAST_MIZO2, EASt_MIZO0);
    private static final VoxelShape WEST_ALL = VoxelShapes.or(WEST, WEST_MIZO0, WEST_MIZO1, WEST_MIZO2);

    public DoorRail(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case NORTH: return NORTH_ALL;
            case SOUTH: return SOUTH_ALL;
            case EAST: return EAST_ALL;
            case WEST: return WEST_ALL;
        }
        return NORTH_ALL;
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
