package net.lausiv1024.realelevator.registry;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.item.FloorMarkerItem;
import net.lausiv1024.realelevator.item.MaintenanceWrenchItem;
import net.lausiv1024.realelevator.item.SpecificationItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealElevator.MODID);

    public static final DeferredItem<BlockItem> INSTALLER = ITEMS.registerSimpleBlockItem(ModBlocks.INSTALLER);

    public static final DeferredItem<BlockItem> POWER_RECEIVER = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_RECEIVER);

    // 内装の素材
    public static final DeferredItem<BlockItem> WOODGRAIN_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.WOODGRAIN_LIGHT);
    public static final DeferredItem<BlockItem> WOODGRAIN_MEDIUM = ITEMS.registerSimpleBlockItem(ModBlocks.WOODGRAIN_MEDIUM);
    public static final DeferredItem<BlockItem> WOODGRAIN_DARK = ITEMS.registerSimpleBlockItem(ModBlocks.WOODGRAIN_DARK);
    public static final DeferredItem<BlockItem> STAINLESS_HAIRLINE = ITEMS.registerSimpleBlockItem(ModBlocks.STAINLESS_HAIRLINE);
    public static final DeferredItem<BlockItem> STAINLESS_MIRROR = ITEMS.registerSimpleBlockItem(ModBlocks.STAINLESS_MIRROR);
    public static final DeferredItem<BlockItem> STAINLESS_ETCHED = ITEMS.registerSimpleBlockItem(ModBlocks.STAINLESS_ETCHED);
    public static final DeferredItem<BlockItem> BRONZE_HAIRLINE = ITEMS.registerSimpleBlockItem(ModBlocks.BRONZE_HAIRLINE);
    public static final DeferredItem<BlockItem> VINYL_GRAY = ITEMS.registerSimpleBlockItem(ModBlocks.VINYL_GRAY);
    public static final DeferredItem<BlockItem> VINYL_BEIGE = ITEMS.registerSimpleBlockItem(ModBlocks.VINYL_BEIGE);

    /** 内装の素材のアイテム（クリエイティブタブの並び）。 */
    public static final List<DeferredItem<BlockItem>> MATERIALS = List.of(
            WOODGRAIN_LIGHT, WOODGRAIN_MEDIUM, WOODGRAIN_DARK, STAINLESS_HAIRLINE, STAINLESS_MIRROR, STAINLESS_ETCHED, BRONZE_HAIRLINE, VINYL_GRAY, VINYL_BEIGE);

    /** 階床登録ツール。制御盤を右クリックしてつなぎ、各階に立って右クリックすると停止階を登録・解除する。 */
    public static final DeferredItem<FloorMarkerItem> FLOOR_MARKER = ITEMS.register("floor_marker",
            () -> new FloorMarkerItem(new Item.Properties().stacksTo(1)));

    /** 保守用レンチ。乗場の操作盤を右クリックすると、リニューアルと解体のメニューを開く（{@code c:tools/wrench} のレンチならどれでも開ける）。 */
    public static final DeferredItem<MaintenanceWrenchItem> MAINTENANCE_WRENCH = ITEMS.register("maintenance_wrench",
            () -> new MaintenanceWrenchItem(new Item.Properties().stacksTo(1)));

    /** 仕様書。設置用ブロックやかごの仕様と意匠（プロファイル）を写し取り、ほかの設置用ブロックやかごに反映する。 */
    public static final DeferredItem<SpecificationItem> SPECIFICATION = ITEMS.register("specification",
            () -> new SpecificationItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
