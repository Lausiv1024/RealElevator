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
import java.util.function.ToIntFunction;

public class HoleLantern extends ElevatorPart{
    private static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape NORTH_BASE = box(6, 0, 0, 10, 16, 0.2);
    private static final VoxelShape NORTH_LIGHT = box(7, 1, 0.2, 9, 15, 1.2);
    private static final VoxelShape EAST_BASE = box(15.8, 0, 6, 16, 16, 10);
    private static final VoxelShape EAST_LIGHT = box(14.8, 1, 7, 15.8, 15, 9);
    private static final VoxelShape SOUTH_BASE = box(6, 0, 15.8, 10, 16, 16);
    private static final VoxelShape SOUTH_LIGHT = box(7, 1, 14.8, 9, 15, 15.8);
    private static final VoxelShape WEST_BASE = box(0, 0, 6, 0.2, 16, 10);
    private static final VoxelShape WEST_LIGHT = box(0.2, 1, 7, 1.2, 15, 9);

    private static final VoxelShape NORTH = VoxelShapes.or(NORTH_BASE, NORTH_LIGHT);
    private static final VoxelShape SOUTH = VoxelShapes.or(SOUTH_BASE, SOUTH_LIGHT);
    private static final VoxelShape EAST = VoxelShapes.or(EAST_BASE, EAST_LIGHT);
    private static final VoxelShape WEST = VoxelShapes.or(WEST_BASE, WEST_LIGHT);

    public HoleLantern(){
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
    }
}
