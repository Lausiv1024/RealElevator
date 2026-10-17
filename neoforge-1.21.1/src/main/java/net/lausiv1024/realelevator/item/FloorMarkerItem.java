package net.lausiv1024.realelevator.item;

import java.util.List;

import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 階床登録ツール。制御盤を右クリックしてつなぎ、停止階にしたい階の床に立って右クリックすると、その階を登録する。
 * 登録済みの階でもう一度右クリックすると解除する。
 */
public class FloorMarkerItem extends Item {
    /** 制御盤から、水平方向にこの距離までの場所で登録できる。 */
    private static final double MAX_HORIZONTAL_DISTANCE = 16.0;

    public FloorMarkerItem(Properties properties) {
        super(properties);
    }

    /** 制御盤 pos につなぐ。 */
    public static void link(ItemStack stack, Level level, BlockPos pos, Player player) {
        stack.set(ModDataComponents.LINKED_PANEL.get(), GlobalPos.of(level.dimension(), pos));
        player.displayClientMessage(Component.translatable("message.realelevator.marker.linked", pos.getX(), pos.getY(), pos.getZ()), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        GlobalPos linked = stack.get(ModDataComponents.LINKED_PANEL.get());
        if (linked == null || !linked.dimension().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.realelevator.marker.not_linked"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos panelPos = linked.pos();
        double dx = player.getX() - (panelPos.getX() + 0.5);
        double dz = player.getZ() - (panelPos.getZ() + 0.5);
        if (dx * dx + dz * dz > MAX_HORIZONTAL_DISTANCE * MAX_HORIZONTAL_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.realelevator.marker.too_far"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isLoaded(panelPos) || !(level.getBlockEntity(panelPos) instanceof ControlPanelBlockEntity panel)) {
            player.displayClientMessage(Component.translatable("message.realelevator.marker.missing"), true);
            return InteractionResultHolder.fail(stack);
        }
        // 立っている床の高さ（足元のブロックの高さ）を停止階にする
        player.displayClientMessage(panel.toggleFloor(player.blockPosition().getY()), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos linked = stack.get(ModDataComponents.LINKED_PANEL.get());
        if (linked != null) {
            BlockPos pos = linked.pos();
            tooltip.add(Component.translatable("tooltip.realelevator.marker.linked", pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.realelevator.marker.not_linked"));
        }
    }
}
