package net.lausiv1024.realelevator.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 保守用レンチ。乗場の操作盤のあたり（三方枠）を右クリックすると、リニューアルと解体のメニューを開く。
 *
 * <p>判定は {@code c:tools/wrench} タグで行うので、ほかのMODのレンチでも開ける（{@code ButtonInput}）。
 */
public class MaintenanceWrenchItem extends Item {
    public MaintenanceWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.realelevator.wrench"));
        tooltip.add(Component.translatable("tooltip.realelevator.wrench_power"));
    }
}
