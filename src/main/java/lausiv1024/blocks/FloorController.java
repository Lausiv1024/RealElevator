package lausiv1024.blocks;

import lausiv1024.RESoundEvents;
import lausiv1024.blocks.interfaces.IHasBounding;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
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

    public static final VoxelShape[] PLATE = {
        box(5, -4, 0, 11, 23, 0.2),
            box(15.8, -4, 5, 16, 23, 11),
            box(5, -4, 15.8, 11, 23, 16),
            box(0, -4, 5, 0.2, 23, 11)
    };
    public static final VoxelShape[] BUTTON_UP = {
        box(7, 4, 0.1, 9, 6, 0.9),
            box(15.1, 4, 7, 15.9, 6, 9),
            box(7, 4, 15.1, 9, 6, 15.9),
            box(0.1, 4, 7, 0.9, 6, 9)
    };
    public static final VoxelShape[] BUTTON_DOWN = {
        box(7, 0, 0.1, 9, 2, 0.9),
            box(15.1, 0, 7, 15.9, 2, 9),
            box(7, 0, 15.1, 9, 2, 15.9),
            box(0.1, 0, 7, 0.9, 2, 9)
    };

    public static final VoxelShape[] ALL = {
            VoxelShapes.or(PLATE[0], BUTTON_UP[0], BUTTON_DOWN[0]),
            VoxelShapes.or(PLATE[1], BUTTON_UP[1], BUTTON_DOWN[1]),
            VoxelShapes.or(PLATE[2], BUTTON_UP[2], BUTTON_DOWN[2]),
            VoxelShapes.or(PLATE[3], BUTTON_UP[3], BUTTON_DOWN[3])
    };

    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public FloorController(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        test(pos, result);
        int a = calculatePressedButton(state, result.getLocation(), pos);
        if (a > 0){
            if (a == 1) playerEntity.playSound(RESoundEvents.UPSOUND, 1, 1);
            if (a == 2) playerEntity.playSound(RESoundEvents.DOWNSOUND, 1, 1);
            return ActionResultType.SUCCESS;
        }
        return super.use(state, world, pos, playerEntity, hand, result);
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
        switch (facing){
            case WEST:
                if (x > 0.015){
                    if (y > yKijun){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case SOUTH:
                if (z < 0.985){
                    if (y > yKijun){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case NORTH:
                if (z > 0.015){
                    if (y > yKijun){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
            case EAST:
                if (x < 0.985){
                    if (y > yKijun){
                        a = 1;
                    }else{
                        a = 2;
                    }
                }
                break;
        }

        return a;
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

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        Direction direction = state.getValue(FACING);
        switch (direction){
            case NORTH:
                return ALL[0];
            case EAST:
                return ALL[1];
            case SOUTH:
                return ALL[2];
            case WEST:
                return ALL[3];
            default:
                throw new IllegalStateException("Invalid Facing Value - " + direction.toString());
        }
    }

    @Override
    public BlockPos[] getBoundingPosList() {
        return new BlockPos[]{new BlockPos(0, 1, 0),
        new BlockPos(0, -1, 0)};
    }
}
