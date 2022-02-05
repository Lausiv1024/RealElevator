package lausiv1024.blocks;

import lausiv1024.blocks.interfaces.IHasBounding;
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

public class Motor extends ElevatorPartBlock implements IHasBounding {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final IntegerProperty GRADE = IntegerProperty.create("grade", 0, 2);

    private static final RotatableBoxShape BASE = new RotatableBoxShape(-2, 0, 7, 29, 3, 11);
    private static final RotatableBoxShape[] PARTS = {new RotatableBoxShape(25, 3, 2, 29, 16, 16),
    new RotatableBoxShape(12, 3, 3, 26, 15, 15),
    new RotatableBoxShape(5, 4, 3.5, 12, 14, 14.5)};

    public Motor(){
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(GRADE, 0));
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

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        Direction facing = state.getValue(FACING);


        return VoxelShapes.or(BASE.rotateAndConvert(facing),PARTS[0].rotateAndConvert(facing),
                PARTS[1].rotateAndConvert(facing), PARTS[2].rotateAndConvert(facing));
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING, GRADE);
    }

    @Override
    public BlockPos[] getBoundingPosList() {
        return new BlockPos[]{new BlockPos(1, 0, 0)};
    }
}
