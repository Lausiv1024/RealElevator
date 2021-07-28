package lausiv1024.blocks;

import lausiv1024.RESoundEvents;
import lausiv1024.RealElevatorCore;
import lausiv1024.tileentity.HoleLanternTile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public class HoleLantern extends ElevatorPartBlock {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape NORTH_BASE = box(6, 0, 0, 10, 16, 0.2);
    private static final VoxelShape NORTH_LIGHT = box(7, 1, 0.2, 9, 15, 1.2);
    private static final VoxelShape EAST_BASE = box(15.8, 0, 6, 16, 16, 10);
    private static final VoxelShape EAST_LIGHT = box(14.8, 1, 7, 15.8, 15, 9);
    private static final VoxelShape SOUTH_BASE = box(6, 0, 15.8, 10, 16, 16);
    private static final VoxelShape SOUTH_LIGHT = box(7, 1, 14.8, 9, 15, 15.8);
    private static final VoxelShape WEST_BASE = box(0, 0, 6, 0.2, 16, 10);
    private static final VoxelShape WEST_LIGHT = box(0.2, 1, 7, 1.2, 15, 9);

    private static final VoxelShape NORTH = VoxelShapes.or(NORTH_BASE, NORTH_LIGHT);
    private static final VoxelShape SOUTH = VoxelShapes.or(SOUTH_BASE, SOUTH_LIGHT);
    private static final VoxelShape EAST = VoxelShapes.or(EAST_BASE, EAST_LIGHT);
    private static final VoxelShape WEST = VoxelShapes.or(WEST_BASE, WEST_LIGHT);

    public HoleLantern(){
        super();
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new HoleLanternTile(UUID.randomUUID());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)){
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case EAST: return EAST;
            case WEST: return WEST;
        }
        return NORTH;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        super.use(state, world, pos, playerEntity, hand, result);
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof HoleLanternTile){
            HoleLanternTile holeLanternTile = (HoleLanternTile) tileEntity;
            RealElevatorCore.LOGGER.info(state.getValue(FACING).getStepZ());
            test(playerEntity.getItemInHand(hand), holeLanternTile, world, pos);
        }

        return ActionResultType.SUCCESS;
    }

    private void test(ItemStack stack, HoleLanternTile holeLanternTile, World world, BlockPos pos){
        if (stack.getItem() == Items.REDSTONE_TORCH){
            holeLanternTile.setLightMode(1);
            world.playSound((PlayerEntity) null, holeLanternTile.getBlockPos(),  RESoundEvents.UPSOUND, SoundCategory.BLOCKS, 0.5f, 1);
        }else if (stack.getItem() == Items.REDSTONE_BLOCK){
            holeLanternTile.setLightMode(2);
            world.playSound((PlayerEntity) null, holeLanternTile.getBlockPos(),  RESoundEvents.DOWNSOUND, SoundCategory.BLOCKS, 0.5f, 1);
        }else if (stack.getItem() == Items.STICK){
            holeLanternTile.setLightMode(3);
        }else if (stack.getItem() == Items.REDSTONE){
            holeLanternTile.setBlinking(!holeLanternTile.getBlinking());
        }else if (stack.getItem() == Items.BLACK_WOOL){
            holeLanternTile.setLightColor(holeLanternTile.getLightColor() == 1 ? 0 : 1);
        }else holeLanternTile.setLightMode(0);
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState state1, boolean p_220082_5_) {
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof HoleLanternTile){
            HoleLanternTile holeLanternTile = (HoleLanternTile) tileEntity;
            holeLanternTile.setElevatorID(UUID.randomUUID());
        }
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
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
        p_206840_1_.add(FACING);
    }
}
