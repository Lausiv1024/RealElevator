package lausiv1024;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REItems {
    public static final List<Item> ITEM_LIST = new ArrayList<>();
    private static Item register(String registryName, Item item){
        Item itemNamed = item.setRegistryName(RealElevatorCore.ID, registryName);
        ITEM_LIST.add(itemNamed);
        return itemNamed;
    }

    public static final Item ITEM_ELEVATOR_MARKER = register("elevator_marker",
            new BlockItem(REBlocks.ELEVATOR_MARKER, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item FLOOR_MARKER = register("floor_marker",
            new Item(new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_ELEVATOR_BUTTON_SINGLE = register("elevator_button_single",
            new BlockItem(REBlocks.ELEVATOR_BUTTON_SINGLE, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_ELEVATOR_BUTTON = register("elevator_button",
            new BlockItem(REBlocks.ELEVATOR_BUTTON, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_DOOR_RAIL = register("door_rail",
            new BlockItem(REBlocks.DOOR_RAIL, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_HOLE_LANTERN = register("hole_lantern",
            new BlockItem(REBlocks.HOLE_LANTERN, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item WRENCH = register("wrench",
            new Item(new Item.Properties().stacksTo(1).tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_GUIDE_RAIL = register("guide_rail",
            new BlockItem(REBlocks.GUIDE_RAIL, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_RANGE_DISPLAY = register("range_display",
            new BlockItem(REBlocks.RANGE_DISPLAY, new Item.Properties()));
    public static final Item ITEM_ELEVATOR_CONSTRUCTOR = register("elevator_constructor",
            new BlockItem(REBlocks.ELEVATOR_CONSTRUCTOR, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_FLOOR_DISPLAY = register("floor_display",
            new BlockItem(REBlocks.FLOOR_DISPLAY, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_DOOR_RAIL_DOUBLE = register("door_rail_double",
            new BlockItem(REBlocks.DOOR_RAIL_DOUBLE, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_ELEVATOR_CONTROLLER = register("controller_module",
            new BlockItem(REBlocks.ELEVATOR_CONTROLLER, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_GUIDE_RAILA = register("guide_rail1",
            new BlockItem(REBlocks.GUIDE_RAILA, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ITEM_GUIDE_RAIL_NO_WEIGHT = register("guide_rail_no_weight",
            new BlockItem(REBlocks.GUIDE_RAIL_NO_WEIGHT, new Item.Properties().tab(RealElevatorCore.REAL_ELEVATOR_GROUP)));
    public static final Item ELEVATOR_BUTTON = register("elevator_button1",
            new Item(new Item.Properties()));

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event){
        for (Item item : ITEM_LIST){
            event.getRegistry().register(item);
        }
    }
}
