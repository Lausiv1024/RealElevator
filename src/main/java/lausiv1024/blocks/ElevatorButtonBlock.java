package lausiv1024.blocks;

import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
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

public class ElevatorButtonBlock extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty IS_SINGLE = BooleanProperty.create("is_single");

    /*-------------------------------------------------NORTH---------------------------------------------------------------*/
    private static final VoxelShape NORTH_BASE = Block.box(5,2,0,11,14,0.2);
    private static final VoxelShape NORTH_BUTTON_UP = Block.box(7,9,0.2,9,11,1);
    private static final VoxelShape NORTH_BUTTON_DOWN = Block.box(7,5,0.2,9,7,1);
    private static final VoxelShape NORTH_ALL = VoxelShapes.or(NORTH_BASE, NORTH_BUTTON_UP, NORTH_BUTTON_DOWN);
    /*---------------------------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------SOUTH---------------------------------------------------------------*/
    private static final VoxelShape SOUTH_BASE  = Block.box(5,2,15.8,11, 14, 16);
    private static final VoxelShape SOUTH_BUTTON_UP = box(7, 9, 15, 9, 11, 15.8);
    private static final VoxelShape SOUTH_BUTTON_DOWN = box(7, 5, 15, 9, 7, 15.8);
    private static final VoxelShape SOUTH_ALL = VoxelShapes.or(SOUTH_BASE, SOUTH_BUTTON_UP, SOUTH_BUTTON_DOWN);
    /*---------------------------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------EAST----------------------------------------------------------------*/
    private static final VoxelShape EAST_BASE = box(15.8, 2, 5, 16, 14, 11);
    private static final VoxelShape EAST_BUTTON_UP = box(15, 9, 7, 15.8, 11, 9);
    private static final VoxelShape EAST_BUTTON_DOWN = box(15, 5, 7, 15.8, 7, 9);
    private static final VoxelShape EAST_ALL = VoxelShapes.or(EAST_BASE, EAST_BUTTON_UP, EAST_BUTTON_DOWN);
    /*---------------------------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------West----------------------------------------------------------------*/
    private static final VoxelShape WEST_BASE = box(0, 2, 5, 0.2, 14, 11);
    private static final VoxelShape WEST_BUTTON_UP = box(0.2, 9, 7, 1, 11, 9);
    private static final VoxelShape WEST_BUTTON_DOWN = box(0.2, 5, 7, 1, 7, 9);
    private static final VoxelShape WEST_ALL = VoxelShapes.or(WEST_BASE, WEST_BUTTON_DOWN, WEST_BUTTON_UP);
    /*---------------------------------------------------------------------------------------------------------------------*/

    public ElevatorButtonBlock(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
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

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        super.use(state, world, pos, playerEntity, hand, result);
        String aaa = result.getLocation().toString();
        double x = result.getLocation().x - pos.getX();
        double y = result.getLocation().y - pos.getY();
        double z = result.getLocation().z - pos.getZ();
        LandingButtonBlockTE landingButtonBlockTE = getTE(world, pos);
        if (landingButtonBlockTE == null) return ActionResultType.PASS;
        int a = 0;
        a = calculatePressedButton(state, result.getLocation(), pos);

        if (a == 0) return ActionResultType.PASS;
        playerEntity.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 1, 1f);
        if (a == 1){
            landingButtonBlockTE.up = true;
        }else{
            landingButtonBlockTE.down = true;
        }
        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.getClickedFace() == Direction.DOWN || context.getClickedFace() == Direction.UP){
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    private LandingButtonBlockTE getTE(World world, BlockPos pos){
        TileEntity entity = world.getBlockEntity(pos);
        if (entity instanceof LandingButtonBlockTE){
            return (LandingButtonBlockTE)entity;
        }
        return null;
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
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new LandingButtonBlockTE(false);
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
    }
    /*
    0 : no button pressed, 1 : up button, 2: down button
     */
    private int calculatePressedButton(BlockState state, Vector3d vector3d, BlockPos pos){
        double x = vector3d.x - pos.getX();
        double y = vector3d.y - pos.getY();
        double z = vector3d.z - pos.getZ();
        int a = 0;

        Direction facing = state.getValue(FACING);
        switch (facing){
            case WEST:
                if (x > 0.015){
                    if (y > 0.5){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case SOUTH:
                if (z < 0.985){
                    if (y > 0.5){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case NORTH:
                if (z > 0.015){
                    if (y > 0.5){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case EAST:
                if (x < 0.985){
                    if (y > 0.5){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
        }

        return a;
    }
}
