package lausiv1024.blocks;

import lausiv1024.RESoundEvents;
import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ElevatorButtonSingleBlock extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    private static final VoxelShape NORTH_BASE = Block.box(5,2,0,11,14,0.2);
    private static final VoxelShape NORTH_BUTTON = Block.box(7, 7,0.2,9,9,1);
    private static final VoxelShape SOUTH_BASE = box(5,2,15.8,11,14,16);
    private static final VoxelShape SOUTH_BUTTON = box(7, 7, 15, 9, 9, 15.8);
    private static final VoxelShape WEST_BASE = box(0, 2, 5, 0.2, 14, 11);
    private static final VoxelShape WEST_BUTTON = box(0.2, 7, 7, 1, 9, 9);
    private static final VoxelShape EAST_BASE = box(15.8, 2, 5, 16, 14, 11);
    private static final VoxelShape EAST_BUTTON = box(15, 7, 7, 16, 9, 9);

    private static final VoxelShape NORTH_ALL = VoxelShapes.or(NORTH_BASE,NORTH_BUTTON);
    private static final VoxelShape SOUTH_ALL = VoxelShapes.or(SOUTH_BUTTON, SOUTH_BASE);
    private static final VoxelShape WEST_ALL = VoxelShapes.or(WEST_BASE, WEST_BUTTON);
    private static final VoxelShape EAST_ALL = VoxelShapes.or(EAST_BASE, EAST_BUTTON);

    public ElevatorButtonSingleBlock(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        super.use(state, world, pos, playerEntity, hand, result);
        TileEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof LandingButtonBlockTE)) return ActionResultType.PASS;
        LandingButtonBlockTE landingButtonBlockTE = (LandingButtonBlockTE) entity;
        boolean a = calculatePressedButton(state, result.getLocation(), pos);
        if (a){
            world.playSound(null, pos, RESoundEvents.CALL_SOUND,SoundCategory.BLOCKS, 0.5f, 1f);
            landingButtonBlockTE.down = true;
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    private boolean calculatePressedButton(BlockState state, Vector3d vector3d, BlockPos pos){
        double x = vector3d.x - pos.getX();
        double y = vector3d.y - pos.getY();
        double z = vector3d.z - pos.getZ();
        boolean a = false;

        Direction facing = state.getValue(FACING);
        switch (facing){
            case WEST:
                if (x > 0.015){
                    a = true;
                }
                break;
            case SOUTH:
                if (z < 0.985)a = true;
                break;
            case NORTH:
                if (z > 0.015)a = true;
                break;
            case EAST:
                if (x < 0.985)a = true;
                break;
        }

        return a;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case NORTH:return NORTH_ALL;
            case EAST:return EAST_ALL;
            case WEST:return WEST_ALL;
            case SOUTH:return SOUTH_ALL;
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
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new LandingButtonBlockTE(true);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
    }
}
