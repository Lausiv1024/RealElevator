package net.lausiv1024.realelevator.item;

import java.util.List;
import java.util.Locale;

import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.client.ProfileScreen;
import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.lausiv1024.realelevator.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 仕様書。エレベーターの仕様と意匠（{@link ElevatorProfile}）を書いておく。
 *
 * <ul>
 * <li>しゃがんで設置用ブロックか乗場の操作盤（三方枠）を右クリックすると、その設定を写し取る。</li>
 * <li>そのまま右クリックすると、書いてある内容を反映する（設置用ブロックにはすべて、かごには設置したあとも変えられる部分だけ）。
 * 乗場の操作盤は {@code ButtonInput}、設置用ブロックは {@code InstallerBlock} で受ける。</li>
 * <li>何もないところで右クリックすると、手元のファイル（プロファイル）とやりとりする画面を開く。</li>
 * </ul>
 */
public class SpecificationItem extends Item {
    public SpecificationItem(Properties properties) {
        super(properties);
    }

    /** 書いてあるプロファイル。白紙なら空。 */
    public static CompoundTag profile(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.PROFILE.get());
        return data != null ? data.copyTag() : new CompoundTag();
    }

    /** 設定 settings を写し取る（名前はなくす）。 */
    public static void copyFrom(ItemStack stack, CompoundTag settings, Player player) {
        write(stack, ElevatorProfile.extract(settings));
        player.displayClientMessage(Component.translatable("message.realelevator.profile.copied"), true);
    }

    /** プロファイル profile を書く。 */
    public static void write(ItemStack stack, CompoundTag profile) {
        stack.set(ModDataComponents.PROFILE.get(), CustomData.of(ElevatorProfile.sanitize(profile)));
    }

    /** 設置用ブロック installer に反映する。白紙なら何もしない。 */
    public static void applyTo(ItemStack stack, InstallerBlockEntity installer, Player player) {
        CompoundTag profile = profile(stack);
        if (!ElevatorProfile.isPresent(profile)) {
            player.displayClientMessage(Component.translatable("message.realelevator.profile.blank"), true);
            return;
        }
        player.displayClientMessage(installer.applyProfile(profile), true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // しゃがんでいるときは設置用ブロックの useItemOn が呼ばれないので、ここで写し取る
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()
                || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof InstallerBlockEntity installer)) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            copyFrom(context.getItemInHand(), installer.settingsTag(), player);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ProfileScreen.openForSpecification(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag profile = profile(stack);
        if (!ElevatorProfile.isPresent(profile)) {
            tooltip.add(Component.translatable("tooltip.realelevator.specification.blank").withStyle(ChatFormatting.GRAY));
        } else {
            String name = ElevatorProfile.name(profile);
            if (!name.isEmpty()) {
                tooltip.add(Component.literal(name).withStyle(ChatFormatting.YELLOW));
            }
            tooltip.add(summary(profile).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.realelevator.specification.copy"));
        tooltip.add(Component.translatable("tooltip.realelevator.specification.apply"));
        tooltip.add(Component.translatable("tooltip.realelevator.specification.files"));
    }

    /** 機種・扉・速度の要約（「P-11 / 2枚戸中央開き / 60m/min」）。 */
    public static MutableComponent summary(CompoundTag profile) {
        return Component.literal(profile.getString("Model").replace("P", "P-") + " / ")
                .append(Component.translatable("gui.realelevator.door." + profile.getString("Door").toLowerCase(Locale.ROOT)))
                .append(" / ")
                .append(Component.translatable("gui.realelevator.speed." + profile.getString("Speed").toLowerCase(Locale.ROOT)));
    }
}
