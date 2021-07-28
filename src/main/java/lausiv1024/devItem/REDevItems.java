package lausiv1024.devItem;

import lausiv1024.RealElevatorCore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REDevItems{
    private static final Item.Properties DEFAULT = new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1);
    public static final List<Item> ITEM_LIST = new ArrayList<>();

    public static final Item DEBUG_BUTTON_SPAWN = register("button_spawn", new ButtonSpawn(DEFAULT));
    public static final Item DEBUG_BOUNDINGBOX_GETTER = register("boundingbox_getter",
            new BoundingBoxGetter(DEFAULT));
    public static final Item ENTITY_POS_GETTER = register("pos_getter",
            new EntityPosGetter(DEFAULT));

    private static Item register(String registryName, Item item){
        Item itemNamed = item.setRegistryName(RealElevatorCore.ID, registryName);
        ITEM_LIST.add(itemNamed);
        return itemNamed;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event){
        for (Item item : ITEM_LIST){
            event.getRegistry().register(item);
        }
    }
}
