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

public class GuideRailNoWeight extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, 2);

    private static final RotatableBoxShape BASE1 = new RotatableBoxShape(0, 0, 2.5, 0.5, 16, 5);
    private static final RotatableBoxShape RAIL1 = new RotatableBoxShape(0, 0, 2, 2.5, 16, 2.5);
    private static final RotatableBoxShape BASE2 = new RotatableBoxShape(13.5, 0, 2, 16, 16, 2.5);
    private static final RotatableBoxShape RAIL2 = new RotatableBoxShape(15.5, 0, 2.5, 16, 16, 5);

    private static final VoxelShape BASE1_NORTH_ALL = VoxelShapes.or(BASE1.getNorth(), RAIL1.getNorth());
    private static final VoxelShape BASE1_SOUTH_ALL = VoxelShapes.or(BASE1.getSouth(), RAIL1.getSouth());
    private static final VoxelShape BASE1_EAST_ALL = VoxelShapes.or(BASE2.getEast(), RAIL2.getEast());
    private static final VoxelShape BASE1_WEST_ALL = VoxelShapes.or(BASE2.getWest(), RAIL2.getWest());

    private static final VoxelShape BASE2_NORTH_ALL = VoxelShapes.or(BASE2.getNorth(), RAIL2.getNorth());
    private static final VoxelShape BASE2_SOUTH_ALL = VoxelShapes.or(BASE2.getSouth(), RAIL2.getSouth());
    private static final VoxelShape BASE2_EAST_ALL = VoxelShapes.or(BASE1.getEast(), RAIL1.getEast());
    private static final VoxelShape BASE2_WEST_ALL = VoxelShapes.or(BASE1.getWest(), RAIL1.getWest());

    public GuideRailNoWeight(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART,1));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP ?
                this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()) :
                this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        if (state.getValue(PART) == 1){
            switch (state.getValue(FACING)){
                case NORTH: return BASE1_NORTH_ALL;
                case SOUTH: return BASE1_SOUTH_ALL;
                case EAST: return BASE1_EAST_ALL;
                case WEST: return BASE1_WEST_ALL;
                default: throw new IllegalStateException("Illegal Facing : UP or Down");
            }
        }else{
            switch (state.getValue(FACING)){
                case NORTH: return BASE2_NORTH_ALL;
                case SOUTH: return BASE2_SOUTH_ALL;
                case EAST: return BASE2_EAST_ALL;
                case WEST: return BASE2_WEST_ALL;
                default: throw new IllegalStateException("Illegal Facing : UP or Down");
            }
        }
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
        p_206840_1_.add(FACING ,PART);
    }
}
