package lausiv1024;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class REItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealElevatorCore.ID);

    public static final RegistryObject<Item> ITEM_ELEVATOR_MARKER = ITEMS.register("elevator_marker",() ->
            new BlockItem(REBlocks.ELEVATOR_MARKER.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> FLOOR_MARKER = ITEMS.register("floor_marker",() ->
            new Item(new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_ELEVATOR_BUTTON_SINGLE = ITEMS.register("elevator_button_single",() ->
            new BlockItem(REBlocks.ELEVATOR_BUTTON_SINGLE.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_ELEVATOR_BUTTON = ITEMS.register("elevator_button",() ->
            new BlockItem(REBlocks.ELEVATOR_BUTTON.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_DOOR_RAIL = ITEMS.register("door_rail",() ->
            new BlockItem(REBlocks.DOOR_RAIL.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_HOLE_LANTERN = ITEMS.register("hole_lantern",() ->
            new BlockItem(REBlocks.HOLE_LANTERN.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",() ->
            new Item(new Item.Properties().stacksTo(1).tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_GUIDE_RAIL = ITEMS.register("guide_rail",() ->
            new BlockItem(REBlocks.GUIDE_RAIL.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_RANGE_DISPLAY = ITEMS.register("range_display",() ->
            new BlockItem(REBlocks.RANGE_DISPLAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> ITEM_ELEVATOR_CONSTRUCTOR = ITEMS.register("elevator_constructor",() ->
            new BlockItem(REBlocks.ELEVATOR_CONSTRUCTOR.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_FLOOR_DISPLAY = ITEMS.register("floor_display",() ->
            new BlockItem(REBlocks.FLOOR_DISPLAY.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_DOOR_RAIL_DOUBLE = ITEMS.register("door_rail_double",() ->
            new BlockItem(REBlocks.DOOR_RAIL_DOUBLE.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_ELEVATOR_CONTROLLER = ITEMS.register("controller_module",() ->
            new BlockItem(REBlocks.ELEVATOR_CONTROLLER.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_GUIDE_RAILA = ITEMS.register("guide_rail1",() ->
            new BlockItem(REBlocks.GUIDE_RAILA.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ITEM_GUIDE_RAIL_NO_WEIGHT = ITEMS.register("guide_rail_no_weight",() ->
            new BlockItem(REBlocks.GUIDE_RAIL_NO_WEIGHT.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final RegistryObject<Item> ELEVATOR_BUTTON = ITEMS.register("elevator_button1",() ->
            new Item(new Item.Properties()));
    public static final RegistryObject<Item> ITEM_FLOOR_CONTROLLER = ITEMS.register("floor_controller", () ->
        new BlockItem(REBlocks.FLOOR_CONTROLLER.get(), new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
