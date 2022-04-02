package lausiv1024.blocks;

import lausiv1024.blocks.interfaces.IHasBounding;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.tileentity.FloorControllerTE;
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

public class FloorController extends ElevatorPartBlock implements IHasBounding {
    public static final RotatableBoxShape PLATE = new RotatableBoxShape(5, -4, 0, 11, 23, 0.2);
    public static final RotatableBoxShape BUT_UP = new RotatableBoxShape(7, 4, 0.1, 9, 6, 0.9);
    public static final RotatableBoxShape BUT_DOWN = new RotatableBoxShape(7, 0, 0.1, 9, 2, 0.9);
    public static final RotatableBoxShape BUT_SINGLE = new RotatableBoxShape(7, 2, 0.1, 9, 4, 0.9);

    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty IS_SINGLE = BooleanProperty.create("is_single");
    public FloorController(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(IS_SINGLE, false));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        test(pos, result);
        FloorControllerTE tile = getTe(world, pos);
        if (tile == null) return ActionResultType.PASS;
        int a = calculatePressedButton(state, result.getLocation(), pos);
        if (!world.isClientSide) {
            if (a > 0) {
                if (a == 1) {
                    //playerEntity.playSound(RESoundEvents.UP_SOUND, 1, 1);
                    tile.setDirection(ElevatorDirection.UP);
                    tile.upA();
                }
                if (a == 2) {
                    //playerEntity.playSound(RESoundEvents.DOWN_SOUND, 1, 1);
                    tile.setDirection(ElevatorDirection.DOWN);
                    tile.dwA();
                }
                if (a == 3) {
                    tile.setCalled();
                }
                return ActionResultType.CONSUME;
            }
        }
        return ActionResultType.CONSUME;
    }

    private void test(BlockPos pos, BlockRayTraceResult result){
    }

    /*
    0 : no button pressed 1 : up button 2: down button
     */
    private int calculatePressedButton(BlockState state, Vector3d vector3d, BlockPos pos){
        double x = vector3d.x - pos.getX();
        double y = vector3d.y - pos.getY();
        double z = vector3d.z - pos.getZ();
        int a = 0;
        double yKijun = 0.2;

        Direction facing = state.getValue(FACING);
        boolean boo = state.getValue(IS_SINGLE);

        switch (facing){
            case WEST:
                if (x > 0.015){
                    if (boo){
                        a = 3;
                    }
                    else if (y > yKijun){
                        a = 1;
                    }
                    else{
                        a = 2;
                    }
                }
                break;
            case SOUTH:
                if (z < 0.985){
                    if (boo){
                        a = 3;
                    }
                    else if (y > yKijun){
                        a = 1;
                    }
                    else{
                        a = 2;
                    }
                }
                break;
            case NORTH:
                if (z > 0.015){
                    if (boo){
                        a = 3;
                    }
                    else if (y > yKijun){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case EAST:
                if (x < 0.985){
                    if (boo){
                        a = 3;
                    }
                    else if (y > yKijun){
                        a = 1;
                    }
                    else{
                        a = 2;
                    }
                }
                break;
        }

        return a;
    }

    private FloorControllerTE getTe(World world, BlockPos pos){
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof FloorControllerTE)) return null;
        return (FloorControllerTE) tileEntity;
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> state) {
        state.add(FACING);
        state.add(IS_SINGLE);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new FloorControllerTE();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        Direction direction = state.getValue(FACING);
        boolean isSingle = state.getValue(IS_SINGLE);
        VoxelShape shape = PLATE.rotateAndConvert(direction);
        shape = isSingle ? VoxelShapes.or(shape, BUT_SINGLE.rotateAndConvert(direction))
                : VoxelShapes.or(shape, BUT_UP.rotateAndConvert(direction), BUT_DOWN.rotateAndConvert(direction));
        return shape;
    }

    @Override
    public BlockPos[] getBoundingPosList() {
        return new BlockPos[]{new BlockPos(0, 1, 0),
        new BlockPos(0, -1, 0)};
    }
}
