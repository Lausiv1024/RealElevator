package lausiv1024;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
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

    public static final Item ITEM_ELEVATOR_BUTTON_SINGLE = register("elevator_button_single", new BlockItem(REBlocks.ELEVATOR_BUTTON_SINGLE, new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final Item ITEM_ELEVATOR_BUTTON = register("elevator_button", new BlockItem(REBlocks.ELEVATOR_BUTTON, new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final Item ITEM_DOOR_RAIL = register("door_rail", new BlockItem(REBlocks.DOOR_RAIL, new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final Item ITEM_HOLE_LANTERN = register("hole_lantern", new BlockItem(REBlocks.HOLE_LANTERN, new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final Item WRENCH = register("wrench", new Item(new Item.Properties().stacksTo(1).tab(ItemGroup.TAB_REDSTONE)));

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event){
        for (Item item : ITEM_LIST){
            event.getRegistry().register(item);
        }
    }
}
