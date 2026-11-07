package net.lausiv1024.realelevator.block;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.install.InstallMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 設置に使う材料（{@link InstallMaterials}）を、持ち物のアイテムとやりとりする。 */
public final class MaterialStacks {
    private MaterialStacks() {
    }

    /** 持ち物に、材料 materials がそろっているか。 */
    public static boolean has(Player player, InstallMaterials materials) {
        return count(player.getInventory(), Items.IRON_INGOT) >= materials.iron()
                && count(player.getInventory(), Items.REDSTONE) >= materials.redstone();
    }

    /** 持ち物から材料 materials を取る（そろっていることを {@link #has} で確かめてから呼ぶ）。 */
    public static void take(Player player, InstallMaterials materials) {
        take(player.getInventory(), Items.IRON_INGOT, materials.iron());
        take(player.getInventory(), Items.REDSTONE, materials.redstone());
        player.getInventory().setChanged();
    }

    /** 材料 materials を返すときのアイテム（1スタックずつ）。 */
    public static List<ItemStack> stacks(InstallMaterials materials) {
        List<ItemStack> stacks = new ArrayList<>();
        add(stacks, Items.IRON_INGOT, materials.iron());
        add(stacks, Items.REDSTONE, materials.redstone());
        return stacks;
    }

    /** 「鉄インゴット×N、レッドストーン×M」。 */
    public static Component describe(InstallMaterials materials) {
        return Component.translatable("message.realelevator.install.materials", materials.iron(), materials.redstone());
    }

    /** 持ち物（オフハンドを含む）にある item の数。 */
    public static int countPreferringOffhand(Player player, Item item) {
        return count(player.getInventory(), item);
    }

    /** 持ち物から item を amount 個取る。オフハンドから先に使う（足りることを確かめてから呼ぶ）。 */
    public static void takePreferringOffhand(Player player, Item item, int amount) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(item)) {
            int taken = Math.min(amount, offhand.getCount());
            offhand.shrink(taken);
            amount -= taken;
        }
        take(player.getInventory(), item, amount);
        player.getInventory().setChanged();
    }

    private static int count(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void take(Inventory inventory, Item item, int amount) {
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
    }

    private static void add(List<ItemStack> stacks, Item item, int amount) {
        int max = new ItemStack(item).getMaxStackSize();
        while (amount > 0) {
            int size = Math.min(amount, max);
            stacks.add(new ItemStack(item, size));
            amount -= size;
        }
    }
}
