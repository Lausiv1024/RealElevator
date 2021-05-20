package lausiv1024.blocks;

import lausiv1024.REItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public abstract class ElevatorPart extends Block /*implements IWaterLoggable */{
    //public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public ElevatorPart() {
        super(AbstractBlock.Properties.of(Material.METAL).sound(SoundType.METAL).harvestLevel(2)
                .strength(200, 200).isSuffocating((p_test_1_, p_test_2_, p_test_3_) -> false)
                .isViewBlocking((p_test_1_, p_test_2_, p_test_3_) -> false).noOcclusion());
        //this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        if (playerEntity.getItemInHand(hand).getItem() == REItems.WRENCH){
            world.destroyBlock(pos, true);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public boolean isPathfindable(BlockState p_196266_1_, IBlockReader p_196266_2_, BlockPos p_196266_3_, PathType p_196266_4_) {
        return false;
    }
}
