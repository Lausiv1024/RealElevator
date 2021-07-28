package lausiv1024.blocks;

import lausiv1024.util.RotatableBoxShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
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

public class GuideRailA extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 1 ,2);

    private static final RotatableBoxShape BASE1 = new RotatableBoxShape(0, 0, 5, 2.5, 16, 5.5);
    private static final RotatableBoxShape RAIL1 = new RotatableBoxShape(0, 0, 5.5, 0.5, 16, 8);
    private static final RotatableBoxShape WEIGHT_BASE1 = new RotatableBoxShape(7.5, 0, 1, 10.5, 16, 1.6);
    private static final RotatableBoxShape WEIGHT_RAIL1 = new RotatableBoxShape(9.9, 0, 1.6, 10.5, 16, 4.6);

    private static final RotatableBoxShape BASE2 = new RotatableBoxShape(13.5, 0, 5, 16, 16, 5.5);
    private static final RotatableBoxShape RAIL2 = new RotatableBoxShape(15.5, 0, 5.5, 16, 16, 8);
    private static final RotatableBoxShape WEIGHT_BASE2 = new RotatableBoxShape(6.5, 0, 1, 9.5, 16, 1.6);
    private static final RotatableBoxShape WEIGHT_RAIL2 = new RotatableBoxShape(6.5, 0, 1.6, 7.1, 16, 4.6);

    private static final VoxelShape ALL1_NORTH = VoxelShapes.or(BASE1.getNorth(), RAIL1.getNorth(),
            WEIGHT_BASE1.getNorth(), WEIGHT_RAIL1.getNorth());
    private static final VoxelShape ALL1_SOUTH = VoxelShapes.or(BASE1.getSouth(), RAIL1.getSouth(),
            WEIGHT_RAIL1.getSouth(), WEIGHT_BASE1.getSouth());
    private static final VoxelShape ALL1_EAST = VoxelShapes.or(BASE2.getEast(), RAIL2.getEast(),
            WEIGHT_RAIL2.getEast().move(0, 0, 0.06), WEIGHT_BASE2.getEast().move(0, 0, 0.06));
    private static final VoxelShape ALL1_WEST = VoxelShapes.or(BASE2.getWest(), RAIL2.getWest(),
            WEIGHT_RAIL2.getWest().move(0, 0, -0.06), WEIGHT_BASE2.getWest().move(0, 0, -0.06));

    private static final VoxelShape ALL2_NORTH = VoxelShapes.or(BASE2.getNorth(), RAIL2.getNorth(),
            WEIGHT_BASE2.getNorth(), WEIGHT_RAIL2.getNorth());
    private static final VoxelShape ALL2_SOUTH = VoxelShapes.or(BASE2.getSouth(), RAIL2.getSouth(),
            WEIGHT_RAIL2.getSouth(), WEIGHT_BASE2.getSouth());
    //ずれやがったので強制的に設定
    private static final VoxelShape ALL2_EAST = VoxelShapes.or(BASE1.getEast(), RAIL1.getEast(),
            WEIGHT_RAIL1.getEast().move(0,0,0.06), WEIGHT_BASE1.getEast().move(0,0,0.06));
    private static final VoxelShape ALL2_WEST = VoxelShapes.or(BASE1.getWest(), RAIL1.getWest(),
            WEIGHT_RAIL1.getWest().move(0, 0, -0.06), WEIGHT_BASE1.getWest().move(0, 0, -0.06));

    public GuideRailA() {
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 1));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
         return context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP ?
                this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()) :
                this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }@Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        if (state.getValue(PART) == 1){
            switch (state.getValue(FACING)){
                case NORTH: return ALL1_NORTH;
                case SOUTH: return ALL1_SOUTH;
                case WEST: return ALL1_WEST;
                case EAST: return ALL1_EAST;
                default: return VoxelShapes.block();
            }
        }else{
            switch (state.getValue(FACING)){
                case NORTH: return ALL2_NORTH;
                case SOUTH: return ALL2_SOUTH;
                case WEST: return ALL2_WEST;
                case EAST: return ALL2_EAST;
                default: return VoxelShapes.block();
            }
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
        p_206840_1_.add(PART);
    }
}
