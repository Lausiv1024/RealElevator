package lausiv1024.blocks;

import lausiv1024.REBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public class ElevatorMarker extends Block {
    public ElevatorMarker(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        boolean match = false;
        int count = 0;
        for (int i = pos.getY() + 5; i < 256; i++){
            BlockPos upper = new BlockPos(pos.getX(), i, pos.getZ());

            if (world.getBlockState(upper).getBlock() == REBlocks.ELEVATOR_MARKER.get()){
                match = true;
                count = i;
            }else if (world.getBlockState(upper).getBlock() != Blocks.AIR){
                break;
            }
        }

        if (match){
            playerEntity.playSound(SoundEvents.ANVIL_LAND, 1, 1);
            for (int i = pos.getY() + 1; i < count; i++){
                world.setBlock(new BlockPos(pos.getX(), i, pos.getZ()),
                        REBlocks.RANGE_DISPLAY.get().defaultBlockState(), 3);
            }
        }
        return ActionResultType.SUCCESS;
    }
}
