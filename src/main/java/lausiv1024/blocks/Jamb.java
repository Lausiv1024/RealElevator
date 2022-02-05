package lausiv1024.blocks;

import lausiv1024.util.ModelRotationHelper;
import lausiv1024.util.RotatableBoxShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.state.BooleanProperty;
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
import sun.security.mscapi.CPublicKey;

import javax.annotation.Nullable;
import javax.swing.*;

public class Jamb extends ElevatorPartBlock{
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty HAS_ROOF = BooleanProperty.create("roof");
    public static final BooleanProperty IS_MIRROR = BooleanProperty.create("mirror");

    public static final RotatableBoxShape BASE1 = new RotatableBoxShape(0, 0, 0, 1.2, 16, 8);
    public static final RotatableBoxShape BASE2 = new RotatableBoxShape(14.8, 0, 0, 16, 16, 8);

    public static final RotatableBoxShape PART1 = new RotatableBoxShape(1, 0, 0, 3.9, 16, 1);
    public static final RotatableBoxShape PART2 = new RotatableBoxShape(12.1, 0, 0, 14, 16, 1);

    public static final RotatableBoxShape ROOF = new RotatableBoxShape(0, 14, 0, 16, 16, 7.9);

    public Jamb(){
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(HAS_ROOF, false).setValue(IS_MIRROR, false));
    }
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    public VoxelShape getShape(BlockState state, IBlockReader iBlockReader, BlockPos pos, ISelectionContext context) {
        VoxelShape a;
        Direction facing = state.getValue(FACING);
        if (state.getValue(IS_MIRROR)) a = VoxelShapes.or(PART2.rotateAndConvert(facing), BASE2.rotateAndConvert(facing));
        else a = VoxelShapes.or(PART1.rotateAndConvert(facing), BASE1.rotateAndConvert(facing));
        if (state.getValue(HAS_ROOF)) a = VoxelShapes.or(a, ROOF.rotateAndConvert(facing));
        return a;
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
        p_206840_1_.add(HAS_ROOF);
        p_206840_1_.add(IS_MIRROR);
    }

}
