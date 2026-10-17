package net.lausiv1024.realelevator.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.lausiv1024.realelevator.client.ControlPanelScreen;
import net.lausiv1024.realelevator.item.FloorMarkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 制御盤。最下階の乗場の壁に置く。正面（FACING）は置いたプレイヤーの方、つまり乗場側を向く。
 *
 * <p>右クリックで設定画面を開く。階床登録ツールで右クリックすると、ツールをこの制御盤につなぐ。
 * 壊すと、設置したかご・乗場の扉・ガイドレールも撤去する。
 */
public class ControlPanelBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ControlPanelBlock> CODEC = simpleCodec(ControlPanelBlock::new);

    public ControlPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControlPanelBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof FloorMarkerItem) {
            if (!level.isClientSide) {
                FloorMarkerItem.link(stack, level, pos, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            // 画面のクラスはクライアントにしかないので、クライアントで実行されたときだけ参照する
            ControlPanelScreen.open(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ControlPanelBlockEntity panel) {
            panel.uninstall();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
