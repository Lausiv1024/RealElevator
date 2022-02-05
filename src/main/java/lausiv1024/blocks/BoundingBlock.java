package lausiv1024.blocks;

import lausiv1024.RealElevator;
import lausiv1024.tileentity.BoundingBlockTE;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BoundingBlock extends Block {
    public BoundingBlock(AbstractBlock.Properties properties){
        super(properties);
    }

    public static BlockPos getMainBlockPos(IBlockReader reader, BlockPos pos){
        TileEntity te = reader.getBlockEntity(pos);
        if (te == null) return null;
        BoundingBlockTE boundingBlockTE = null;
        if (te instanceof BoundingBlockTE){
             boundingBlockTE = (BoundingBlockTE) te;
            if (!boundingBlockTE.blockRegistered || !pos.equals(boundingBlockTE.getMainPos())) return null;
        }
        return boundingBlockTE.getMainPos();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) return ActionResultType.FAIL;
        BlockState mainState = world.getBlockState(mainPos);
        return mainState.getBlock().use(mainState, world, mainPos, playerEntity, hand, result);
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())){
            BlockPos mainPos = getMainBlockPos(world, pos);
            if (mainPos != null){
                BlockState mainState = world.getBlockState(mainPos);
                if (!mainState.isAir(world, mainPos)){
                    world.removeBlock(mainPos, false);
                }
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) return ItemStack.EMPTY;
        BlockState mainState = world.getBlockState(mainPos);
        return mainState.getBlock().getPickBlock(mainState, target, world, mainPos, player);
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderShape(@Nonnull BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!world.isClientSide){
            TileEntity en = world.getBlockEntity(pos);
            BlockPos mainPos = getMainBlockPos(world, pos);
            if (mainPos != null){
                world.getBlockState(mainPos).neighborChanged(world, mainPos, neighborBlock, neighborPos, isMoving);
            }
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new BoundingBlockTE();
    }

    @Override
    public float getExplosionResistance(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return super.getExplosionResistance(state, world, pos, explosion);
        }
        return world.getBlockState(mainPos).getExplosionResistance(world, mainPos, explosion);
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader world, @Nonnull BlockPos pos, @Nonnull ISelectionContext context) {
        BlockPos mainPos = getMainBlockPos(world, pos);
        if (mainPos == null) {
            return VoxelShapes.empty();
        }
        BlockState mainState;
        try {
            mainState = world.getBlockState(mainPos);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (world instanceof ChunkRenderCache) {
                mainState = world.getBlockState(mainPos);
            } else {
                RealElevator.LOGGER.error("Error getting bounding block shape, for position {}, with main position {}. World of type {}", pos, mainPos, world.getClass().getName());
                return VoxelShapes.empty();
            }
        }
        VoxelShape shape = mainState.getShape(world, mainPos, context);
        BlockPos offset = pos.subtract(mainPos);
        return shape.move(-offset.getX(), -offset.getY(), -offset.getZ());
    }
}
