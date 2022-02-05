package lausiv1024.blocks;

import lausiv1024.REBlocks;
import lausiv1024.REItems;
import lausiv1024.blocks.interfaces.IHasBounding;
import lausiv1024.tileentity.BoundingBlockTE;
import lausiv1024.util.ModelRotationHelper;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

public abstract class ElevatorPartBlock extends Block{
    public ElevatorPartBlock() {
        super(AbstractBlock.Properties.of(Material.METAL).sound(SoundType.METAL).harvestLevel(2)
                .isSuffocating((p_test_1_, p_test_2_, p_test_3_) -> false)
                .isViewBlocking((p_test_1_, p_test_2_, p_test_3_) -> false).noOcclusion().strength(-1.0f, 114514.0f).isValidSpawn(ElevatorPartBlock::never));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        if (playerEntity.getItemInHand(hand).getItem() == REItems.WRENCH.get()){
            world.destroyBlock(pos, true);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState p_149656_1_) {
        return PushReaction.BLOCK;
    }

    private static BlockPos convert(BlockPos pos, Direction direction){
        return pos.rotate(ModelRotationHelper.toRotation(direction));
    }

    @Override
    public boolean isPathfindable(BlockState p_196266_1_, IBlockReader p_196266_2_, BlockPos p_196266_3_, PathType p_196266_4_) {
        return false;
    }

    private static Boolean never(BlockState p_235427_0_, IBlockReader p_235427_1_, BlockPos p_235427_2_, EntityType<?> p_235427_3_) {
        return false;
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos thisPos, BlockState newState, boolean isMoving) {
        super.onPlace(state, world, thisPos, newState, isMoving);
        if (state.getBlock() instanceof IHasBounding){
            IHasBounding bounding = (IHasBounding) state.getBlock();
            for (BlockPos boundingPos : bounding.getBoundingPosList()){
                BlockPos rotated;
                if (state.hasProperty(HorizontalBlock.FACING)){
                    rotated = convert(boundingPos, state.getValue(HorizontalBlock.FACING));
                }else rotated = boundingPos;

                BlockPos willPlace = thisPos.offset(rotated);
                world.setBlock(willPlace, REBlocks.BOUNDING_BLOCK.get().defaultBlockState(), Constants.BlockFlags.DEFAULT);
                TileEntity te = world.getBlockEntity(willPlace);

                if (te instanceof BoundingBlockTE){
                    BoundingBlockTE boundingBlockTE = (BoundingBlockTE) te;
                    boundingBlockTE.setMainLocation(thisPos);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoved) {
        super.onRemove(state, world, pos, newState, isMoved);
        if (state.getBlock() instanceof IHasBounding){
            IHasBounding bounding = (IHasBounding) state.getBlock();
            for (BlockPos boundingPos : bounding.getBoundingPosList()){
                BlockPos rotated;
                if (state.hasProperty(HorizontalBlock.FACING)){
                    rotated = convert(boundingPos, state.getValue(HorizontalBlock.FACING));
                }else rotated = boundingPos;

                BlockPos willPlace = pos.offset(rotated);
                world.setBlock(willPlace, Blocks.AIR.defaultBlockState(), Constants.BlockFlags.DEFAULT);
            }
        }
    }
}
