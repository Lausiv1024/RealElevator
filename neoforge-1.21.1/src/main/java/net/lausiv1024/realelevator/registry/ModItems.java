package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.item.FloorMarkerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealElevator.MODID);

    public static final DeferredItem<BlockItem> CONTROL_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.CONTROL_PANEL);

    /** 階床登録ツール。制御盤を右クリックしてつなぎ、各階に立って右クリックすると停止階を登録・解除する。 */
    public static final DeferredItem<FloorMarkerItem> FLOOR_MARKER = ITEMS.register("floor_marker",
            () -> new FloorMarkerItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
