package lausiv1024.blocks;

import lausiv1024.RealElevator;
import lausiv1024.tileentity.LandingButtonBlockTE;
import lausiv1024.util.RotatableBoxShape;
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

    private static final RotatableBoxShape BASE = new RotatableBoxShape(5, 0, 0, 11, 12, 0.2);
    private static final RotatableBoxShape UP_BUT = new RotatableBoxShape(7, 7, 0.2, 9, 9, 1);
    private static final RotatableBoxShape DW_BUT = new RotatableBoxShape(7, 3, 0.2, 9, 5, 1);
    private static final RotatableBoxShape SINGLE_BUT = new RotatableBoxShape(7, 5, 0.2, 9, 7, 1);

    public ElevatorButtonBlock(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(IS_SINGLE, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        Direction direction = state.getValue(FACING);
        VoxelShape shape = BASE.rotateAndConvert(direction);
        VoxelShape buttons = state.getValue(IS_SINGLE) ? SINGLE_BUT.rotateAndConvert(direction) :
                VoxelShapes.or(UP_BUT.rotateAndConvert(direction), DW_BUT.rotateAndConvert(direction));
        return VoxelShapes.or(shape, buttons);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        super.use(state, world, pos, playerEntity, hand, result);
        String aaa = result.getLocation().toString();
        RealElevator.LOGGER.info("Clicked Location >> {}", aaa);
        double x = result.getLocation().x - pos.getX();
        double y = result.getLocation().y - pos.getY();
        double z = result.getLocation().z - pos.getZ();
        LandingButtonBlockTE landingButtonBlockTE = getTE(world, pos);
        if (landingButtonBlockTE == null) return ActionResultType.PASS;
        if (world.isClientSide) return ActionResultType.CONSUME;

        int a = 0;
        a = calculatePressedButton(state, result.getLocation(), pos);

        if (a == 0) return ActionResultType.PASS;
        playerEntity.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 1, 1f);
        if (a == 1){
            landingButtonBlockTE.upA();
        }else if (a == 2){
            landingButtonBlockTE.dwA();
        }else if (a == 3){
            landingButtonBlockTE.setCalled();
        }
        return ActionResultType.CONSUME;
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
        return new LandingButtonBlockTE();
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING, IS_SINGLE);
    }
    /*
    0 : no button pressed, 1 : up button, 2: down button
     */
    private int calculatePressedButton(BlockState state, Vector3d vector3d, BlockPos pos){
        double x = vector3d.x - pos.getX();
        double y = vector3d.y - pos.getY();
        double z = vector3d.z - pos.getZ();
        int a = 0;
        boolean b = state.getValue(IS_SINGLE);

        Direction facing = state.getValue(FACING);
        switch (facing){
            case WEST:
                if (x > 0.015){
                    if (y > 0.3){
                        a = 1;
                    }else{
                        a = 2;
                    }

                    if (b) a = 3;
                }
                break;
            case SOUTH:
                if (z < 0.985){
                    if (y > 0.3){
                        a = 1;
                    }else{
                        a = 2;
                    }
                    if (b) a = 3;
                }
                break;
            case NORTH:
                if (z > 0.015){
                    if (y > 0.3){
                        a = 1;
                    }else{
                        a = 2;
                    }
                    if (b) a = 3;
                }
                break;
            case EAST:
                if (x < 0.985){
                    if (y > 0.3){
                        a = 1;
                    }else{
                        a = 2;
                    }
                    if (b) a = 3;
                }
                break;
        }

        return a;
    }
}
