package lausiv1024.blocks;

import lausiv1024.REBlocks;
import lausiv1024.REItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class RangeDisplay extends Block {
    public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 4);
    public static final VoxelShape SHAPE = box(4, 0, 4, 12, 16, 12);

    public RangeDisplay(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(STATE, 0));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
        int curY = pos.getY();
        int currentState = getStateNumWithFacing(result.getDirection());
        if (playerEntity.getItemInHand(hand).getItem() == REItems.FLOOR_MARKER && world.getBlockState(pos).getValue(STATE) == 0){

            for (int i = curY - 4; i < curY + 5; i++) {
                if (world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).getBlock() != REBlocks.RANGE_DISPLAY) {
                    if (!world.isClientSide)
                        playerEntity.sendMessage(new TranslationTextComponent("chat.mineelevator.not_enough_space"), playerEntity.getUUID());
                    //System.out.println("not_enough_space");
                    return ActionResultType.SUCCESS;
                }
                System.out.println(world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).getValue(STATE));
                if (curY != i) {
                    if (!(world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).getValue(STATE) == 0 ||
                            world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).getValue(STATE) != currentState)) {

                        if (!world.isClientSide)
                            playerEntity.sendMessage(new TranslationTextComponent("chat.mineelevator.not_enough_floor_space"), playerEntity.getUUID());
                        return ActionResultType.PASS;
                    }
                }
            }

            switch (result.getDirection()) {
                case NORTH:
                    world.setBlock(pos, state.setValue(STATE, 1), 3);
                    break;
                case SOUTH:
                    world.setBlock(pos, state.setValue(STATE, 3), 3);
                    break;
                case EAST:
                    world.setBlock(pos, state.setValue(STATE, 2), 3);
                    break;
                case WEST:
                    world.setBlock(pos, state.setValue(STATE, 4), 3);
                    break;
                default:
                    return ActionResultType.PASS;
            }
        }else{
            world.setBlock(pos, defaultBlockState(), 3);
        }
        return ActionResultType.SUCCESS;
    }

    private boolean isOpposite(int target, int origin){
        if (origin == 1 && target == 3) return true;
        else if (origin == 2 && target == 4) return true;
        else if (origin == 3 && target == 1) return true;
        else if (origin == 4 && target == 2) return true;
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState state1, IWorld world, BlockPos pos, BlockPos pos2) {
        BlockPos up = pos.above();
        BlockPos down = pos.below();
        boolean updownCorrect = (world.getBlockState(up).getBlock() == REBlocks.ELEVATOR_MARKER || world.getBlockState(up).getBlock() == REBlocks.RANGE_DISPLAY) &&
                (world.getBlockState(down).getBlock() == REBlocks.ELEVATOR_MARKER || world.getBlockState(down).getBlock() == REBlocks.RANGE_DISPLAY);
        if (!updownCorrect && !world.isClientSide()){
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, state1, world, pos, pos2);
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    private int getStateNumWithFacing(Direction facing) {
        switch (facing) {
            case NORTH:
                return 1;
            case SOUTH:
                return 3;
            case EAST:
                return 2;
            case WEST:
                return 4;
            default:
                return 0;
        }
    }
}
