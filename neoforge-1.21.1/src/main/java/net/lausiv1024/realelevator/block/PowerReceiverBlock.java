package net.lausiv1024.realelevator.block;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;

/**
 * 運転切替盤。レッドストーン信号でエレベーターの通常運転と休止を切り替える。どこにでも置け、保守用レンチでエレベーターにつなぐ（普通仕様・高速仕様とも）。
 *
 * <p>つなぎ方：レンチを持ってしゃがみながら乗場の操作盤のあたりを右クリックしてエレベーターを選び、そのレンチで運転切替盤を右クリックする。
 * しゃがみながらレンチで運転切替盤を右クリックすると、つなぎを外す。レンチで右クリックすると、つないでいるかと通常運転か休止かを表示する。
 *
 * <p>まわりからレッドストーン信号を受けているあいだ（POWERED）つないだエレベーターは通常運転し、正面のランプが緑に点く。
 * 信号がないあいだは休止する。運転切替盤を壊すと、つなぎが外れて常に通常運転の扱いに戻る。
 */
public class PowerReceiverBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PowerReceiverBlock> CODEC = simpleCodec(PowerReceiverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PowerReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerReceiverBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Tags.Items.TOOLS_WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PowerReceiverBlockEntity receiver) {
            if (player.isShiftKeyDown()) {
                unlink(level, pos, receiver);
                player.displayClientMessage(Component.translatable("message.realelevator.power.unlinked"), true);
            } else if (stack.has(ModDataComponents.SELECTED_CAR.get())) {
                link(level, pos, receiver, stack.get(ModDataComponents.SELECTED_CAR.get()), player);
                stack.remove(ModDataComponents.SELECTED_CAR.get());
            } else {
                showStatus(level, pos, state, receiver, player);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void link(Level level, BlockPos pos, PowerReceiverBlockEntity receiver, UUID carId, Player player) {
        ElevatorCarEntity car = ElevatorTracker.findCar(level, carId);
        if (car == null) {
            player.displayClientMessage(Component.translatable("message.realelevator.power.no_car"), true);
            return;
        }
        unlink(level, pos, receiver);
        car.linkPowerReceiver(pos);
        receiver.setCar(carId);
        player.displayClientMessage(Component.translatable("message.realelevator.power.linked"), true);
    }

    private static void unlink(Level level, BlockPos pos, PowerReceiverBlockEntity receiver) {
        UUID carId = receiver.car();
        if (carId != null) {
            ElevatorCarEntity car = ElevatorTracker.findCar(level, carId);
            if (car != null && pos.equals(car.powerReceiverPos())) {
                car.unlinkPowerReceiver();
            }
            receiver.setCar(null);
        }
    }

    private static void showStatus(Level level, BlockPos pos, BlockState state, PowerReceiverBlockEntity receiver, Player player) {
        boolean linked = receiver.car() != null && ElevatorTracker.findCar(level, receiver.car()) != null;
        String key = !linked ? "message.realelevator.power.status_unlinked"
                : state.getValue(POWERED) ? "message.realelevator.power.status_on" : "message.realelevator.power.status_off";
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PowerReceiverBlockEntity receiver) {
            // 壊したら、つないでいたエレベーターは常に通常運転の扱いに戻る（読み込まれていなければ、かごの側で気づいて外す）
            unlink(level, pos, receiver);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
