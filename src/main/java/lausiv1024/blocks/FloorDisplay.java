package lausiv1024.blocks;

import lausiv1024.RealElevatorCore;
import lausiv1024.tileentity.FloorDisplayTile;
import lausiv1024.util.ElevatorDirection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class FloorDisplay extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape NORTH = box(5, 3, 0, 11, 15, 0.3);
    private static final VoxelShape EAST = box(15.7, 3, 5, 16, 15, 11);
    private static final VoxelShape SOUTH = box(5, 3, 15.7, 11, 15, 16);
    private static final VoxelShape WEST = box(0,3,5,0.3,15,11);


    public FloorDisplay(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        super.use(state, world, pos, playerEntity, hand, result);
        TileEntity a = world.getBlockEntity(pos);
        if (a instanceof FloorDisplayTile){
            FloorDisplayTile floorDisplayTile = (FloorDisplayTile) a;
            floorDisplayTile.setCurFloorName("R");
            test(playerEntity, hand, floorDisplayTile);
        }
        return ActionResultType.SUCCESS;
    }

    private void test(PlayerEntity playerEntity, Hand hand, FloorDisplayTile tile){
        Item item = playerEntity.getItemInHand(hand).getItem();
        if (item == Items.REDSTONE){
            tile.curDirection = ElevatorDirection.UP;
            tile.setMoving(2);
            RealElevatorCore.LOGGER.info("asd");
        }else if (item == Items.BLACK_CONCRETE){
            tile.curDirection = ElevatorDirection.DOWN;
        }else if (item == Items.BLUE_CONCRETE){
            tile.clear();
        }else if (item == Items.CYAN_CONCRETE){
            tile.startBlinking();
        }
        else{
            tile.setMoving(0);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new FloorDisplayTile();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case NORTH: return NORTH;
            case EAST: return EAST;
            case WEST: return WEST;
            case SOUTH: return SOUTH;
        }
        return NORTH;
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
