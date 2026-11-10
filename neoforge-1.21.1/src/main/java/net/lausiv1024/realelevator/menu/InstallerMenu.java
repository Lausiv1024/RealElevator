package net.lausiv1024.realelevator.menu;

import net.lausiv1024.realelevator.registry.ModItems;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 制御盤の設定画面のコンテナ。パワーモジュールのスロットと、プレイヤーの持ち物。
 *
 * <p>設定画面は「設定」と「動力」の表示を切り替え、スロットは「動力」の表示のときだけ出す（クライアントだけで切り替える）。
 */
public class InstallerMenu extends AbstractContainerMenu {
    /** パワーモジュールのスロットの位置（画面の枠の左上から）。 */
    public static final int STAR_X = 80;
    public static final int STAR_Y = 30;
    /** 持ち物の位置。 */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;

    private static final int STAR_SLOT = 0;
    private static final int INVENTORY_START = 1;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final BlockPos pos;
    private final ContainerLevelAccess access;
    /** スロットを出すか。クライアントの画面が切り替える。サーバーでは常に出している扱い。 */
    private boolean slotsVisible = true;

    /** サーバーで開くとき。 */
    public InstallerMenu(int containerId, Inventory inventory, InstallerBlockEntity panel) {
        this(containerId, inventory, panel.getBlockPos(), panel.starContainer());
    }

    /** クライアントで開くとき。スロットの中身はサーバーから同期される。 */
    public InstallerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), new SimpleContainer(1));
    }

    private InstallerMenu(int containerId, Inventory inventory, BlockPos pos, Container star) {
        super(ModMenus.INSTALLER.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        addSlot(new StarSlot(star, STAR_X, STAR_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new HideableSlot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new HideableSlot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    public BlockPos pos() {
        return pos;
    }

    public void setSlotsVisible(boolean visible) {
        this.slotsVisible = visible;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == STAR_SLOT) {
            if (!slot.mayPickup(player) || !moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.POWER_MODULE.get())) {
            if (!moveItemStackTo(stack, STAR_SLOT, STAR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INSTALLER.get());
    }

    /** 「動力」の表示のときだけ出すスロット。 */
    private class HideableSlot extends Slot {
        HideableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return slotsVisible;
        }
    }

    /** パワーモジュールを1個だけはめるスロット。 */
    private class StarSlot extends HideableSlot {
        StarSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.POWER_MODULE.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
