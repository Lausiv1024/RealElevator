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
import org.lwjgl.system.CallbackI;

import javax.annotation.Nullable;

public class GuideRailBlock extends ElevatorPart{
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final VoxelShape A_NORTH = box(5, 0, 0, 11, 16, 1.1);
    public static final VoxelShape B_NORTH = box(7, 0, 1, 9, 16, 3);
    public static final VoxelShape C_NORTH = box(6, 0, 3, 10, 16, 4);
    public static final VoxelShape A_EAST = box(14.9, 0, 5, 16, 16, 11);
    public static final VoxelShape B_EAST = box(13, 0, 7, 15, 16, 9);
    public static final VoxelShape C_EAST = box(12, 0, 6, 13, 16, 10);
    public static final VoxelShape A_SOUTH = box(5, 0, 14.9, 11, 16, 16);
    public static final VoxelShape B_SOUTH = box(7, 0, 13, 9, 16, 15);
    public static final VoxelShape C_SOUTH = box(6, 0, 12, 10, 16, 13);
    public static final VoxelShape A_WEST = box(0, 0, 5, 1.1, 16, 11);
    public static final VoxelShape B_WEST = box(1, 0, 7, 3, 16, 9);
    public static final VoxelShape C_WEST = box(3, 0, 6, 4, 16, 10);


    public static final VoxelShape ALL_NORTH = VoxelShapes.or(A_NORTH, B_NORTH, C_NORTH);
    public static final VoxelShape ALL_EAST = VoxelShapes.or(A_EAST ,B_EAST ,C_EAST);
    public static final VoxelShape ALL_SOUTH = VoxelShapes.or(A_SOUTH, B_SOUTH, C_SOUTH);
    public static final VoxelShape ALL_WEST = VoxelShapes.or(A_WEST, B_WEST, C_WEST);

    public GuideRailBlock(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case SOUTH: return ALL_SOUTH;
            case WEST: return ALL_WEST;
            case NORTH: return ALL_NORTH;
            case EAST: return ALL_EAST;
        }
        return ALL_NORTH;
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
        p_206840_1_.add(FACING);
    }
}